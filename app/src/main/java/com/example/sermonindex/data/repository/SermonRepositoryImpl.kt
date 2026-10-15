package com.example.sermonindex.data.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.sermonindex.data.local.AppDatabase
import com.example.sermonindex.data.local.entities.FavoriteSermonEntity
import com.example.sermonindex.data.local.entities.HistoryEntity
import com.example.sermonindex.data.mapper.toDomain
import com.example.sermonindex.data.mapper.toFavoriteEntity
import com.example.sermonindex.data.network.SermonIndexApiService
import com.example.sermonindex.data.sync.AudioDownloadWorker
import com.example.sermonindex.domain.model.BibleBook
import com.example.sermonindex.domain.model.ChapterDetails
import com.example.sermonindex.domain.model.DownloadedSermon
import com.example.sermonindex.domain.model.FavoriteSermon
import com.example.sermonindex.domain.model.ListeningHistory
import com.example.sermonindex.domain.model.OfflineSearchResult
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.model.SpeakerPagedSermons
import com.example.sermonindex.domain.model.Topic
import com.example.sermonindex.domain.model.TopicDetail
import com.example.sermonindex.domain.repository.SermonRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

import com.example.sermonindex.di.IoDispatcher
import javax.inject.Inject

/**
 * Concrete implementation of domain [SermonRepository].
 * Injects [CoroutineDispatcher] to avoid hardcoding Dispatchers.IO.
 */
class SermonRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: SermonIndexApiService,
    private val database: AppDatabase,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SermonRepository {

    override val favorites: Flow<List<FavoriteSermon>> =
        database.favoritesDao.getAllFavorites()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override val history: Flow<List<ListeningHistory>> =
        database.historyDao.getRecentHistory()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override val downloads: Flow<List<DownloadedSermon>> =
        database.downloadsDao.getAllDownloads()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun isFavorite(id: String): Flow<Boolean> =
        database.favoritesDao.isFavorite(id).flowOn(ioDispatcher)

    override fun isDownloaded(sermonId: String): Flow<Boolean> =
        database.downloadsDao.isDownloaded(sermonId).flowOn(ioDispatcher)

    override fun getDownloadProgress(sermonId: String): Flow<Int?> {
        val workManager = WorkManager.getInstance(context)
        return workManager.getWorkInfosForUniqueWorkFlow("download_$sermonId")
            .map { workInfos ->
                val workInfo = workInfos.firstOrNull() ?: return@map null
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> workInfo.progress.getInt(AudioDownloadWorker.KEY_PROGRESS, 0)
                    WorkInfo.State.ENQUEUED -> 0
                    else -> null
                }
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun downloadSermon(sermon: Sermon) = withContext(ioDispatcher) {
        val workManager = WorkManager.getInstance(context)
        val inputData = workDataOf(
            AudioDownloadWorker.KEY_SERMON_ID to sermon.id,
            AudioDownloadWorker.KEY_TITLE to sermon.title,
            AudioDownloadWorker.KEY_SPEAKER to sermon.speaker,
            AudioDownloadWorker.KEY_SPEAKER_IMAGE to sermon.speakerImage,
            AudioDownloadWorker.KEY_DURATION to sermon.duration,
            AudioDownloadWorker.KEY_PRIMARY_URL to sermon.mp3Url,
            AudioDownloadWorker.KEY_CDN_URL to sermon.cdnMp3Url
        )

        val request = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(
            "download_${sermon.id}",
            ExistingWorkPolicy.KEEP,
            request
        )
        Unit
    }

    override suspend fun cancelDownload(sermonId: String) = withContext(ioDispatcher) {
        WorkManager.getInstance(context).cancelUniqueWork("download_$sermonId")
        Unit
    }

    override suspend fun deleteDownload(sermonId: String) = withContext(ioDispatcher) {
        val entity = database.downloadsDao.getDownload(sermonId)
        if (entity != null) {
            val file = File(entity.localPath)
            if (file.exists()) {
                file.delete()
            }
            database.downloadsDao.deleteDownload(sermonId)
        }
    }

    override suspend fun getLocalAudioPath(sermonId: String): String? = withContext(ioDispatcher) {
        val entity = database.downloadsDao.getDownload(sermonId) ?: return@withContext null
        val file = File(entity.localPath)
        if (file.exists() && file.length() > 0) {
            entity.localPath
        } else {
            null
        }
    }

    override suspend fun toggleFavorite(sermon: Sermon) = withContext(ioDispatcher) {
        val currentlyFav = database.favoritesDao.isFavorite(sermon.id).first()
        if (currentlyFav) {
            database.favoritesDao.deleteFavorite(sermon.id)
        } else {
            database.favoritesDao.insertFavorite(sermon.toFavoriteEntity())
        }
    }

    override suspend fun updateHistory(
        sermonId: String,
        title: String,
        speaker: String,
        speakerImage: String,
        positionMs: Long,
        durationMs: Long
    ) = withContext(ioDispatcher) {
        val completed = durationMs > 0 && positionMs >= (durationMs - 5000L)
        database.historyDao.upsertHistory(
            HistoryEntity(
                sermonId = sermonId,
                title = title,
                speaker = speaker,
                speakerImage = speakerImage,
                lastPositionMs = positionMs,
                durationMs = durationMs,
                updatedAt = System.currentTimeMillis(),
                completed = completed
            )
        )
    }

    override suspend fun getPopularSermons(): Result<List<Sermon>> = withContext(ioDispatcher) {
        runCatching {
            apiService.getPopularSermons().sermons.map { it.toDomain() }
        }
    }

    override suspend fun getScriptureBooks(): Result<List<BibleBook>> = withContext(ioDispatcher) {
        runCatching {
            apiService.getScriptureBooks().books.map { it.toDomain() }
        }
    }

    override suspend fun getScriptureChapter(bookId: String, chapter: Int): Result<ChapterDetails> =
        withContext(ioDispatcher) {
            runCatching {
                apiService.getScriptureChapter(bookId, chapter).toDomain()
            }
        }

    override suspend fun getScriptureVerse(
        bookId: String,
        chapter: Int,
        verse: Int
    ): Result<List<Sermon>> = withContext(ioDispatcher) {
        runCatching {
            apiService.getScriptureVerse(bookId, chapter, verse).sermons.map { it.toDomain() }
        }
    }

    override suspend fun getSpeakers(): Result<List<Speaker>> = withContext(ioDispatcher) {
        runCatching {
            apiService.getSpeakers().speakers.map { it.toDomain() }
        }
    }

    override suspend fun getSpeakerDetails(slug: String): Result<Speaker> = withContext(ioDispatcher) {
        runCatching {
            apiService.getSpeakerDetails(slug).toDomain()
        }
    }

    override suspend fun getSpeakerSermonsPage(slug: String, page: Int): Result<SpeakerPagedSermons> =
        withContext(ioDispatcher) {
            runCatching {
                apiService.getSpeakerSermonsPage(slug, page).toDomain()
            }
        }

    override suspend fun getTopics(): Result<List<Topic>> = withContext(ioDispatcher) {
        runCatching {
            apiService.getTopics().topics.map { it.toDomain() }
        }
    }

    override suspend fun getTopicDetails(slug: String): Result<TopicDetail> = withContext(ioDispatcher) {
        runCatching {
            apiService.getTopicDetails(slug).toDomain()
        }
    }

    override suspend fun getSermonDetails(id: String): Result<SermonDetail> = withContext(ioDispatcher) {
        runCatching {
            apiService.getSermonDetails(id).toDomain()
        }
    }

    override val ftsIndexedCount: Flow<Int> = database.getFtsCountFlow().flowOn(ioDispatcher)

    override suspend fun searchOffline(query: String): List<OfflineSearchResult> =
        withContext(ioDispatcher) {
            database.searchFts(query).map { it.toDomain() }
        }

    override suspend fun clearOfflineIndex() = withContext(ioDispatcher) {
        database.clearFts()
    }

    override suspend fun clearHistory() = withContext(ioDispatcher) {
        database.historyDao.clearHistory()
    }
}
