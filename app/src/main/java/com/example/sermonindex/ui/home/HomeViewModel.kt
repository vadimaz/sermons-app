package com.example.sermonindex.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.domain.model.ListeningHistory
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.usecase.GetFavoritesUseCase
import com.example.sermonindex.domain.usecase.GetHistoryUseCase
import com.example.sermonindex.domain.usecase.GetPopularSermonsUseCase
import com.example.sermonindex.domain.usecase.GetSermonDetailsUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import com.example.sermonindex.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val popularSermons: List<Sermon> = emptyList(),
    val history: List<ListeningHistory> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

sealed interface HomeUiEvent {
    data object Refresh : HomeUiEvent
    data class PlaySermon(val sermon: Sermon) : HomeUiEvent
    data class ResumeHistory(val history: ListeningHistory) : HomeUiEvent
    data class ToggleFavorite(val sermon: Sermon) : HomeUiEvent
    data class SelectSermon(val sermon: Sermon) : HomeUiEvent
    data class SelectTopic(val slug: String) : HomeUiEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getPopularSermonsUseCase: GetPopularSermonsUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val getHistoryUseCase: GetHistoryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getSermonDetailsUseCase: GetSermonDetailsUseCase,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeFavoritesAndHistory()
        loadPopularSermons()
    }

    private fun observeFavoritesAndHistory() {
        viewModelScope.launch {
            getFavoritesUseCase().collect { favorites ->
                val favIds = favorites.map { it.id }.toSet()
                _uiState.update { it.copy(favoriteIds = favIds) }
            }
        }
        viewModelScope.launch {
            getHistoryUseCase().collect { historyList ->
                _uiState.update { it.copy(history = historyList) }
            }
        }
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.Refresh -> loadPopularSermons()
            is HomeUiEvent.PlaySermon -> playbackManager.playSermon(event.sermon)
            is HomeUiEvent.ResumeHistory -> resumeHistoryItem(event.history)
            is HomeUiEvent.ToggleFavorite -> {
                viewModelScope.launch {
                    toggleFavoriteUseCase(event.sermon)
                }
            }
            is HomeUiEvent.SelectSermon -> Unit
            is HomeUiEvent.SelectTopic -> Unit
        }
    }

    private fun loadPopularSermons() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getPopularSermonsUseCase()
                .onSuccess { sermons ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            popularSermons = sermons,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load popular sermons."
                        )
                    }
                }
        }
    }

    private fun resumeHistoryItem(history: ListeningHistory) {
        val matching = _uiState.value.popularSermons.find { it.id == history.sermonId }
        if (matching != null) {
            playbackManager.playSermon(matching, startPositionMs = history.lastPositionMs)
        } else {
            viewModelScope.launch {
                getSermonDetailsUseCase(history.sermonId).onSuccess { detail ->
                    playbackManager.playSermon(detail.toSermon(), startPositionMs = history.lastPositionMs)
                }
            }
        }
    }
}
