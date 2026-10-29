package com.example.sermonindex.ui.sermon

import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.domain.usecase.DeleteDownloadUseCase
import com.example.sermonindex.domain.usecase.DownloadSermonUseCase
import com.example.sermonindex.domain.usecase.GetDownloadProgressUseCase
import com.example.sermonindex.domain.usecase.GetSermonDetailsUseCase
import com.example.sermonindex.domain.usecase.IsDownloadedUseCase
import com.example.sermonindex.domain.usecase.IsFavoriteUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import com.example.sermonindex.playback.PlaybackState
import com.example.sermonindex.testing.FakePlaybackManager
import com.example.sermonindex.testing.FakeSermonRepository
import com.example.sermonindex.testing.MainDispatcherRule
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
class SermonDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val fakePlaybackManager = FakePlaybackManager()

    private val testSermonDetail = SermonDetail(
        id = "sermon-1",
        title = "Faith and Prayer",
        speaker = "A.W. Tozer",
        mp3Url = "https://example.com/sermon1.mp3"
    )

    private lateinit var fakeRepository: FakeSermonRepository
    private lateinit var viewModel: SermonDetailViewModel

    @Before
    fun setUp() {
        fakePlaybackManager.reset()
        fakeRepository = FakeSermonRepository().apply {
            sermonDetailsProvider = { Result.success(testSermonDetail) }
        }

        viewModel = SermonDetailViewModel(
            sermonId = "sermon-1",
            getSermonDetailsUseCase = GetSermonDetailsUseCase(fakeRepository),
            isFavoriteUseCase = IsFavoriteUseCase(fakeRepository),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(fakeRepository),
            isDownloadedUseCase = IsDownloadedUseCase(fakeRepository),
            getDownloadProgressUseCase = GetDownloadProgressUseCase(fakeRepository),
            downloadSermonUseCase = DownloadSermonUseCase(fakeRepository),
            deleteDownloadUseCase = DeleteDownloadUseCase(fakeRepository),
            playbackManager = fakePlaybackManager
        )
    }

    @Test
    fun initialState_hasDetailAndIsNotActiveInPlayer() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("sermon-1", state.detail?.id)
        assertFalse(state.isActiveInPlayer)
        assertFalse(state.isPlaying)
    }

    @Test
    fun whenMatchingSermonIsPlaying_isActiveInPlayerAndIsPlayingAreTrue() = runTest(testDispatcher) {
        advanceUntilIdle()

        fakePlaybackManager.playbackStateFlow.value = PlaybackState(
            currentSermon = testSermonDetail.toSermon(),
            isPlaying = true
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isActiveInPlayer)
        assertTrue(state.isPlaying)
    }

    @Test
    fun whenMatchingSermonIsPaused_isActiveInPlayerIsTrueAndIsPlayingIsFalse() = runTest(testDispatcher) {
        advanceUntilIdle()

        fakePlaybackManager.playbackStateFlow.value = PlaybackState(
            currentSermon = testSermonDetail.toSermon(),
            isPlaying = false
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isActiveInPlayer)
        assertFalse(state.isPlaying)
    }

    @Test
    fun whenDifferentSermonIsPlaying_isActiveInPlayerAndIsPlayingAreFalse() = runTest(testDispatcher) {
        advanceUntilIdle()

        fakePlaybackManager.playbackStateFlow.value = PlaybackState(
            currentSermon = Sermon(id = "sermon-99", title = "Other", speaker = "Other"),
            isPlaying = true
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isActiveInPlayer)
        assertFalse(state.isPlaying)
    }

    @Test
    fun onEventPlay_whenNotActiveInPlayer_callsPlaySermon() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(SermonDetailUiEvent.Play)
        advanceUntilIdle()

        assertEquals("sermon-1", fakePlaybackManager.lastPlayedSermon?.id)
        assertFalse(fakePlaybackManager.togglePlayPauseCalled)
    }

    @Test
    fun onEventPlay_whenActiveInPlayer_callsTogglePlayPause() = runTest(testDispatcher) {
        advanceUntilIdle()

        fakePlaybackManager.playbackStateFlow.value = PlaybackState(
            currentSermon = testSermonDetail.toSermon(),
            isPlaying = false
        )
        advanceUntilIdle()

        viewModel.onEvent(SermonDetailUiEvent.Play)
        advanceUntilIdle()

        assertTrue(fakePlaybackManager.togglePlayPauseCalled)
    }
}
