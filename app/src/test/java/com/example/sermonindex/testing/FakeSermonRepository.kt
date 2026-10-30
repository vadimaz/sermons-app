package com.example.sermonindex.testing

import com.example.sermonindex.domain.model.BibleBook
import com.example.sermonindex.domain.model.BibleBookmark
import com.example.sermonindex.domain.model.BibleChapter
import com.example.sermonindex.domain.model.BibleReadingPreferences
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
import com.example.sermonindex.domain.model.Topic
import com.example.sermonindex.domain.model.TopicDetail
import com.example.sermonindex.domain.model.TranscriptCue
import com.example.sermonindex.domain.repository.SermonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * State-backed Fake implementation of [SermonRepository] for unit tests.
 *
 * Uses [MutableStateFlow] and configurable properties/lambdas so tests can emit reactive states,
 * simulate errors, and inspect recorded invocations without mocking frameworks.
 */
open class FakeSermonRepository : SermonRepository {

    val favoritesFlow = MutableStateFlow<List<FavoriteSermon>>(emptyList())
    val historyFlow = MutableStateFlow<List<ListeningHistory>>(emptyList())
    val downloadsFlow = MutableStateFlow<List<DownloadedSermon>>(emptyList())
    val downloadProgressMap = mutableMapOf<String, Int?>()
    val bibleReadingPreferencesFlow = MutableStateFlow(BibleReadingPreferences())
    val bibleBookmarksFlow = MutableStateFlow<List<BibleBookmark>>(emptyList())
    val catalogSyncStateFlow = MutableStateFlow<CatalogSyncState>(CatalogSyncState.NotDownloaded)
    val recentSearchQueriesFlow = MutableStateFlow<List<String>>(emptyList())
    val catalogReadyBannerDismissedFlow = MutableStateFlow(false)

    // Recorded invocations for state/interaction verification
    var lastToggledFavorite: Sermon? = null
    var lastDownloadedSermon: Sermon? = null
    var lastCancelledDownloadId: String? = null
    var lastDeletedDownloadId: String? = null
    var deleteAllDownloadsCalled: Boolean = false
    var clearHistoryCalled: Boolean = false
    var downloadCatalogCalled: Boolean = false
    var deleteCatalogCalled: Boolean = false
    var recordSermonViewCalled: Boolean = false
    var checkCatalogUpdateCalled: Boolean = false
    var checkForCatalogUpdateResult: Boolean = false
    var requestedRandomShard: Int? = null
    var savedReadingPosition: Triple<String, String, Int>? = null
    var savedTypography: Triple<Float, Boolean, Float>? = null
    var savedNarrator: String? = null

    val savedBookmarks = mutableListOf<BibleBookmark>()
    val deletedBookmarkIds = mutableListOf<String>()
    val deletedBookmarkVerses = mutableListOf<Triple<String, String, Pair<Int, Int>>>()
    val addedSearchQueries = mutableListOf<String>()
    val deletedSearchQueries = mutableListOf<String>()
    var clearRecentQueriesCalled: Boolean = false

    // Configurable one-shot results
    var popularSermonsResult: Result<List<Sermon>> = Result.success(emptyList())
    var randomSermonsResult: Result<List<Sermon>> = Result.success(emptyList())
    var scriptureBooksResult: Result<List<BibleBook>> = Result.success(emptyList())
    var scriptureChapterResult: Result<ChapterDetails> = Result.failure(NotImplementedError())
    var scriptureVerseResult: Result<List<Sermon>> = Result.success(emptyList())
    var availableBibleTranslationsResult: Result<List<BibleTranslation>> = Result.success(emptyList())
    var bibleVerseTextResult: Result<String> = Result.success("")
    var speakersResult: Result<List<Speaker>> = Result.success(emptyList())
    var speakerDetailsResult: Result<Speaker> = Result.failure(NotImplementedError())
    var speakerSermonsPageProvider: (slug: String, page: Int) -> Result<SpeakerPagedSermons> =
        { _, _ -> Result.failure(NotImplementedError()) }
    var topicsResult: Result<List<Topic>> = Result.success(emptyList())
    var topicDetailsResult: Result<TopicDetail> = Result.failure(NotImplementedError())
    var sermonDetailsProvider: (id: String) -> Result<SermonDetail> =
        { Result.failure(NotImplementedError()) }
    var transcriptCuesResult: Result<List<TranscriptCue>> = Result.success(emptyList())
    var bibleChapterResult: Result<BibleChapter> = Result.failure(NotImplementedError())
    var bibleBooksForTranslationResult: Result<List<BibleBook>> = Result.success(emptyList())
    var searchProvider: (query: String, category: SearchCategory) -> Flow<SearchAggregateResult> =
        { query, _ -> flowOf(SearchAggregateResult(query = query)) }

    override val favorites: Flow<List<FavoriteSermon>> = favoritesFlow.asStateFlow()
    override val history: Flow<List<ListeningHistory>> = historyFlow.asStateFlow()
    override val downloads: Flow<List<DownloadedSermon>> = downloadsFlow.asStateFlow()

    override fun isFavorite(id: String): Flow<Boolean> =
        favoritesFlow.map { list -> list.any { it.id == id } }

    override suspend fun toggleFavorite(sermon: Sermon) {
        lastToggledFavorite = sermon
        val current = favoritesFlow.value
        favoritesFlow.value = if (current.any { it.id == sermon.id }) {
            current.filterNot { it.id == sermon.id }
        } else {
            current + FavoriteSermon(
                id = sermon.id,
                title = sermon.title,
                speaker = sermon.speaker,
                speakerImage = sermon.speakerImage,
                duration = sermon.duration
            )
        }
    }

    override fun isDownloaded(sermonId: String): Flow<Boolean> =
        downloadsFlow.map { list -> list.any { it.id == sermonId } }

    override fun getDownloadProgress(sermonId: String): Flow<Int?> =
        flowOf(downloadProgressMap[sermonId])

    override suspend fun downloadSermon(sermon: Sermon) {
        lastDownloadedSermon = sermon
    }

    override suspend fun cancelDownload(sermonId: String) {
        lastCancelledDownloadId = sermonId
        downloadProgressMap.remove(sermonId)
    }

    override suspend fun deleteDownload(sermonId: String) {
        lastDeletedDownloadId = sermonId
        downloadsFlow.value = downloadsFlow.value.filterNot { it.id == sermonId }
        downloadProgressMap.remove(sermonId)
    }

    override suspend fun deleteAllDownloads() {
        deleteAllDownloadsCalled = true
        downloadsFlow.value = emptyList()
        downloadProgressMap.clear()
    }

    override suspend fun getLocalAudioPath(sermonId: String): String? =
        downloadsFlow.value.firstOrNull { it.id == sermonId }?.localPath

    override suspend fun updateHistory(
        sermonId: String,
        title: String,
        speaker: String,
        speakerImage: String,
        positionMs: Long,
        durationMs: Long
    ) {
        val entry = ListeningHistory(
            sermonId = sermonId,
            title = title,
            speaker = speaker,
            speakerImage = speakerImage,
            lastPositionMs = positionMs,
            durationMs = durationMs,
            updatedAt = System.currentTimeMillis(),
            completed = durationMs > 0 && positionMs >= durationMs
        )
        historyFlow.value = historyFlow.value.filterNot { it.sermonId == sermonId } + entry
    }

    override suspend fun recordSermonView(
        sermonId: String,
        title: String,
        speaker: String,
        speakerImage: String
    ) {
        recordSermonViewCalled = true
        val existing = historyFlow.value.firstOrNull { it.sermonId == sermonId }
        val entry = ListeningHistory(
            sermonId = sermonId,
            title = title,
            speaker = speaker,
            speakerImage = speakerImage,
            lastPositionMs = existing?.lastPositionMs ?: 0L,
            durationMs = existing?.durationMs ?: 0L,
            updatedAt = System.currentTimeMillis(),
            completed = existing?.completed ?: false
        )
        historyFlow.value = listOf(entry) + historyFlow.value.filterNot { it.sermonId == sermonId }
    }

    override suspend fun clearHistory() {
        clearHistoryCalled = true
        historyFlow.value = emptyList()
    }

    override suspend fun getPopularSermons(): Result<List<Sermon>> = popularSermonsResult

    override suspend fun getRandomSermons(shard: Int?): Result<List<Sermon>> {
        requestedRandomShard = shard
        return randomSermonsResult
    }

    override suspend fun getScriptureBooks(): Result<List<BibleBook>> = scriptureBooksResult

    override suspend fun getScriptureChapter(bookId: String, chapter: Int): Result<ChapterDetails> =
        scriptureChapterResult

    override suspend fun getScriptureVerse(bookId: String, chapter: Int, verse: Int): Result<List<Sermon>> =
        scriptureVerseResult

    override suspend fun getAvailableBibleTranslations(): Result<List<BibleTranslation>> =
        availableBibleTranslationsResult

    override suspend fun getBibleVerseText(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int
    ): Result<String> = bibleVerseTextResult

    override suspend fun getSpeakers(): Result<List<Speaker>> = speakersResult

    override suspend fun getSpeakerDetails(slug: String): Result<Speaker> = speakerDetailsResult

    override suspend fun getSpeakerSermonsPage(slug: String, page: Int): Result<SpeakerPagedSermons> =
        speakerSermonsPageProvider(slug, page)

    override suspend fun getTopics(): Result<List<Topic>> = topicsResult

    override suspend fun getTopicDetails(slug: String): Result<TopicDetail> = topicDetailsResult

    override suspend fun getSermonDetails(id: String): Result<SermonDetail> = sermonDetailsProvider(id)

    override suspend fun getTranscriptCues(vttUrl: String): Result<List<TranscriptCue>> =
        transcriptCuesResult

    override val bibleReadingPreferences: Flow<BibleReadingPreferences> =
        bibleReadingPreferencesFlow.asStateFlow()

    override suspend fun getBibleChapter(
        translationId: String,
        bookId: String,
        chapter: Int
    ): Result<BibleChapter> = bibleChapterResult

    override suspend fun getBibleBooksForTranslation(translationId: String): Result<List<BibleBook>> =
        bibleBooksForTranslationResult

    override suspend fun saveBibleReadingPosition(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int
    ) {
        savedReadingPosition = Triple(translationId, bookId, chapter)
        bibleReadingPreferencesFlow.value = bibleReadingPreferencesFlow.value.copy(
            translationId = translationId,
            bookId = bookId,
            chapter = chapter,
            verse = verse
        )
    }

    override suspend fun saveBibleTypographyPreferences(
        fontSizeSp: Float,
        isSerif: Boolean,
        lineSpacingMultiplier: Float
    ) {
        savedTypography = Triple(fontSizeSp, isSerif, lineSpacingMultiplier)
        bibleReadingPreferencesFlow.value = bibleReadingPreferencesFlow.value.copy(
            fontSizeSp = fontSizeSp,
            isSerif = isSerif,
            lineSpacingMultiplier = lineSpacingMultiplier
        )
    }

    override suspend fun saveBibleNarrator(narrator: String?) {
        savedNarrator = narrator
        bibleReadingPreferencesFlow.value = bibleReadingPreferencesFlow.value.copy(
            selectedNarrator = narrator
        )
    }

    override val bibleBookmarks: Flow<List<BibleBookmark>> = bibleBookmarksFlow.asStateFlow()

    override fun getBibleBookmarksForChapter(
        translationId: String,
        bookId: String,
        chapter: Int
    ): Flow<List<BibleBookmark>> = bibleBookmarksFlow.map { list ->
        list.filter {
            it.translationId == translationId && it.bookId == bookId && it.chapter == chapter
        }
    }

    override suspend fun saveBibleBookmark(bookmark: BibleBookmark) {
        savedBookmarks.add(bookmark)
        bibleBookmarksFlow.value = bibleBookmarksFlow.value + bookmark
    }

    override suspend fun deleteBibleBookmark(id: String) {
        deletedBookmarkIds.add(id)
        bibleBookmarksFlow.value = bibleBookmarksFlow.value.filterNot { it.id == id }
    }

    override suspend fun deleteBibleBookmarkForVerse(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int
    ) {
        deletedBookmarkVerses.add(Triple(translationId, bookId, Pair(chapter, verse)))
        bibleBookmarksFlow.value = bibleBookmarksFlow.value.filterNot {
            it.translationId == translationId && it.bookId == bookId && it.chapter == chapter && it.verse == verse
        }
    }

    override fun search(query: String, category: SearchCategory): Flow<SearchAggregateResult> =
        searchProvider(query, category)

    override val catalogSyncState: Flow<CatalogSyncState> = catalogSyncStateFlow.asStateFlow()

    override suspend fun downloadOrUpdateCatalog() {
        downloadCatalogCalled = true
    }

    override suspend fun deleteCatalog() {
        deleteCatalogCalled = true
        catalogSyncStateFlow.value = CatalogSyncState.NotDownloaded
    }

    override suspend fun checkForCatalogUpdate(): Boolean {
        checkCatalogUpdateCalled = true
        return checkForCatalogUpdateResult
    }

    override val recentSearchQueries: Flow<List<String>> = recentSearchQueriesFlow.asStateFlow()

    override suspend fun addRecentSearchQuery(query: String) {
        addedSearchQueries.add(query)
        recentSearchQueriesFlow.value = listOf(query) + recentSearchQueriesFlow.value.filterNot { it == query }
    }

    override suspend fun deleteRecentSearchQuery(query: String) {
        deletedSearchQueries.add(query)
        recentSearchQueriesFlow.value = recentSearchQueriesFlow.value.filterNot { it == query }
    }

    override suspend fun clearRecentSearchQueries() {
        clearRecentQueriesCalled = true
        recentSearchQueriesFlow.value = emptyList()
    }

    override val isCatalogReadyBannerDismissed: Flow<Boolean> =
        catalogReadyBannerDismissedFlow.asStateFlow()

    override suspend fun setCatalogReadyBannerDismissed(dismissed: Boolean) {
        catalogReadyBannerDismissedFlow.value = dismissed
    }

    fun setPopularSermons(list: List<Sermon>) {
        popularSermonsResult = Result.success(list)
    }

    fun setRandomSermons(list: List<Sermon>) {
        randomSermonsResult = Result.success(list)
    }

    fun setSpeakers(list: List<Speaker>) {
        speakersResult = Result.success(list)
    }

    fun setTopics(list: List<Topic>) {
        topicsResult = Result.success(list)
    }

    fun setFavorites(list: List<FavoriteSermon>) {
        favoritesFlow.value = list
    }

    fun setHistory(list: List<ListeningHistory>) {
        historyFlow.value = list
    }

    fun isFavoriteSync(id: String): Boolean = favoritesFlow.value.any { it.id == id }
}
