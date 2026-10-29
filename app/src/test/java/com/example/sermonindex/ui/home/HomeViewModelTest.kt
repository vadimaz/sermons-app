package com.example.sermonindex.ui.home

import app.cash.turbine.test
import com.example.sermonindex.domain.usecase.GetFavoritesUseCase
import com.example.sermonindex.domain.usecase.GetHistoryUseCase
import com.example.sermonindex.domain.usecase.GetPopularSermonsUseCase
import com.example.sermonindex.domain.usecase.GetRandomSermonsUseCase
import com.example.sermonindex.domain.usecase.GetSermonDetailsUseCase
import com.example.sermonindex.domain.usecase.GetSpeakersUseCase
import com.example.sermonindex.domain.usecase.GetTopicsUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import com.example.sermonindex.testing.FakePlaybackManager
import com.example.sermonindex.testing.FakeSermonRepository
import com.example.sermonindex.testing.MainDispatcherRule
import com.example.sermonindex.testing.createTestFavorite
import com.example.sermonindex.testing.createTestHistory
import com.example.sermonindex.testing.createTestSermon
import com.example.sermonindex.testing.createTestSpeaker
import com.example.sermonindex.testing.createTestTopic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var fakeRepository: FakeSermonRepository
    private lateinit var fakePlaybackManager: FakePlaybackManager
    private lateinit var viewModel: HomeViewModel

    private val popularSermon = createTestSermon(id = "pop-1", title = "Revival Praying")
    private val randomSermon = createTestSermon(id = "rnd-1", title = "Holiness")
    private val speaker = createTestSpeaker(id = "spk-1", name = "Leonard Ravenhill")
    private val topic = createTestTopic(id = "top-1", name = "Prayer")

    @Before
    fun setUp() {
        fakeRepository = FakeSermonRepository()
        fakePlaybackManager = FakePlaybackManager()

        fakeRepository.setPopularSermons(listOf(popularSermon))
        fakeRepository.setRandomSermons(listOf(randomSermon))
        fakeRepository.setSpeakers(listOf(speaker))
        fakeRepository.setTopics(listOf(topic))

        viewModel = HomeViewModel(
            getPopularSermonsUseCase = GetPopularSermonsUseCase(fakeRepository),
            getRandomSermonsUseCase = GetRandomSermonsUseCase(fakeRepository),
            getSpeakersUseCase = GetSpeakersUseCase(fakeRepository),
            getTopicsUseCase = GetTopicsUseCase(fakeRepository),
            getFavoritesUseCase = GetFavoritesUseCase(fakeRepository),
            getHistoryUseCase = GetHistoryUseCase(fakeRepository),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(fakeRepository),
            getSermonDetailsUseCase = GetSermonDetailsUseCase(fakeRepository),
            playbackManager = fakePlaybackManager
        )
    }

    @Test
    fun initialLoad_populatesContentAndClearsLoading() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.popularSermons.size)
        assertEquals("pop-1", state.popularSermons[0].id)
        assertEquals(1, state.speakers.size)
        assertEquals("spk-1", state.speakers[0].id)
        assertEquals(1, state.topics.size)
        assertEquals("top-1", state.topics[0].id)
        assertEquals(1, state.randomSermons.size)
        assertEquals("rnd-1", state.randomSermons[0].id)
    }

    @Test
    fun favoritesAndHistoryFlows_updateUiState() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val current = awaitItem()
            assertTrue(current.favoriteIds.isEmpty())
            assertTrue(current.history.isEmpty())

            fakeRepository.setFavorites(listOf(createTestFavorite(id = "fav-1")))
            testDispatcher.scheduler.advanceUntilIdle()

            val withFav = awaitItem()
            assertTrue(withFav.favoriteIds.contains("fav-1"))

            fakeRepository.setHistory(listOf(createTestHistory(sermonId = "hist-1")))
            testDispatcher.scheduler.advanceUntilIdle()

            val withHist = awaitItem()
            assertEquals(1, withHist.history.size)
            assertEquals("hist-1", withHist.history[0].sermonId)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun playSermonEvent_delegatesToPlaybackManager() = runTest {
        viewModel.onEvent(HomeUiEvent.PlaySermon(popularSermon))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("pop-1", fakePlaybackManager.lastPlayedSermon?.id)
        assertTrue(fakePlaybackManager.playbackState.value.isPlaying)
    }

    @Test
    fun toggleFavoriteEvent_invokesRepository() = runTest {
        viewModel.onEvent(HomeUiEvent.ToggleFavorite(popularSermon))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeRepository.isFavoriteSync("pop-1"))
    }
}
