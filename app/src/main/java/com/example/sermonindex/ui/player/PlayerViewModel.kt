package com.example.sermonindex.ui.player

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.domain.model.SleepTimerState
import com.example.sermonindex.domain.usecase.DeleteDownloadUseCase
import com.example.sermonindex.domain.usecase.DownloadSermonUseCase
import com.example.sermonindex.domain.usecase.GetDownloadProgressUseCase
import com.example.sermonindex.domain.usecase.GetFavoritesUseCase
import com.example.sermonindex.domain.usecase.GetSermonDetailsUseCase
import com.example.sermonindex.domain.usecase.IsDownloadedUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import com.example.sermonindex.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class PlayerUiState(
    val sermon: Sermon? = null,
    val sermonDetail: SermonDetail? = null,
    val isLoadingDetail: Boolean = false,
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val downloadProgress: Int? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isUsingFailoverCdn: Boolean = false,
    val isLocalAudio: Boolean = false,
    val sleepTimerState: SleepTimerState = SleepTimerState.Off,
    val showSleepTimerDialog: Boolean = false,
    val selectedTab: Int = 0,
    val sliderPosition: Float = 0f,
    val isDraggingSlider: Boolean = false,
    val showSpeedMenu: Boolean = false
) {
    val displayPositionMs: Long
        get() = if (isDraggingSlider) {
            (sliderPosition * durationMs).toLong()
        } else {
            currentPositionMs
        }
}

sealed interface PlayerUiEvent {
    data object TogglePlayPause : PlayerUiEvent
    data class SeekTo(val positionMs: Long) : PlayerUiEvent
    data class Rewind(val seconds: Int = 15) : PlayerUiEvent
    data class FastForward(val seconds: Int = 30) : PlayerUiEvent
    data class SetSpeed(val speed: Float) : PlayerUiEvent
    data object ToggleFavorite : PlayerUiEvent
    data object ToggleDownload : PlayerUiEvent
    data class SelectTab(val tabIndex: Int) : PlayerUiEvent
    data class SliderChange(val position: Float) : PlayerUiEvent
    data object SliderChangeFinished : PlayerUiEvent
    data class SetSpeedMenuVisible(val visible: Boolean) : PlayerUiEvent
    data class SetSleepTimerDialogVisible(val visible: Boolean) : PlayerUiEvent
    data class SetSleepTimerMinutes(val minutes: Int) : PlayerUiEvent
    data object SetSleepTimerEndOfSermon : PlayerUiEvent
    data object CancelSleepTimer : PlayerUiEvent
}

/**
 * Modern Android Architecture ViewModel for the Fullscreen Player.
 * Observes [PlaybackManager] and manages UI state without Android View dependencies.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val getSermonDetailsUseCase: GetSermonDetailsUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val isDownloadedUseCase: IsDownloadedUseCase,
    private val getDownloadProgressUseCase: GetDownloadProgressUseCase,
    private val downloadSermonUseCase: DownloadSermonUseCase,
    private val deleteDownloadUseCase: DeleteDownloadUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentFavoriteIds = emptySet<String>()
    private var downloadObserverJob: Job? = null
    private var progressObserverJob: Job? = null

    init {
        observePlaybackState()
        observeFavorites()
    }

    private fun observePlaybackState() {
        viewModelScope.launch {
            playbackManager.playbackState.collect { pbState ->
                val prevSermonId = _uiState.value.sermon?.id
                val newSermon = pbState.currentSermon
                val sermonChanged = newSermon?.id != prevSermonId

                _uiState.update { current ->
                    current.copy(
                        sermon = newSermon,
                        isPlaying = pbState.isPlaying,
                        isBuffering = pbState.isBuffering,
                        currentPositionMs = pbState.currentPositionMs,
                        durationMs = pbState.durationMs,
                        playbackSpeed = pbState.playbackSpeed,
                        isUsingFailoverCdn = pbState.isUsingFailoverCdn,
                        isLocalAudio = pbState.isLocalAudio,
                        sleepTimerState = pbState.sleepTimerState,
                        isFavorite = newSermon?.id?.let { currentFavoriteIds.contains(it) } ?: false
                    )
                }

                if (sermonChanged && newSermon != null) {
                    loadSermonDetails(newSermon.id)
                    observeDownloadsForSermon(newSermon.id)
                }
            }
        }
    }

    private fun observeDownloadsForSermon(sermonId: String) {
        downloadObserverJob?.cancel()
        downloadObserverJob = viewModelScope.launch {
            isDownloadedUseCase(sermonId).collect { downloaded ->
                _uiState.update { it.copy(isDownloaded = downloaded) }
            }
        }
        progressObserverJob?.cancel()
        progressObserverJob = viewModelScope.launch {
            getDownloadProgressUseCase(sermonId).collect { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            getFavoritesUseCase().collect { favorites ->
                currentFavoriteIds = favorites.map { it.id }.toSet()
                val currentId = _uiState.value.sermon?.id
                _uiState.update { current ->
                    current.copy(isFavorite = currentId?.let { currentFavoriteIds.contains(it) } ?: false)
                }
            }
        }
    }

    private fun loadSermonDetails(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true) }
            getSermonDetailsUseCase(id).fold(
                onSuccess = { detail ->
                    _uiState.update { it.copy(sermonDetail = detail, isLoadingDetail = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingDetail = false) }
                }
            )
        }
    }

    fun onEvent(event: PlayerUiEvent) {
        when (event) {
            is PlayerUiEvent.TogglePlayPause -> playbackManager.togglePlayPause()
            is PlayerUiEvent.SeekTo -> playbackManager.seekTo(event.positionMs)
            is PlayerUiEvent.Rewind -> playbackManager.rewind(event.seconds)
            is PlayerUiEvent.FastForward -> playbackManager.fastForward(event.seconds)
            is PlayerUiEvent.SetSpeed -> {
                playbackManager.setSpeed(event.speed)
                _uiState.update { it.copy(showSpeedMenu = false) }
            }
            is PlayerUiEvent.ToggleFavorite -> {
                val s = _uiState.value.sermon ?: return
                viewModelScope.launch {
                    toggleFavoriteUseCase(s)
                }
            }
            is PlayerUiEvent.SelectTab -> {
                _uiState.update { it.copy(selectedTab = event.tabIndex) }
            }
            is PlayerUiEvent.SliderChange -> {
                _uiState.update { it.copy(sliderPosition = event.position, isDraggingSlider = true) }
            }
            is PlayerUiEvent.SliderChangeFinished -> {
                val duration = _uiState.value.durationMs
                val targetMs = (_uiState.value.sliderPosition * duration).toLong()
                playbackManager.seekTo(targetMs)
                _uiState.update { it.copy(isDraggingSlider = false) }
            }
            is PlayerUiEvent.SetSpeedMenuVisible -> {
                _uiState.update { it.copy(showSpeedMenu = event.visible) }
            }
            is PlayerUiEvent.SetSleepTimerDialogVisible -> {
                _uiState.update { it.copy(showSleepTimerDialog = event.visible) }
            }
            is PlayerUiEvent.SetSleepTimerMinutes -> {
                playbackManager.setSleepTimerMinutes(event.minutes)
                _uiState.update { it.copy(showSleepTimerDialog = false) }
            }
            is PlayerUiEvent.SetSleepTimerEndOfSermon -> {
                playbackManager.setSleepTimerEndOfSermon()
                _uiState.update { it.copy(showSleepTimerDialog = false) }
            }
            is PlayerUiEvent.CancelSleepTimer -> {
                playbackManager.cancelSleepTimer()
                _uiState.update { it.copy(showSleepTimerDialog = false) }
            }
            is PlayerUiEvent.ToggleDownload -> {
                val s = _uiState.value.sermon ?: return
                viewModelScope.launch {
                    if (_uiState.value.isDownloaded) {
                        deleteDownloadUseCase(s.id)
                    } else {
                        downloadSermonUseCase(s)
                    }
                }
            }
        }
    }
}
