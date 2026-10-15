package com.example.sermonindex.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.sermonindex.data.local.AppDatabase
import com.example.sermonindex.data.local.entities.FtsSermonResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CatalogSyncWorkerEntryPoint {
    fun getAppDatabase(): AppDatabase
}

class CatalogSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            CatalogSyncWorkerEntryPoint::class.java
        )
        val database = entryPoint.getAppDatabase()
        val catalogUrl = "https://api.sermonindex.net/downloads/sermons.json.gz"
        Log.i(TAG, "Starting full catalog sync from: $catalogUrl")

        var connection: HttpURLConnection? = null
        try {
            val url = URL(catalogUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            connection.setRequestProperty("User-Agent", "SermonIndexAndroid/1.0")

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Failed to download catalog. HTTP status: $responseCode")
                return@withContext Result.retry()
            }

            database.clearFts()

            val gzipStream = GZIPInputStream(connection.inputStream)
            val reader = BufferedReader(InputStreamReader(gzipStream, Charsets.UTF_8), 32 * 1024)

            val batch = ArrayList<FtsSermonResult>(1000)
            var count = 0
            val estimatedTotal = 70_000

            reader.useLines { lines ->
                for (line in lines) {
                    if (isStopped) {
                        Log.i(TAG, "Catalog sync worker was cancelled/stopped.")
                        return@useLines
                    }

                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || !trimmed.startsWith("{")) continue

                    try {
                        val json = JSONObject(trimmed)
                        val id = json.optString("id")
                        val title = json.optString("title")
                        val speaker = json.optString("speaker")
                        val summary = json.optString("summary")

                        if (id.isNotBlank()) {
                            batch.add(
                                FtsSermonResult(
                                    id = id,
                                    title = title,
                                    speaker = speaker,
                                    summary = summary
                                )
                            )
                        }

                        if (batch.size >= 1000) {
                            database.insertFtsBatch(batch)
                            count += batch.size
                            batch.clear()

                            val progress = ((count.toFloat() / estimatedTotal.toFloat()) * 100).toInt().coerceAtMost(99)
                            setProgress(workDataOf(KEY_PROGRESS to progress, KEY_COUNT to count))
                        }
                    } catch (e: Exception) {
                        // Skip malformed line and continue streaming
                    }
                }
            }

            if (batch.isNotEmpty()) {
                database.insertFtsBatch(batch)
                count += batch.size
                batch.clear()
            }

            Log.i(TAG, "Catalog sync complete. Total indexed: $count")
            setProgress(workDataOf(KEY_PROGRESS to 100, KEY_COUNT to count))
            Result.success(workDataOf(KEY_COUNT to count))
        } catch (e: Exception) {
            Log.e(TAG, "Catalog sync error: ${e.message}", e)
            Result.retry()
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val TAG = "CatalogSyncWorker"
        const val WORK_NAME = "catalog_sync_work"
        const val KEY_PROGRESS = "progress"
        const val KEY_COUNT = "count"
    }
}
