package com.example.sermonindex.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.sermonindex.R
import com.example.sermonindex.data.local.dao.DownloadsDao
import com.example.sermonindex.data.local.entities.DownloadedSermonEntity
import com.example.sermonindex.di.IoDispatcher
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AudioDownloadWorkerEntryPoint {
    fun getDownloadsDao(): DownloadsDao
    fun getOkHttpClient(): OkHttpClient
    @IoDispatcher
    fun getIoDispatcher(): CoroutineDispatcher
}

class AudioDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "AudioDownloadWorker"
        const val KEY_SERMON_ID = "sermon_id"
        const val KEY_TITLE = "title"
        const val KEY_SPEAKER = "speaker"
        const val KEY_SPEAKER_IMAGE = "speaker_image"
        const val KEY_DURATION = "duration"
        const val KEY_PRIMARY_URL = "primary_url"
        const val KEY_CDN_URL = "cdn_url"
        const val KEY_IS_VIDEO = "is_video"
        const val KEY_PROGRESS = "progress"

        private const val CHANNEL_ID = "sermon_downloads_channel"
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            AudioDownloadWorkerEntryPoint::class.java
        )
        val downloadsDao = entryPoint.getDownloadsDao()
        val okHttpClient = entryPoint.getOkHttpClient()
        val ioDispatcher = entryPoint.getIoDispatcher()

        return withContext(ioDispatcher) {
            val sermonId = inputData.getString(KEY_SERMON_ID) ?: return@withContext Result.failure()
            val title = inputData.getString(KEY_TITLE) ?: "Sermon"
            val speaker = inputData.getString(KEY_SPEAKER) ?: ""
            val speakerImage = inputData.getString(KEY_SPEAKER_IMAGE) ?: ""
            val duration = inputData.getString(KEY_DURATION) ?: ""
            val primaryUrl = inputData.getString(KEY_PRIMARY_URL).orEmpty()
            val cdnUrl = inputData.getString(KEY_CDN_URL).orEmpty()

            val downloadUrl = primaryUrl.ifBlank { cdnUrl }
            if (downloadUrl.isBlank()) {
                Log.e(TAG, "No download URL available for sermon $sermonId")
                return@withContext Result.failure()
            }

            val isVideo = inputData.getBoolean(KEY_IS_VIDEO, false)
            val ext = if (isVideo || downloadUrl.contains(".mp4", ignoreCase = true)) "mp4" else "mp3"

            val downloadsDir = File(applicationContext.filesDir, "audio_downloads").apply { mkdirs() }
            val targetFile = File(downloadsDir, "${sermonId}.${ext}")
            val tempFile = File(downloadsDir, "${sermonId}.${ext}.tmp")

            createNotificationChannel()

            val downloadClient = okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            try {
                var response: Response = executeDownloadRequest(downloadClient, downloadUrl)

                // Try CDN failover if primary failed with HTTP error
                if (!response.isSuccessful && cdnUrl.isNotBlank() && cdnUrl != downloadUrl) {
                    Log.w(TAG, "Primary URL failed (${response.code}). Trying CDN URL: $cdnUrl")
                    response.close()
                    response = executeDownloadRequest(downloadClient, cdnUrl)
                }

                response.use { activeResponse ->
                    if (!activeResponse.isSuccessful) {
                        Log.e(TAG, "Failed to download audio. HTTP error code: ${activeResponse.code}")
                        tempFile.delete()
                        return@withContext Result.retry()
                    }

                    val body = activeResponse.body
                    val totalBytes = body.contentLength()
                    var downloadedBytes = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var bytesRead: Int
                            var lastReportedProgress = -1

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                if (isStopped) {
                                    Log.i(TAG, "Download worker was stopped for sermon $sermonId")
                                    tempFile.delete()
                                    return@withContext Result.failure()
                                }

                                output.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead

                                val progress = if (totalBytes > 0) {
                                    ((downloadedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt().coerceIn(0, 99)
                                } else {
                                    0
                                }

                                if (progress != lastReportedProgress && (progress % 5 == 0 || progress == 99)) {
                                    lastReportedProgress = progress
                                    setProgress(workDataOf(KEY_PROGRESS to progress))
                                }
                            }
                        }
                    }
                }

                if (tempFile.exists() && tempFile.length() > 0) {
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    val renamed = tempFile.renameTo(targetFile)
                    if (!renamed) {
                        tempFile.copyTo(targetFile, overwrite = true)
                        tempFile.delete()
                    }

                    // Insert into downloads table
                    downloadsDao.insertDownload(
                        DownloadedSermonEntity(
                            id = sermonId,
                            title = title,
                            speaker = speaker,
                            speakerImage = speakerImage,
                            duration = duration,
                            localPath = targetFile.absolutePath,
                            fileSizeBytes = targetFile.length(),
                            downloadedAt = System.currentTimeMillis()
                        )
                    )

                    setProgress(workDataOf(KEY_PROGRESS to 100))
                    Log.i(TAG, "Successfully downloaded sermon $sermonId (${targetFile.length()} bytes)")
                    Result.success()
                } else {
                    tempFile.delete()
                    Result.failure()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download error for sermon $sermonId", e)
                tempFile.delete()
                Result.retry()
            }
        }
    }

    private fun executeDownloadRequest(client: OkHttpClient, url: String): Response {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SermonIndexAndroid/1.1")
            .build()
        return client.newCall(request).execute()
    }

    private fun createNotificationChannel() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.download_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = applicationContext.getString(R.string.download_channel_description)
        }
        manager.createNotificationChannel(channel)
    }
}
