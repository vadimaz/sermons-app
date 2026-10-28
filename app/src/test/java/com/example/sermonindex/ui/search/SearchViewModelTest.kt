package com.example.sermonindex.ui.search

import com.example.sermonindex.domain.model.CatalogSyncState
import com.example.sermonindex.domain.model.SearchAggregateResult
import com.example.sermonindex.domain.model.SearchCategory
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.model.Topic
import com.example.sermonindex.domain.repository.SermonRepository
import com.example.sermonindex.domain.usecase.AddRecentSearchQueryUseCase
import com.example.sermonindex.domain.usecase.CheckCatalogUpdateUseCase
import com.example.sermonindex.domain.usecase.ClearRecentSearchQueriesUseCase
import com.example.sermonindex.domain.usecase.DeleteRecentSearchQueryUseCase
import com.example.sermonindex.domain.usecase.DownloadOrUpdateCatalogUseCase
import com.example.sermonindex.domain.usecase.GetCatalogReadyBannerDismissedUseCase
import com.example.sermonindex.domain.usecase.GetCatalogSyncStateUseCase
import com.example.sermonindex.domain.usecase.GetFavoritesUseCase
import com.example.sermonindex.domain.usecase.GetRecentSearchQueriesUseCase
import com.example.sermonindex.domain.usecase.GetSermonDetailsUseCase
import com.example.sermonindex.domain.usecase.GetSpeakersUseCase
import com.example.sermonindex.domain.usecase.GetTopicsUseCase
import com.example.sermonindex.domain.usecase.SearchUseCase
import com.example.sermonindex.domain.usecase.SetCatalogReadyBannerDismissedUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import com.example.sermonindex.playback.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val catalogSyncStateFlow = MutableStateFlow<CatalogSyncState>(CatalogSyncState.NotDownloaded)
    private val recentQueriesFlow = MutableStateFlow<List<String>>(listOf("tozer", "faith"))
    private val bannerDismissedFlow = MutableStateFlow(false)
    private val favoritesFlow = MutableStateFlow<List<com.example.sermonindex.domain.model.FavoriteSermon>>(emptyList())
    private var lastToggledSermon: Sermon? = null

    private val fakePlaybackManager = object : PlaybackManager(null) {
        var lastPlayedSermon: Sermon? = null
        override fun playSermon(sermon: Sermon, startPositionMs: Long) {
            lastPlayedSermon = sermon
        }
    }

    private var downloadCatalogCalled = false
    private var checkCatalogUpdateCalled = false
    private val addedQueries = mutableListOf<String>()
    private val deletedQueries = mutableListOf<String>()
    private var clearedQueries = false

    private val fakeSpeaker = Speaker(id = "1", name = "A.W. Tozer", slug = "tozer", sermonCount = 100)
    private val fakeTopic = Topic(id = "1", name = "Prayer", slug = "prayer", count = 50)
    private val fakeSermon = Sermon(id = "1", title = "The Crucified Life", speaker = "A.W. Tozer")

    private val fakeRepository = object : SermonRepository {
        override val favorites = favoritesFlow.asStateFlow()
        override val history = emptyFlow<List<com.example.sermonindex.domain.model.ListeningHistory>>()
        override val downloads = emptyFlow<List<com.example.sermonindex.domain.model.DownloadedSermon>>()
        override fun isFavorite(id: String) = flowOf(false)
        override suspend fun toggleFavorite(sermon: Sermon) {
            lastToggledSermon = sermon
        }
        override fun isDownloaded(sermonId: String) = flowOf(false)
        override fun getDownloadProgress(sermonId: String) = flowOf(null)
        override suspend fun downloadSermon(sermon: Sermon) {}
        override suspend fun cancelDownload(sermonId: String) {}
        override suspend fun deleteDownload(sermonId: String) {}
        override suspend fun deleteAllDownloads() {}
        override suspend fun getLocalAudioPath(sermonId: String) = null
        override suspend fun updateHistory(sermonId: String, title: String, speaker: String, speakerImage: String, positionMs: Long, durationMs: Long) {}
        override suspend fun getPopularSermons() = Result.success(listOf(fakeSermon))
        override suspend fun getScriptureBooks() = Result.success(emptyList<com.example.sermonindex.domain.model.BibleBook>())
        override suspend fun getScriptureChapter(bookId: String, chapter: Int) = Result.failure<com.example.sermonindex.domain.model.ChapterDetails>(NotImplementedError())
        override suspend fun getScriptureVerse(bookId: String, chapter: Int, verse: Int) = Result.success(emptyList<Sermon>())
        override suspend fun getAvailableBibleTranslations() = Result.success(emptyList<com.example.sermonindex.domain.model.BibleTranslation>())
        override suspend fun getBibleVerseText(translationId: String, bookId: String, chapter: Int, verse: Int) = Result.success("")
        override suspend fun getSpeakers() = Result.success(listOf(fakeSpeaker))
        override suspend fun getSpeakerDetails(slug: String) = Result.success(fakeSpeaker)
        override suspend fun getSpeakerSermonsPage(slug: String, page: Int) = Result.failure<com.example.sermonindex.domain.model.SpeakerPagedSermons>(NotImplementedError())
        override suspend fun getTopics() = Result.success(listOf(fakeTopic))
        override suspend fun getTopicDetails(slug: String) = Result.failure<com.example.sermonindex.domain.model.TopicDetail>(NotImplementedError())
        override suspend fun getSermonDetails(id: String) = Result.success(
            com.example.sermonindex.domain.model.SermonDetail(
                id = id,
                title = "The Crucified Life",
                speaker = "A.W. Tozer",
                mp3Url = "https://archive.org/download/tozer.mp3"
            )
        )
        override suspend fun getTranscriptCues(vttUrl: String) = Result.success(emptyList<com.example.sermonindex.domain.model.TranscriptCue>())
        override val bibleReadingPreferences = flowOf(com.example.sermonindex.domain.model.BibleReadingPreferences())
        override suspend fun getBibleChapter(translationId: String, bookId: String, chapter: Int) = Result.failure<com.example.sermonindex.domain.model.BibleChapter>(NotImplementedError())
        override suspend fun getBibleBooksForTranslation(translationId: String) = Result.success(emptyList<com.example.sermonindex.domain.model.BibleBook>())
        override suspend fun saveBibleReadingPosition(translationId: String, bookId: String, chapter: Int, verse: Int) {}
        override suspend fun saveBibleTypographyPreferences(fontSizeSp: Float, isSerif: Boolean, lineSpacingMultiplier: Float) {}
        override suspend fun saveBibleNarrator(narrator: String?) {}
        override suspend fun clearHistory() {}

        override fun search(query: String, category: SearchCategory): Flow<SearchAggregateResult> = flowOf(
            SearchAggregateResult(
                query = query,
                speakers = if (category == SearchCategory.ALL || category == SearchCategory.SPEAKERS) listOf(fakeSpeaker) else emptyList(),
                topics = if (category == SearchCategory.ALL || category == SearchCategory.TOPICS) listOf(fakeTopic) else emptyList(),
                sermons = if (category == SearchCategory.ALL || category == SearchCategory.SERMONS) listOf(fakeSermon) else emptyList(),
                totalSermonMatches = 1
            )
        )

        override val catalogSyncState: Flow<CatalogSyncState> = catalogSyncStateFlow.asStateFlow()

        override suspend fun downloadOrUpdateCatalog() {
            downloadCatalogCalled = true
        }

        override suspend fun checkForCatalogUpdate(): Boolean {
            checkCatalogUpdateCalled = true
            return false
        }

        override val recentSearchQueries: Flow<List<String>> = recentQueriesFlow.asStateFlow()

        override suspend fun addRecentSearchQuery(query: String) {
            addedQueries.add(query)
        }

        override suspend fun deleteRecentSearchQuery(query: String) {
            deletedQueries.add(query)
        }

        override suspend fun clearRecentSearchQueries() {
            clearedQueries = true
        }

        override val isCatalogReadyBannerDismissed: Flow<Boolean> = bannerDismissedFlow.asStateFlow()

        override suspend fun setCatalogReadyBannerDismissed(dismissed: Boolean) {
            bannerDismissedFlow.value = dismissed
        }
    }

    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SearchViewModel(
            searchUseCase = SearchUseCase(fakeRepository),
            getCatalogSyncStateUseCase = GetCatalogSyncStateUseCase(fakeRepository),
            downloadOrUpdateCatalogUseCase = DownloadOrUpdateCatalogUseCase(fakeRepository),
            checkCatalogUpdateUseCase = CheckCatalogUpdateUseCase(fakeRepository),
            getRecentSearchQueriesUseCase = GetRecentSearchQueriesUseCase(fakeRepository),
            addRecentSearchQueryUseCase = AddRecentSearchQueryUseCase(fakeRepository),
            deleteRecentSearchQueryUseCase = DeleteRecentSearchQueryUseCase(fakeRepository),
            clearRecentSearchQueriesUseCase = ClearRecentSearchQueriesUseCase(fakeRepository),
            getSpeakersUseCase = GetSpeakersUseCase(fakeRepository),
            getTopicsUseCase = GetTopicsUseCase(fakeRepository),
            getCatalogReadyBannerDismissedUseCase = GetCatalogReadyBannerDismissedUseCase(fakeRepository),
            setCatalogReadyBannerDismissedUseCase = SetCatalogReadyBannerDismissedUseCase(fakeRepository),
            getFavoritesUseCase = GetFavoritesUseCase(fakeRepository),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(fakeRepository),
            playbackManager = fakePlaybackManager,
            getSermonDetailsUseCase = GetSermonDetailsUseCase(fakeRepository),
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsSuggestionsAndRecentQueries() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("", state.query)
        assertEquals(SearchCategory.ALL, state.selectedCategory)
        assertEquals(listOf("tozer", "faith"), state.recentQueries)
        assertEquals(1, state.popularSpeakers.size)
        assertEquals(1, state.popularTopics.size)
        assertTrue(checkCatalogUpdateCalled)
    }

    @Test
    fun updateQuery_debouncesAndExecutesSearch() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(SearchUiEvent.UpdateQuery("tozer"))

        // Before debounce delay (250ms), results should not be updated yet
        advanceTimeBy(100L)
        assertEquals("", viewModel.uiState.value.searchResult.query)

        // After debounce delay, search executes
        advanceTimeBy(200L)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("tozer", state.query)
        assertEquals("tozer", state.searchResult.query)
        assertEquals(1, state.searchResult.speakers.size)
        assertEquals(1, state.searchResult.topics.size)
        assertEquals(1, state.searchResult.sermons.size)
    }

    @Test
    fun selectCategory_filtersSearchCategories() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(SearchUiEvent.UpdateQuery("prayer"))
        advanceTimeBy(300L)
        advanceUntilIdle()

        viewModel.onEvent(SearchUiEvent.SelectCategory(SearchCategory.TOPICS))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SearchCategory.TOPICS, state.selectedCategory)
        assertEquals(0, state.searchResult.speakers.size)
        assertEquals(1, state.searchResult.topics.size)
    }

    @Test
    fun clearQuery_resetsQueryAndResults() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(SearchUiEvent.UpdateQuery("grace"))
        advanceTimeBy(300L)
        advanceUntilIdle()

        viewModel.onEvent(SearchUiEvent.ClearQuery)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.query)
        assertEquals("", state.searchResult.query)
    }

    @Test
    fun executeSearch_recordsRecentQuery() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(SearchUiEvent.ExecuteSearch("revival"))
        advanceUntilIdle()

        assertTrue(addedQueries.contains("revival"))
    }

    @Test
    fun catalogDownloadAndSync_delegatesToRepository() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(SearchUiEvent.DownloadCatalog)
        advanceUntilIdle()

        assertTrue(downloadCatalogCalled)
    }

    @Test
    fun manageRecentQueries_handlesDeleteAndClear() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(SearchUiEvent.DeleteRecentQuery("tozer"))
        advanceUntilIdle()
        assertTrue(deletedQueries.contains("tozer"))

        viewModel.onEvent(SearchUiEvent.ClearRecentQueries)
        advanceUntilIdle()
        assertTrue(clearedQueries)
    }

    @Test
    fun dismissCatalogReadyBanner_updatesStateAndRepository() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isCatalogReadyBannerDismissed)

        viewModel.onEvent(SearchUiEvent.DismissCatalogReadyBanner)
        advanceUntilIdle()

        assertTrue(bannerDismissedFlow.value)
        assertTrue(viewModel.uiState.value.isCatalogReadyBannerDismissed)
    }

    @Test
    fun toggleFavorite_invokesUseCase() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(SearchUiEvent.ToggleFavorite(fakeSermon))
        advanceUntilIdle()
        assertEquals(fakeSermon, lastToggledSermon)
    }

    @Test
    fun playSermon_withBlankMp3_fetchesDetailsAndInvokesPlaybackManager() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(SearchUiEvent.PlaySermon(fakeSermon))
        advanceUntilIdle()
        assertEquals("1", fakePlaybackManager.lastPlayedSermon?.id)
        assertEquals("https://archive.org/download/tozer.mp3", fakePlaybackManager.lastPlayedSermon?.mp3Url)
    }

    @Test
    fun playSermon_withMp3_invokesPlaybackManagerDirectly() = runTest(testDispatcher) {
        advanceUntilIdle()
        val directSermon = fakeSermon.copy(mp3Url = "https://example.com/audio.mp3")
        viewModel.onEvent(SearchUiEvent.PlaySermon(directSermon))
        assertEquals(directSermon, fakePlaybackManager.lastPlayedSermon)
    }

    @Test
    fun favoritesObservation_updatesFavoriteIds() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.favoriteIds.isEmpty())

        favoritesFlow.value = listOf(com.example.sermonindex.domain.model.FavoriteSermon(id = "1", title = "The Crucified Life", speaker = "A.W. Tozer"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.favoriteIds.contains("1"))
    }
}
