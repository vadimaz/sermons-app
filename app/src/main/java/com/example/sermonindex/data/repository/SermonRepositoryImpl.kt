package com.example.sermonindex.data.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.sermonindex.data.local.dao.BibleBookmarksDao
import com.example.sermonindex.data.local.dao.DownloadsDao
import com.example.sermonindex.data.local.dao.FavoritesDao
import com.example.sermonindex.data.local.dao.HistoryDao
import com.example.sermonindex.data.local.dao.SearchDao
import com.example.sermonindex.data.local.entities.FavoriteSermonEntity
import com.example.sermonindex.data.local.entities.HistoryEntity
import com.example.sermonindex.data.local.entities.SearchHistoryEntity
import com.example.sermonindex.data.mapper.extractVerseMap
import com.example.sermonindex.data.mapper.toDomain
import com.example.sermonindex.data.mapper.toEntity
import com.example.sermonindex.data.mapper.toFavoriteEntity
import com.example.sermonindex.data.network.SermonIndexApiService
import com.example.sermonindex.data.sync.AudioDownloadWorker
import com.example.sermonindex.data.sync.CatalogSyncWorker
import com.example.sermonindex.domain.model.BibleBookmark
import com.example.sermonindex.domain.model.BibleBook
import com.example.sermonindex.domain.model.BibleTranslation
import com.example.sermonindex.domain.model.CatalogSyncState
import com.example.sermonindex.domain.model.ChapterDetails
import com.example.sermonindex.domain.model.DownloadedSermon
import com.example.sermonindex.domain.model.FavoriteSermon
import com.example.sermonindex.domain.model.ListeningHistory
import com.example.sermonindex.domain.model.SearchAggregateResult
import com.example.sermonindex.domain.model.SearchCategory
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.model.SpeakerPagedSermons
import com.example.sermonindex.data.parser.VttParser
import com.example.sermonindex.domain.model.Topic
import com.example.sermonindex.domain.model.TopicDetail
import com.example.sermonindex.domain.model.TranscriptCue
import com.example.sermonindex.domain.repository.SermonRepository
import com.example.sermonindex.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
    private val favoritesDao: FavoritesDao,
    private val historyDao: HistoryDao,
    private val downloadsDao: DownloadsDao,
    private val searchDao: SearchDao,
    private val bibleBookmarksDao: BibleBookmarksDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SermonRepository {

    private var cachedTranslations: List<BibleTranslation>? = null
    private var cachedSpeakers: List<Speaker>? = null
    private var cachedTopics: List<Topic>? = null
    private var cachedPopularSermons: List<Sermon>? = null
    private val hasCatalogUpdateState = MutableStateFlow(false)
    private val chapterVerseCache = java.util.concurrent.ConcurrentHashMap<String, Map<Int, String>>()

    override val favorites: Flow<List<FavoriteSermon>> =
        favoritesDao.getAllFavorites()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override val history: Flow<List<ListeningHistory>> =
        historyDao.getRecentHistory()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override val downloads: Flow<List<DownloadedSermon>> =
        downloadsDao.getAllDownloads()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun isFavorite(id: String): Flow<Boolean> =
        favoritesDao.isFavorite(id).flowOn(ioDispatcher)

    override fun isDownloaded(sermonId: String): Flow<Boolean> =
        downloadsDao.isDownloaded(sermonId).flowOn(ioDispatcher)

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
        val isVideo = sermon.hasVideo
        val primaryUrl = if (isVideo) {
            sermon.mp4Url.ifBlank { sermon.cdnMp4Url.ifBlank { sermon.mp3Url } }
        } else {
            sermon.mp3Url.ifBlank { sermon.cdnMp3Url }
        }
        val cdnUrl = if (isVideo) {
            sermon.cdnMp4Url.ifBlank { sermon.cdnMp3Url }
        } else {
            sermon.cdnMp3Url
        }
        val inputData = workDataOf(
            AudioDownloadWorker.KEY_SERMON_ID to sermon.id,
            AudioDownloadWorker.KEY_TITLE to sermon.title,
            AudioDownloadWorker.KEY_SPEAKER to sermon.speaker,
            AudioDownloadWorker.KEY_SPEAKER_IMAGE to sermon.speakerImage,
            AudioDownloadWorker.KEY_DURATION to sermon.duration,
            AudioDownloadWorker.KEY_PRIMARY_URL to primaryUrl,
            AudioDownloadWorker.KEY_CDN_URL to cdnUrl,
            AudioDownloadWorker.KEY_IS_VIDEO to isVideo
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

    override suspend fun deleteDownload(sermonId: String): Unit = withContext(ioDispatcher) {
        val entity = downloadsDao.getDownload(sermonId)
        if (entity != null) {
            val file = File(entity.localPath)
            if (file.exists()) {
                file.delete()
            }
            downloadsDao.deleteDownload(sermonId)
        }
    }

    override suspend fun deleteAllDownloads(): Unit = withContext(ioDispatcher) {
        val allDownloads = downloadsDao.getAllDownloads().first()
        for (entity in allDownloads) {
            val file = File(entity.localPath)
            if (file.exists()) {
                file.delete()
            }
        }
        downloadsDao.deleteAllDownloads()
    }

    override suspend fun getLocalAudioPath(sermonId: String): String? = withContext(ioDispatcher) {
        val entity = downloadsDao.getDownload(sermonId) ?: return@withContext null
        val file = File(entity.localPath)
        if (file.exists() && file.length() > 0) {
            entity.localPath
        } else {
            null
        }
    }

    override suspend fun toggleFavorite(sermon: Sermon): Unit = withContext(ioDispatcher) {
        val currentlyFav = favoritesDao.isFavorite(sermon.id).first()
        if (currentlyFav) {
            favoritesDao.deleteFavorite(sermon.id)
        } else {
            favoritesDao.insertFavorite(sermon.toFavoriteEntity())
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
        if (sermonId.startsWith("bible_")) return@withContext
        val completed = durationMs > 0 && positionMs >= (durationMs - 5000L)
        historyDao.upsertHistory(
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

    override suspend fun recordSermonView(
        sermonId: String,
        title: String,
        speaker: String,
        speakerImage: String
    ): Unit = withContext(ioDispatcher) {
        if (sermonId.startsWith("bible_")) return@withContext
        val existing = historyDao.getHistory(sermonId)
        historyDao.upsertHistory(
            HistoryEntity(
                sermonId = sermonId,
                title = title,
                speaker = speaker,
                speakerImage = speakerImage,
                lastPositionMs = existing?.lastPositionMs ?: 0L,
                durationMs = existing?.durationMs ?: 0L,
                updatedAt = System.currentTimeMillis(),
                completed = existing?.completed ?: false
            )
        )
    }

    override suspend fun getPopularSermons(): Result<List<Sermon>> = withContext(ioDispatcher) {
        cachedPopularSermons?.let { return@withContext Result.success(it) }
        runCatching {
            val sermons = apiService.getPopularSermons().sermons.map { it.toDomain() }
            cachedPopularSermons = sermons
            sermons
        }
    }

    override suspend fun getRandomSermons(shard: Int?): Result<List<Sermon>> = withContext(ioDispatcher) {
        runCatching {
            val selectedShard = shard ?: kotlin.random.Random.nextInt(1, 81)
            apiService.getRandomSermons(selectedShard).sermons.map { it.toDomain() }
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
        cachedSpeakers?.let { return@withContext Result.success(it) }
        runCatching {
            val speakers = apiService.getSpeakers().speakers.map { it.toDomain() }
            cachedSpeakers = speakers
            speakers
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
        cachedTopics?.let { return@withContext Result.success(it) }
        runCatching {
            val topics = apiService.getTopics().topics.map { it.toDomain() }
            cachedTopics = topics
            topics
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
        userPreferencesRepository.bibleReadingPreferences

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
        userPreferencesRepository.saveBibleReadingPosition(translationId, bookId, chapter, verse)
    }

    override suspend fun saveBibleTypographyPreferences(
        fontSizeSp: Float,
        isSerif: Boolean,
        lineSpacingMultiplier: Float
    ) = withContext(ioDispatcher) {
        userPreferencesRepository.saveBibleTypography(fontSizeSp, isSerif, lineSpacingMultiplier)
    }

    override suspend fun saveBibleNarrator(narrator: String?) = withContext(ioDispatcher) {
        userPreferencesRepository.saveBibleNarrator(narrator)
    }

    override suspend fun clearHistory(): Unit = withContext(ioDispatcher) {
        historyDao.clearHistory()
    }

    override val bibleBookmarks: Flow<List<BibleBookmark>> =
        bibleBookmarksDao.getAllBookmarks()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun getBibleBookmarksForChapter(
        translationId: String,
        bookId: String,
        chapter: Int
    ): Flow<List<BibleBookmark>> =
        bibleBookmarksDao.getBookmarksForChapter(translationId, bookId, chapter)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun saveBibleBookmark(bookmark: BibleBookmark) = withContext(ioDispatcher) {
        bibleBookmarksDao.upsertBookmark(bookmark.toEntity())
    }

    override suspend fun deleteBibleBookmark(id: String): Unit = withContext(ioDispatcher) {
        bibleBookmarksDao.deleteBookmark(id)
    }

    override suspend fun deleteBibleBookmarkForVerse(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int
    ): Unit = withContext(ioDispatcher) {
        bibleBookmarksDao.deleteBookmarkForVerse(translationId, bookId, chapter, verse)
    }

    override val catalogSyncState: Flow<CatalogSyncState> = combine(
        searchDao.getCatalogMetadataFlow(),
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(CatalogSyncWorker.WORK_NAME),
        hasCatalogUpdateState
    ) { meta, workInfos, hasUpdate ->
        val activeWork = workInfos.firstOrNull()
        if (activeWork != null) {
            when (activeWork.state) {
                WorkInfo.State.RUNNING -> {
                    val progress = activeWork.progress.getFloat(CatalogSyncWorker.KEY_PROGRESS, 0f)
                    return@combine CatalogSyncState.Downloading(progress)
                }
                WorkInfo.State.ENQUEUED -> {
                    return@combine CatalogSyncState.Downloading(0f)
                }
                WorkInfo.State.FAILED -> {
                    val error = activeWork.outputData.getString(CatalogSyncWorker.KEY_ERROR) ?: "Sync failed"
                    return@combine CatalogSyncState.Error(error)
                }
                else -> { /* fall through to inspect database */ }
            }
        }

        val ftsCount = searchDao.getFtsSermonCount()
        if (meta != null && meta.sermonCount > 0 && ftsCount > 0) {
            CatalogSyncState.Ready(
                sermonCount = ftsCount,
                generatedAt = meta.generatedAt,
                hasUpdate = hasUpdate
            )
        } else {
            CatalogSyncState.NotDownloaded
        }
    }.flowOn(ioDispatcher)

    override suspend fun downloadOrUpdateCatalog(): Unit = withContext(ioDispatcher) {
        val workRequest = OneTimeWorkRequestBuilder<CatalogSyncWorker>()
            .addTag(CatalogSyncWorker.TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            CatalogSyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        Unit
    }

    override suspend fun deleteCatalog(): Unit = withContext(ioDispatcher) {
        searchDao.clearSermonsFts()
        searchDao.clearCatalogMetadata()
        hasCatalogUpdateState.value = false
    }

    override suspend fun checkForCatalogUpdate(): Boolean = withContext(ioDispatcher) {
        runCatching {
            val meta = searchDao.getCatalogMetadata() ?: return@withContext false
            val stats = apiService.getStats()
            if (stats.generated.isNotBlank() && stats.generated != meta.generatedAt) {
                hasCatalogUpdateState.value = true
                true
            } else {
                hasCatalogUpdateState.value = false
                false
            }
        }.getOrDefault(false)
    }

    override val recentSearchQueries: Flow<List<String>> =
        searchDao.getRecentSearchQueries().flowOn(ioDispatcher)

    override suspend fun addRecentSearchQuery(query: String) = withContext(ioDispatcher) {
        val clean = query.trim()
        if (clean.isNotBlank()) {
            searchDao.insertSearchQuery(SearchHistoryEntity(query = clean, timestamp = System.currentTimeMillis()))
        }
    }

    override suspend fun deleteRecentSearchQuery(query: String): Unit = withContext(ioDispatcher) {
        searchDao.deleteSearchQuery(query.trim())
    }

    override suspend fun clearRecentSearchQueries(): Unit = withContext(ioDispatcher) {
        searchDao.clearSearchHistory()
    }

    override val isCatalogReadyBannerDismissed: Flow<Boolean> =
        userPreferencesRepository.isCatalogReadyBannerDismissed

    override suspend fun setCatalogReadyBannerDismissed(dismissed: Boolean) {
        userPreferencesRepository.setCatalogReadyBannerDismissed(dismissed)
    }

    override fun search(query: String, category: SearchCategory): Flow<SearchAggregateResult> = kotlinx.coroutines.flow.flow {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            emit(SearchAggregateResult(query = query))
            return@flow
        }

        val allMatchingSpeakers = if (category == SearchCategory.ALL || category == SearchCategory.SPEAKERS) {
            val allSpeakers = cachedSpeakers ?: getSpeakers().getOrDefault(emptyList())
            allSpeakers.filter { speaker ->
                speaker.name.contains(cleanQuery, ignoreCase = true) ||
                    speaker.slug.contains(cleanQuery, ignoreCase = true)
            }
        } else emptyList()
        val totalSpeakerMatches = allMatchingSpeakers.size
        val matchingSpeakers = if (category == SearchCategory.ALL) allMatchingSpeakers.take(5) else allMatchingSpeakers

        val allMatchingTopics = if (category == SearchCategory.ALL || category == SearchCategory.TOPICS) {
            val allTopics = cachedTopics ?: getTopics().getOrDefault(emptyList())
            allTopics.filter { topic ->
                topic.name.contains(cleanQuery, ignoreCase = true) ||
                    topic.slug.contains(cleanQuery, ignoreCase = true)
            }
        } else emptyList()
        val totalTopicMatches = allMatchingTopics.size
        val matchingTopics = if (category == SearchCategory.ALL) allMatchingTopics.take(8) else allMatchingTopics

        val (matchingSermons, totalSermonMatches) = if (category == SearchCategory.ALL || category == SearchCategory.SERMONS) {
            val ftsCount = searchDao.getFtsSermonCount()
            val hasCatalog = ftsCount > 0
            if (hasCatalog) {
                val ftsQuery = formatFtsMatchQuery(cleanQuery)
                if (ftsQuery.isBlank()) {
                    Pair(emptyList(), 0)
                } else {
                    val list = searchDao.searchSermonsFts(ftsQuery, limit = if (category == SearchCategory.ALL) 10 else 100)
                        .map { it.toDomain() }
                    val totalCount = searchDao.countSermonsFts(ftsQuery)
                    Pair(list, totalCount)
                }
            } else {
                val popular = cachedPopularSermons ?: getPopularSermons().getOrDefault(emptyList())
                val favs = favorites.first().map { it.toSermon() }
                val hist = history.first().map { it.toSermon() }
                val combined = (popular + favs + hist).distinctBy { it.id }
                val filtered = combined.filter { sermon ->
                    sermon.title.contains(cleanQuery, ignoreCase = true) ||
                        sermon.speaker.contains(cleanQuery, ignoreCase = true)
                }
                val list = if (category == SearchCategory.ALL) filtered.take(10) else filtered
                Pair(list, filtered.size)
            }
        } else Pair(emptyList(), 0)

        emit(
            SearchAggregateResult(
                query = query,
                speakers = matchingSpeakers,
                totalSpeakerMatches = totalSpeakerMatches,
                topics = matchingTopics,
                totalTopicMatches = totalTopicMatches,
                sermons = matchingSermons,
                totalSermonMatches = totalSermonMatches
            )
        )
    }.flowOn(ioDispatcher)

    private fun formatFtsMatchQuery(query: String): String {
        val clean = query.trim()
        if (clean.isBlank()) return ""
        val tokens = clean.split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ""
        return tokens.joinToString(" ") { "$it*" }
    }
}
