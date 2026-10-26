package com.example.sermonindex.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.sermonindex.R

/**
 * BroadcastReceiver triggered by the "Update Now" action button in the catalog update system notification.
 * Cancels the notification and enqueues CatalogSyncWorker immediately in the background.
 */
class CatalogUpdateBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_START_CATALOG_UPDATE) {
            // 1. Cancel the notification
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(CatalogUpdateCheckWorker.NOTIFICATION_ID)

            // 2. Trigger the catalog sync worker
            val workRequest = OneTimeWorkRequestBuilder<CatalogSyncWorker>()
                .addTag(CatalogSyncWorker.TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                CatalogSyncWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            // 3. User feedback
            Toast.makeText(
                context,
                context.getString(R.string.catalog_update_notification_started),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        const val ACTION_START_CATALOG_UPDATE = "com.example.sermonindex.action.START_CATALOG_UPDATE"
    }
}
