package com.example.sermonindex.ui.player

import app.cash.turbine.test
import com.example.sermonindex.playback.PlaybackState
import com.example.sermonindex.domain.model.SleepTimerState
import com.example.sermonindex.domain.usecase.DeleteDownloadUseCase
import com.example.sermonindex.domain.usecase.DownloadSermonUseCase
import com.example.sermonindex.domain.usecase.GetDownloadProgressUseCase
import com.example.sermonindex.domain.usecase.GetFavoritesUseCase
import com.example.sermonindex.domain.usecase.GetSermonDetailsUseCase
import com.example.sermonindex.domain.usecase.GetTranscriptCuesUseCase
import com.example.sermonindex.domain.usecase.IsDownloadedUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import com.example.sermonindex.testing.FakePlaybackManager
import com.example.sermonindex.testing.FakeSermonRepository
import com.example.sermonindex.testing.MainDispatcherRule
import com.example.sermonindex.testing.createTestDownload
import com.example.sermonindex.testing.createTestSermon
import com.example.sermonindex.testing.createTestSermonDetail
import com.example.sermonindex.testing.createTestTranscriptCue
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
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var fakeRepository: FakeSermonRepository
    private lateinit var fakePlaybackManager: FakePlaybackManager
    private lateinit var viewModel: PlayerViewModel

    private val testSermon = createTestSermon(id = "player-sermon-1", title = "The Deeper Life")

    @Before
    fun setUp() {
        fakeRepository = FakeSermonRepository()
        fakePlaybackManager = FakePlaybackManager()

        fakeRepository.sermonDetailsProvider = { id ->
            Result.success(createTestSermonDetail(id = id, title = "The Deeper Life"))
        }

        viewModel = PlayerViewModel(
            playbackManager = fakePlaybackManager,
            getSermonDetailsUseCase = GetSermonDetailsUseCase(fakeRepository),
            getTranscriptCuesUseCase = GetTranscriptCuesUseCase(fakeRepository),
            getFavoritesUseCase = GetFavoritesUseCase(fakeRepository),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(fakeRepository),
            isDownloadedUseCase = IsDownloadedUseCase(fakeRepository),
            getDownloadProgressUseCase = GetDownloadProgressUseCase(fakeRepository),
            downloadSermonUseCase = DownloadSermonUseCase(fakeRepository),
            deleteDownloadUseCase = DeleteDownloadUseCase(fakeRepository)
        )
    }

    @Test
    fun playbackStateChange_syncsWithUiState() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        fakePlaybackManager.setPlaybackState(
            PlaybackState(
                currentSermon = testSermon,
                isPlaying = true,
                currentPositionMs = 15000L,
                durationMs = 60000L,
                playbackSpeed = 1.25f
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("player-sermon-1", state.sermon?.id)
        assertTrue(state.isPlaying)
        assertEquals(15000L, state.currentPositionMs)
        assertEquals(60000L, state.durationMs)
        assertEquals(1.25f, state.playbackSpeed)
    }

    @Test
    fun togglePlayPause_delegatesToPlaybackManager() = runTest {
        fakePlaybackManager.setPlaybackState(PlaybackState(isPlaying = false))
        viewModel.onEvent(PlayerUiEvent.TogglePlayPause)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakePlaybackManager.playbackState.value.isPlaying)
    }

    @Test
    fun setSpeed_updatesPlaybackManagerAndHidesSpeedMenu() = runTest {
        viewModel.onEvent(PlayerUiEvent.SetSpeedMenuVisible(true))
        assertTrue(viewModel.uiState.value.showSpeedMenu)

        viewModel.onEvent(PlayerUiEvent.SetSpeed(1.5f))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1.5f, fakePlaybackManager.playbackState.value.playbackSpeed)
        assertFalse(viewModel.uiState.value.showSpeedMenu)
    }

    @Test
    fun sliderDraggingAndFinish_seeksAccurately() = runTest {
        fakePlaybackManager.setPlaybackState(
            PlaybackState(
                currentSermon = testSermon,
                currentPositionMs = 10000L,
                durationMs = 100000L
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(PlayerUiEvent.SliderChange(0.5f))
        val draggingState = viewModel.uiState.value
        assertTrue(draggingState.isDraggingSlider)
        assertEquals(50000L, draggingState.displayPositionMs)

        viewModel.onEvent(PlayerUiEvent.SliderChangeFinished)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDraggingSlider)
        assertEquals(50000L, fakePlaybackManager.lastSeekPosition)
    }

    @Test
    fun sleepTimerEvents_configureStateAndDismissDialog() = runTest {
        viewModel.onEvent(PlayerUiEvent.SetSleepTimerDialogVisible(true))
        assertTrue(viewModel.uiState.value.showSleepTimerDialog)

        viewModel.onEvent(PlayerUiEvent.SetSleepTimerMinutes(30))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showSleepTimerDialog)
        assertTrue(fakePlaybackManager.playbackState.value.sleepTimerState is SleepTimerState.Active)

        viewModel.onEvent(PlayerUiEvent.CancelSleepTimer)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(SleepTimerState.Off, fakePlaybackManager.playbackState.value.sleepTimerState)
    }

    @Test
    fun activeCueIndex_calculatesCorrectCueBasedOnPosition() {
        val cues = listOf(
            createTestTranscriptCue(index = 0, startMs = 0L, endMs = 5000L, text = "First cue"),
            createTestTranscriptCue(index = 1, startMs = 5000L, endMs = 10000L, text = "Second cue"),
            createTestTranscriptCue(index = 2, startMs = 10000L, endMs = 15000L, text = "Third cue")
        )

        val stateAtStart = PlayerUiState(currentPositionMs = 2000L, transcriptCues = cues)
        assertEquals(0, stateAtStart.activeCueIndex)

        val stateAtSecond = PlayerUiState(currentPositionMs = 7000L, transcriptCues = cues)
        assertEquals(1, stateAtSecond.activeCueIndex)

        val stateAtThird = PlayerUiState(currentPositionMs = 12000L, transcriptCues = cues)
        assertEquals(2, stateAtThird.activeCueIndex)
    }

    @Test
    fun toggleDownload_downloadsWhenNotDownloaded_deletesWhenDownloaded() = runTest {
        fakePlaybackManager.setPlaybackState(PlaybackState(currentSermon = testSermon))
        testDispatcher.scheduler.advanceUntilIdle()

        // Initially not downloaded -> toggleDownload calls downloadSermon
        viewModel.onEvent(PlayerUiEvent.ToggleDownload)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("player-sermon-1", fakeRepository.lastDownloadedSermon?.id)

        // Mark as downloaded
        fakeRepository.downloadsFlow.value = listOf(createTestDownload(id = "player-sermon-1"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Now downloaded -> toggleDownload calls deleteDownload
        viewModel.onEvent(PlayerUiEvent.ToggleDownload)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("player-sermon-1", fakeRepository.lastDeletedDownloadId)
    }
}
