package com.example.sermonindex.ui.speakers

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.usecase.GetFavoritesUseCase
import com.example.sermonindex.domain.usecase.GetSpeakerDetailsUseCase
import com.example.sermonindex.domain.usecase.GetSpeakerSermonsPageUseCase
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
data class SpeakerDetailUiState(
    val speaker: Speaker? = null,
    val sermons: List<Sermon> = emptyList(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoadingSpeaker: Boolean = true,
    val isLoadingMore: Boolean = false,
    val favoriteIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

sealed interface SpeakerDetailUiEvent {
    data object Retry : SpeakerDetailUiEvent
    data object LoadMore : SpeakerDetailUiEvent
    data class PlaySermon(val sermon: Sermon) : SpeakerDetailUiEvent
    data class ReadSermon(val sermon: Sermon) : SpeakerDetailUiEvent
    data class ToggleFavorite(val sermon: Sermon) : SpeakerDetailUiEvent
    data class ClickSermon(val sermon: Sermon) : SpeakerDetailUiEvent
    data class ClickTopic(val slug: String) : SpeakerDetailUiEvent
}

@HiltViewModel(assistedFactory = SpeakerDetailViewModel.Factory::class)
class SpeakerDetailViewModel @AssistedInject constructor(
    @Assisted private val slug: String,
    private val getSpeakerDetailsUseCase: GetSpeakerDetailsUseCase,
    private val getSpeakerSermonsPageUseCase: GetSpeakerSermonsPageUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(slug: String): SpeakerDetailViewModel
    }

    private val _uiState = MutableStateFlow(SpeakerDetailUiState())
    val uiState: StateFlow<SpeakerDetailUiState> = _uiState.asStateFlow()

    init {
        observeFavorites()
        loadSpeakerAndPage1()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            getFavoritesUseCase().collect { favs ->
                val favIds = favs.map { it.id }.toSet()
                _uiState.update { it.copy(favoriteIds = favIds) }
            }
        }
    }

    fun onEvent(event: SpeakerDetailUiEvent) {
        when (event) {
            is SpeakerDetailUiEvent.Retry -> loadSpeakerAndPage1()
            is SpeakerDetailUiEvent.LoadMore -> loadNextPage()
            is SpeakerDetailUiEvent.PlaySermon -> playbackManager.playSermon(event.sermon)
            is SpeakerDetailUiEvent.ReadSermon -> Unit
            is SpeakerDetailUiEvent.ToggleFavorite -> {
                viewModelScope.launch {
                    toggleFavoriteUseCase(event.sermon)
                }
            }
            is SpeakerDetailUiEvent.ClickSermon -> Unit
            is SpeakerDetailUiEvent.ClickTopic -> Unit
        }
    }

    private fun loadSpeakerAndPage1() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSpeaker = true, errorMessage = null) }
            getSpeakerDetailsUseCase(slug)
                .onSuccess { sp ->
                    _uiState.update {
                        it.copy(
                            speaker = sp,
                            totalPages = sp.totalPages.coerceAtLeast(1)
                        )
                    }
                    getSpeakerSermonsPageUseCase(slug, 1)
                        .onSuccess { paged ->
                            _uiState.update {
                                it.copy(
                                    sermons = paged.sermons,
                                    totalPages = paged.totalPages,
                                    currentPage = 1,
                                    isLoadingSpeaker = false,
                                    errorMessage = null
                                )
                            }
                        }
                        .onFailure {
                            _uiState.update { it.copy(isLoadingSpeaker = false) }
                        }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingSpeaker = false,
                            errorMessage = error.localizedMessage ?: "Failed to load preacher profile."
                        )
                    }
                }
        }
    }

    private fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoadingMore || state.currentPage >= state.totalPages) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextPage = state.currentPage + 1
            getSpeakerSermonsPageUseCase(slug, nextPage)
                .onSuccess { paged ->
                    _uiState.update {
                        it.copy(
                            sermons = it.sermons + paged.sermons,
                            currentPage = nextPage,
                            totalPages = paged.totalPages,
                            isLoadingMore = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }
}
