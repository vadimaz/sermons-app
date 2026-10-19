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
import com.example.sermonindex.data.mapper.extractVerseMap
import com.example.sermonindex.data.mapper.toDomain
import com.example.sermonindex.data.mapper.toFavoriteEntity
import com.example.sermonindex.data.network.SermonIndexApiService
import com.example.sermonindex.data.sync.AudioDownloadWorker
import com.example.sermonindex.domain.model.BibleBook
import com.example.sermonindex.domain.model.BibleTranslation
import com.example.sermonindex.domain.model.ChapterDetails
import com.example.sermonindex.domain.model.DownloadedSermon
import com.example.sermonindex.domain.model.FavoriteSermon
import com.example.sermonindex.domain.model.ListeningHistory
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.model.SpeakerPagedSermons
import com.example.sermonindex.data.parser.VttParser
import com.example.sermonindex.domain.model.Topic
import com.example.sermonindex.domain.model.TopicDetail
import com.example.sermonindex.domain.model.TranscriptCue
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

    private var cachedTranslations: List<BibleTranslation>? = null
    private val chapterVerseCache = java.util.concurrent.ConcurrentHashMap<String, Map<Int, String>>()

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

    override suspend fun deleteAllDownloads() = withContext(ioDispatcher) {
        val allDownloads = database.downloadsDao.getAllDownloads().first()
        for (entity in allDownloads) {
            val file = File(entity.localPath)
            if (file.exists()) {
                file.delete()
            }
        }
        database.downloadsDao.deleteAllDownloads()
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

    override suspend fun getAvailableBibleTranslations(): Result<List<BibleTranslation>> =
        withContext(ioDispatcher) {
            runCatching {
                cachedTranslations?.let { return@runCatching it }
                val translations = apiService.getAvailableBibleTranslations().translations.map { it.toDomain() }
                cachedTranslations = translations
                translations
            }
        }

    override suspend fun getBibleVerseText(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val key = "$translationId:$bookId:$chapter"
            val cachedMap = chapterVerseCache[key]
            if (cachedMap != null) {
                return@runCatching cachedMap[verse]
                    ?: throw NoSuchElementException("Verse $verse not found in $key")
            }
            val response = apiService.getBibleChapter(translationId, bookId, chapter)
            val verseMap = response.extractVerseMap()
            chapterVerseCache[key] = verseMap
            verseMap[verse] ?: throw NoSuchElementException("Verse $verse not found in $key")
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

    private val vttCache = java.util.concurrent.ConcurrentHashMap<String, List<TranscriptCue>>()

    override suspend fun getTranscriptCues(vttUrl: String): Result<List<TranscriptCue>> = withContext(ioDispatcher) {
        if (vttUrl.isBlank()) return@withContext Result.success(emptyList())
        val cached = vttCache[vttUrl]
        if (cached != null) return@withContext Result.success(cached)

        runCatching {
            val response = apiService.getRawVtt(vttUrl)
            val vttText = response.string()
            val cues = VttParser.parse(vttText)
            vttCache[vttUrl] = cues
            cues
        }
    }

    private val translationBooksCache = java.util.concurrent.ConcurrentHashMap<String, List<BibleBook>>()

    override val bibleReadingPreferences: Flow<com.example.sermonindex.domain.model.BibleReadingPreferences> =
        database.bibleDao.getReadingState()
            .map { entity ->
                entity?.toDomain() ?: defaultBibleReadingPreferences()
            }
            .flowOn(ioDispatcher)

    private fun defaultBibleReadingPreferences(): com.example.sermonindex.domain.model.BibleReadingPreferences {
        val language = java.util.Locale.getDefault().language
        val defaultTranslation = when (language) {
            "ru" -> "rus_syn"
            "uk" -> "ukr_1996"
            "es" -> "spa_r09"
            else -> "BSB"
        }
        return com.example.sermonindex.domain.model.BibleReadingPreferences(
            translationId = defaultTranslation,
            bookId = "GEN",
            chapter = 1,
            verse = 1,
            fontSizeSp = 18f,
            isSerif = true,
            lineSpacingMultiplier = 1.5f,
            selectedNarrator = null
        )
    }

    override suspend fun getBibleBooksForTranslation(translationId: String): Result<List<BibleBook>> =
        withContext(ioDispatcher) {
            runCatching {
                translationBooksCache[translationId]?.let { return@runCatching it }
                val books = apiService.getBibleTranslationBooks(translationId).books.map { it.toDomain() }
                translationBooksCache[translationId] = books
                books
            }
        }

    override suspend fun getBibleChapter(
        translationId: String,
        bookId: String,
        chapter: Int
    ): Result<com.example.sermonindex.domain.model.BibleChapter> = withContext(ioDispatcher) {
        runCatching {
            val booksResult = getBibleBooksForTranslation(translationId).getOrNull()
            val bookName = booksResult?.firstOrNull { it.id.equals(bookId, ignoreCase = true) }?.name ?: bookId
            val response = apiService.getBibleChapter(translationId, bookId, chapter)

            val key = "$translationId:$bookId:$chapter"
            chapterVerseCache[key] = response.extractVerseMap()

            response.toDomain(
                translationId = translationId,
                bookId = bookId,
                bookName = bookName,
                chapterNumber = chapter
            )
        }
    }

    override suspend fun saveBibleReadingPosition(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int
    ) = withContext(ioDispatcher) {
        database.bibleDao.saveReadingPosition(translationId, bookId, chapter, verse)
    }

    override suspend fun saveBibleTypographyPreferences(
        fontSizeSp: Float,
        isSerif: Boolean,
        lineSpacingMultiplier: Float
    ) = withContext(ioDispatcher) {
        database.bibleDao.saveTypography(fontSizeSp, isSerif, lineSpacingMultiplier)
    }

    override suspend fun saveBibleNarrator(narrator: String?) = withContext(ioDispatcher) {
        database.bibleDao.saveNarrator(narrator)
    }

    override suspend fun clearHistory() = withContext(ioDispatcher) {
        database.historyDao.clearHistory()
    }
}
