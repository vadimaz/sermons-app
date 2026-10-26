package com.example.sermonindex.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.sermonindex.data.local.dao.SearchDao
import com.example.sermonindex.data.local.entities.CatalogMetadataEntity
import com.example.sermonindex.data.local.entities.SermonFtsEntity
import com.example.sermonindex.data.network.SermonIndexApiService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CatalogSyncWorkerEntryPoint {
    fun getSearchDao(): SearchDao
    fun getApiService(): SermonIndexApiService
    fun getOkHttpClient(): OkHttpClient
}

@Serializable
private data class CatalogSermonJson(
    val id: String = "",
    val title: String = "",
    val speaker: String = "",
    val summary: String = "",
    val duration: String = "",
    val speakerImage: String = "",
    val hasAudio: Boolean = false,
    val mp3Url: String = "",
    val cdnMp3Url: String = ""
)

/**
 * Background WorkManager worker that downloads the compressed 70,000+ sermon catalog
 * (sermons.json.gz) and streams it directly into the Room SQLite FTS4 virtual table.
 */
class CatalogSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "catalog_sync_work"
        const val TAG = "CatalogSyncWorker"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        private const val CATALOG_URL = "https://api.sermonindex.net/downloads/sermons.json.gz"
        private const val BATCH_SIZE = 1000
        private const val ESTIMATED_TOTAL_SERMONS = 70_000
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            CatalogSyncWorkerEntryPoint::class.java
        )
        val searchDao = entryPoint.getSearchDao()
        val apiService = entryPoint.getApiService()
        val okHttpClient = entryPoint.getOkHttpClient()

        try {
            setProgress(workDataOf(KEY_PROGRESS to 0.01f))

            // 1. Fetch latest stats to get generated date and total count
            val stats = runCatching { apiService.getStats() }.getOrNull()
            val totalExpected = if (stats != null && stats.sermons > 0) stats.sermons else ESTIMATED_TOTAL_SERMONS
            val generatedAt = stats?.generated?.ifBlank { "unknown" } ?: "unknown"

            // 2. Download and stream sermons.json.gz
            val request = Request.Builder()
                .url(CATALOG_URL)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to download catalog: HTTP ${response.code}")
                return@withContext Result.failure(workDataOf(KEY_ERROR to "HTTP ${response.code}"))
            }

            val body = response.body ?: run {
                Log.e(TAG, "Empty response body from $CATALOG_URL")
                return@withContext Result.failure(workDataOf(KEY_ERROR to "Empty response body"))
            }

            // Clear previous index before populating fresh catalog
            searchDao.clearSermonsFts()

            val gzipStream = GZIPInputStream(body.byteStream())
            val reader = BufferedReader(InputStreamReader(gzipStream, Charsets.UTF_8), 32 * 1024)

            val batch = ArrayList<SermonFtsEntity>(BATCH_SIZE)
            var processedCount = 0

            reader.use { bufferedReader ->
                var line: String? = bufferedReader.readLine()
                while (line != null) {
                    if (isStopped) {
                        Log.w(TAG, "Catalog sync cancelled by WorkManager")
                        return@withContext Result.failure()
                    }

                    val cleanLine = line.trim()
                    if (cleanLine.isNotEmpty()) {
                        runCatching {
                            val sermon = json.decodeFromString<CatalogSermonJson>(cleanLine)
                            if (sermon.id.isNotBlank()) {
                                batch.add(
                                    SermonFtsEntity(
                                        sermonId = sermon.id,
                                        title = sermon.title,
                                        speaker = sermon.speaker,
                                        summary = sermon.summary,
                                        duration = sermon.duration,
                                        speakerImage = sermon.speakerImage,
                                        hasAudio = if (sermon.hasAudio) 1 else 0,
                                        mp3Url = sermon.mp3Url,
                                        cdnMp3Url = sermon.cdnMp3Url
                                    )
                                )
                                processedCount++
                            }
                        }

                        if (batch.size >= BATCH_SIZE) {
                            searchDao.insertSermonsFtsBatch(batch)
                            batch.clear()

                            val progress = (processedCount.toFloat() / totalExpected).coerceIn(0.02f, 0.98f)
                            setProgress(workDataOf(KEY_PROGRESS to progress))
                        }
                    }
                    line = bufferedReader.readLine()
                }

                // Insert remaining records
                if (batch.isNotEmpty()) {
                    searchDao.insertSermonsFtsBatch(batch)
                    batch.clear()
                }
            }

            // 3. Save catalog metadata in database
            searchDao.saveCatalogMetadata(
                CatalogMetadataEntity(
                    id = 1,
                    sermonCount = processedCount,
                    generatedAt = generatedAt,
                    downloadedAt = System.currentTimeMillis()
                )
            )

            // Ensure periodic update checks are active
            CatalogUpdateCheckWorker.enqueuePeriodicCheck(applicationContext)

            setProgress(workDataOf(KEY_PROGRESS to 1.0f))
            Log.i(TAG, "Catalog sync successfully completed: $processedCount sermons indexed (generatedAt=$generatedAt)")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Catalog sync error: ${e.message}", e)
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }
}
