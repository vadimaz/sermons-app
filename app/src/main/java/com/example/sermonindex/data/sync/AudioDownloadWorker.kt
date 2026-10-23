package com.example.sermonindex.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.sermonindex.R
import com.example.sermonindex.data.local.dao.DownloadsDao
import com.example.sermonindex.data.local.entities.DownloadedSermonEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AudioDownloadWorkerEntryPoint {
    fun getDownloadsDao(): DownloadsDao
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
        const val KEY_PROGRESS = "progress"

        private const val CHANNEL_ID = "sermon_downloads_channel"
        private const val NOTIFICATION_ID_BASE = 2000
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
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

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            AudioDownloadWorkerEntryPoint::class.java
        )
        val downloadsDao = entryPoint.getDownloadsDao()

        val downloadsDir = File(applicationContext.filesDir, "audio_downloads").apply { mkdirs() }
        val targetFile = File(downloadsDir, "${sermonId}.mp3")
        val tempFile = File(downloadsDir, "${sermonId}.mp3.tmp")

        createNotificationChannel()
        val notificationId = NOTIFICATION_ID_BASE + (sermonId.hashCode() and 0x7FFF)

        var connection: HttpURLConnection? = null
        try {
            var url = URL(downloadUrl)
            connection = openConnectionWithRedirects(url)

            var responseCode = connection.responseCode
            // Try CDN failover if primary failed with HTTP error
            if (responseCode !in 200..299 && cdnUrl.isNotBlank() && cdnUrl != downloadUrl) {
                Log.w(TAG, "Primary URL failed ($responseCode). Trying CDN URL: $cdnUrl")
                connection.disconnect()
                url = URL(cdnUrl)
                connection = openConnectionWithRedirects(url)
                responseCode = connection.responseCode
            }

            if (responseCode !in 200..299) {
                Log.e(TAG, "Failed to download audio. HTTP error code: $responseCode")
                tempFile.delete()
                return@withContext Result.retry()
            }

            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
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
        } finally {
            connection?.disconnect()
        }
    }

    private fun openConnectionWithRedirects(initialUrl: URL): HttpURLConnection {
        var currentUrl = initialUrl
        var redirects = 0
        while (redirects < 5) {
            val connection = currentUrl.openConnection() as HttpURLConnection
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "SermonIndexAndroid/1.1")

            val status = connection.responseCode
            if (status in 300..399) {
                val newUrlStr = connection.getHeaderField("Location")
                connection.disconnect()
                if (newUrlStr != null) {
                    currentUrl = URL(newUrlStr)
                    redirects++
                    continue
                }
            }
            return connection
        }
        return currentUrl.openConnection() as HttpURLConnection
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
}
