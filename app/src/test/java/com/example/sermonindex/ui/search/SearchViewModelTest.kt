package com.example.sermonindex.ui.search

import com.example.sermonindex.domain.model.FavoriteSermon
import com.example.sermonindex.domain.model.SearchAggregateResult
import com.example.sermonindex.domain.model.SearchCategory
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.model.Topic
import com.example.sermonindex.domain.usecase.AddRecentSearchQueryUseCase
import com.example.sermonindex.domain.usecase.CheckCatalogUpdateUseCase
import com.example.sermonindex.domain.usecase.ClearRecentSearchQueriesUseCase
import com.example.sermonindex.domain.usecase.DeleteRecentSearchQueryUseCase
import com.example.sermonindex.domain.usecase.DownloadOrUpdateCatalogUseCase
import com.example.sermonindex.domain.usecase.GetCatalogPromptBannerDismissedUseCase
import com.example.sermonindex.domain.usecase.GetCatalogReadyBannerDismissedUseCase
import com.example.sermonindex.domain.usecase.GetCatalogSyncStateUseCase
import com.example.sermonindex.domain.usecase.GetFavoritesUseCase
import com.example.sermonindex.domain.usecase.GetRecentSearchQueriesUseCase
import com.example.sermonindex.domain.usecase.GetSermonDetailsUseCase
import com.example.sermonindex.domain.usecase.GetSpeakersUseCase
import com.example.sermonindex.domain.usecase.GetTopicsUseCase
import com.example.sermonindex.domain.usecase.SearchUseCase
import com.example.sermonindex.domain.usecase.SetCatalogPromptBannerDismissedUseCase
import com.example.sermonindex.domain.usecase.SetCatalogReadyBannerDismissedUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import com.example.sermonindex.testing.FakePlaybackManager
import com.example.sermonindex.testing.FakeSermonRepository
import com.example.sermonindex.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val fakePlaybackManager = FakePlaybackManager()

    private val fakeSpeaker = Speaker(id = "1", name = "A.W. Tozer", slug = "tozer", sermonCount = 100)
    private val fakeTopic = Topic(id = "1", name = "Prayer", slug = "prayer", count = 50)
    private val fakeSermon = Sermon(id = "1", title = "The Crucified Life", speaker = "A.W. Tozer")

    private lateinit var fakeRepository: FakeSermonRepository
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        fakePlaybackManager.reset()
        fakeRepository = FakeSermonRepository().apply {
            recentSearchQueriesFlow.value = listOf("tozer", "faith")
            popularSermonsResult = Result.success(listOf(fakeSermon))
            speakersResult = Result.success(listOf(fakeSpeaker))
            speakerDetailsResult = Result.success(fakeSpeaker)
            topicsResult = Result.success(listOf(fakeTopic))
            sermonDetailsProvider = { id ->
                Result.success(
                    SermonDetail(
                        id = id,
                        title = "The Crucified Life",
                        speaker = "A.W. Tozer",
                        mp3Url = "https://archive.org/download/tozer.mp3"
                    )
                )
            }
            searchProvider = { query, category ->
                flowOf(
                    SearchAggregateResult(
                        query = query,
                        speakers = if (category == SearchCategory.ALL || category == SearchCategory.SPEAKERS) listOf(fakeSpeaker) else emptyList(),
                        topics = if (category == SearchCategory.ALL || category == SearchCategory.TOPICS) listOf(fakeTopic) else emptyList(),
                        sermons = if (category == SearchCategory.ALL || category == SearchCategory.SERMONS) listOf(fakeSermon) else emptyList(),
                        totalSermonMatches = 1
                    )
                )
            }
        }

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
            getCatalogPromptBannerDismissedUseCase = GetCatalogPromptBannerDismissedUseCase(fakeRepository),
            setCatalogPromptBannerDismissedUseCase = SetCatalogPromptBannerDismissedUseCase(fakeRepository),
            getFavoritesUseCase = GetFavoritesUseCase(fakeRepository),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(fakeRepository),
            playbackManager = fakePlaybackManager,
            getSermonDetailsUseCase = GetSermonDetailsUseCase(fakeRepository),
            ioDispatcher = testDispatcher
        )
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
        assertTrue(fakeRepository.checkCatalogUpdateCalled)
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

        assertTrue(fakeRepository.addedSearchQueries.contains("revival"))
    }

    @Test
    fun catalogDownloadAndSync_delegatesToRepository() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(SearchUiEvent.DownloadCatalog)
        advanceUntilIdle()

        assertTrue(fakeRepository.downloadCatalogCalled)
    }

    @Test
    fun manageRecentQueries_handlesDeleteAndClear() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(SearchUiEvent.DeleteRecentQuery("tozer"))
        advanceUntilIdle()
        assertTrue(fakeRepository.deletedSearchQueries.contains("tozer"))

        viewModel.onEvent(SearchUiEvent.ClearRecentQueries)
        advanceUntilIdle()
        assertTrue(fakeRepository.clearRecentQueriesCalled)
    }

    @Test
    fun dismissCatalogReadyBanner_updatesStateAndRepository() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isCatalogReadyBannerDismissed)

        viewModel.onEvent(SearchUiEvent.DismissCatalogReadyBanner)
        advanceUntilIdle()

        assertTrue(fakeRepository.catalogReadyBannerDismissedFlow.value)
        assertTrue(viewModel.uiState.value.isCatalogReadyBannerDismissed)
    }

    @Test
    fun dismissCatalogPromptBanner_updatesStateAndRepository() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isCatalogPromptBannerDismissed)

        viewModel.onEvent(SearchUiEvent.DismissCatalogPromptBanner)
        advanceUntilIdle()

        assertTrue(fakeRepository.catalogPromptBannerDismissedFlow.value)
        assertTrue(viewModel.uiState.value.isCatalogPromptBannerDismissed)
    }

    @Test
    fun toggleFavorite_invokesUseCase() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(SearchUiEvent.ToggleFavorite(fakeSermon))
        advanceUntilIdle()
        assertEquals(fakeSermon, fakeRepository.lastToggledFavorite)
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

        fakeRepository.favoritesFlow.value = listOf(
            FavoriteSermon(id = "1", title = "The Crucified Life", speaker = "A.W. Tozer")
        )
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.favoriteIds.contains("1"))
    }
}
