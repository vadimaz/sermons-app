package com.example.sermonindex.data.sync

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.sermonindex.MainActivity
import com.example.sermonindex.R
import com.example.sermonindex.data.local.dao.SearchDao
import com.example.sermonindex.data.network.SermonIndexApiService
import com.example.sermonindex.di.IoDispatcher
import com.example.sermonindex.domain.repository.UserPreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CatalogUpdateWorkerEntryPoint {
    fun getSearchDao(): SearchDao
    fun getApiService(): SermonIndexApiService
    fun getUserPreferencesRepository(): UserPreferencesRepository
    @IoDispatcher
    fun getIoDispatcher(): CoroutineDispatcher
}

/**
 * Background WorkManager CoroutineWorker that periodically checks whether
 * an updated sermon catalog is available on SermonIndex servers.
 *
 * Runs with NetworkType.CONNECTED and batteryNotLow constraints. If a newer
 * dataset is detected, dispatches a system notification with 1-tap update action.
 */
class CatalogUpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            CatalogUpdateWorkerEntryPoint::class.java
        )
        val searchDao = entryPoint.getSearchDao()
        val apiService = entryPoint.getApiService()
        val userPreferencesRepository = entryPoint.getUserPreferencesRepository()
        val ioDispatcher = entryPoint.getIoDispatcher()

        return withContext(ioDispatcher) {
            try {
                // 1. Check if local catalog exists; skip if user hasn't downloaded it yet
                val localMeta = searchDao.getCatalogMetadata()
                val ftsCount = searchDao.getFtsSermonCount()
                if (localMeta == null || localMeta.sermonCount == 0 || ftsCount == 0) {
                    Log.d(TAG, "No offline catalog found locally. Skipping background update check.")
                    return@withContext Result.success()
                }

                // 2. Fetch latest stats from SermonIndex API
                val stats = runCatching { apiService.getStats() }.getOrNull()
                if (stats == null) {
                    Log.w(TAG, "Failed to connect to /v2/stats API. Retrying later.")
                    return@withContext Result.retry()
                }

                // Record check timestamp
                userPreferencesRepository.setLastCatalogUpdateCheckTimestamp(System.currentTimeMillis())

                // 3. Compare timestamps and auto-trigger sync if newer dataset exists
                val hasUpdate = stats.generated.isNotBlank() && stats.generated != localMeta.generatedAt
                if (hasUpdate) {
                    Log.i(TAG, "New catalog detected! Server generated: ${stats.generated}, local: ${localMeta.generatedAt}. Automatically kicking off background sync.")
                    val syncWorkRequest = OneTimeWorkRequestBuilder<CatalogSyncWorker>()
                        .addTag(CatalogSyncWorker.TAG)
                        .build()
                    WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                        CatalogSyncWorker.WORK_NAME,
                        ExistingWorkPolicy.KEEP,
                        syncWorkRequest
                    )
                    postUpdateNotification(applicationContext, stats.sermons)
                } else {
                    Log.d(TAG, "Catalog is up-to-date (generatedAt: ${localMeta.generatedAt}).")
                }

                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for catalog update: ${e.message}", e)
                Result.retry()
            }
        }
    }

    companion object {
        const val WORK_NAME = "catalog_update_check_periodic"
        const val TAG = "CatalogUpdateCheckWorker"
        const val CHANNEL_ID = "catalog_updates_channel"
        const val NOTIFICATION_ID = 3001
        const val EXTRA_NAVIGATE_TO_SEARCH = "extra_navigate_to_search"
        const val EXTRA_NAVIGATE_TO_SETTINGS = "extra_navigate_to_settings"

        fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.catalog_updates_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.catalog_updates_channel_desc)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        @SuppressLint("MissingPermission")
        fun postUpdateNotification(context: Context, totalSermons: Int) {
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) {
                Log.w(TAG, "Notifications are disabled by user. Skipping notification.")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Skipping notification.")
                return
            }

            createNotificationChannel(context)

            val openSettingsIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_NAVIGATE_TO_SETTINGS, true)
            }
            val pendingOpenSettings = PendingIntent.getActivity(
                context,
                0,
                openSettingsIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val updateActionIntent = Intent(context, CatalogUpdateBroadcastReceiver::class.java).apply {
                action = CatalogUpdateBroadcastReceiver.ACTION_START_CATALOG_UPDATE
            }
            val pendingUpdateAction = PendingIntent.getBroadcast(
                context,
                1,
                updateActionIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val contentText = context.getString(R.string.catalog_update_notification_desc, totalSermons)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.catalog_update_notification_title))
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingOpenSettings)
                .addAction(
                    android.R.drawable.ic_popup_sync,
                    context.getString(R.string.catalog_update_notification_action_update),
                    pendingUpdateAction
                )
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        fun enqueuePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<CatalogUpdateCheckWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
                flexTimeInterval = 6,
                flexTimeIntervalUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancelPeriodicCheck(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
