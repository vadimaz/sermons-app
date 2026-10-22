package com.example.sermonindex.ui.sermon

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.domain.usecase.DeleteDownloadUseCase
import com.example.sermonindex.domain.usecase.DownloadSermonUseCase
import com.example.sermonindex.domain.usecase.GetDownloadProgressUseCase
import com.example.sermonindex.domain.usecase.GetSermonDetailsUseCase
import com.example.sermonindex.domain.usecase.IsDownloadedUseCase
import com.example.sermonindex.domain.usecase.IsFavoriteUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import com.example.sermonindex.playback.PlaybackManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class SermonDetailUiState(
    val detail: SermonDetail? = null,
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val downloadProgress: Int? = null,
    val isPlaying: Boolean = false,
    val isActiveInPlayer: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedTab: Int = 0
)

sealed interface SermonDetailUiEvent {
    data object Retry : SermonDetailUiEvent
    data object Play : SermonDetailUiEvent
    data object ToggleFavorite : SermonDetailUiEvent
    data object ToggleDownload : SermonDetailUiEvent
    data class ClickSpeaker(val slug: String) : SermonDetailUiEvent
    data class SelectTab(val tabIndex: Int) : SermonDetailUiEvent
}

@HiltViewModel(assistedFactory = SermonDetailViewModel.Factory::class)
class SermonDetailViewModel @AssistedInject constructor(
    @Assisted private val sermonId: String,
    private val getSermonDetailsUseCase: GetSermonDetailsUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val isDownloadedUseCase: IsDownloadedUseCase,
    private val getDownloadProgressUseCase: GetDownloadProgressUseCase,
    private val downloadSermonUseCase: DownloadSermonUseCase,
    private val deleteDownloadUseCase: DeleteDownloadUseCase,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(sermonId: String): SermonDetailViewModel
    }

    private val _uiState = MutableStateFlow(SermonDetailUiState())
    val uiState: StateFlow<SermonDetailUiState> = _uiState.asStateFlow()

    init {
        observeFavorite()
        observeDownload()
        observePlayback()
        loadDetails()
    }

    private fun observePlayback() {
        viewModelScope.launch {
            playbackManager.playbackState.collect { pbState ->
                val isActive = pbState.currentSermon?.id == sermonId
                val playing = isActive && pbState.isPlaying
                _uiState.update { it.copy(isPlaying = playing, isActiveInPlayer = isActive) }
            }
        }
    }

    private fun observeFavorite() {
        viewModelScope.launch {
            isFavoriteUseCase(sermonId).collect { isFav ->
                _uiState.update { it.copy(isFavorite = isFav) }
            }
        }
    }

    private fun observeDownload() {
        viewModelScope.launch {
            isDownloadedUseCase(sermonId).collect { isDown ->
                _uiState.update { it.copy(isDownloaded = isDown) }
            }
        }
        viewModelScope.launch {
            getDownloadProgressUseCase(sermonId).collect { prog ->
                _uiState.update { it.copy(downloadProgress = prog) }
            }
        }
    }

    fun onEvent(event: SermonDetailUiEvent) {
        when (event) {
            is SermonDetailUiEvent.Retry -> loadDetails()
            is SermonDetailUiEvent.Play -> {
                _uiState.value.detail?.let { detail ->
                    val currentState = playbackManager.playbackState.value
                    if (currentState.currentSermon?.id == detail.id) {
                        playbackManager.togglePlayPause()
                    } else {
                        playbackManager.playSermon(detail.toSermon())
                    }
                }
            }
            is SermonDetailUiEvent.ToggleFavorite -> {
                _uiState.value.detail?.let { detail ->
                    viewModelScope.launch {
                        toggleFavoriteUseCase(detail.toSermon())
                    }
                }
            }
            is SermonDetailUiEvent.ToggleDownload -> {
                val detail = _uiState.value.detail ?: return
                viewModelScope.launch {
                    if (_uiState.value.isDownloaded) {
                        deleteDownloadUseCase(detail.id)
                    } else {
                        downloadSermonUseCase(detail.toSermon())
                    }
                }
            }
            is SermonDetailUiEvent.SelectTab -> {
                _uiState.update { it.copy(selectedTab = event.tabIndex) }
            }
            is SermonDetailUiEvent.ClickSpeaker -> Unit
        }
    }

    private fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getSermonDetailsUseCase(sermonId)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            detail = detail,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load sermon details."
                        )
                    }
                }
        }
    }
}
