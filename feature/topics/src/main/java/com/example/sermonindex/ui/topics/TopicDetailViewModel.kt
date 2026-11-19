package com.example.sermonindex.ui.topics

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.TopicDetail
import com.example.sermonindex.domain.usecase.GetFavoritesUseCase
import com.example.sermonindex.domain.usecase.GetTopicDetailsUseCase
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
data class TopicDetailUiState(
    val topicDetail: TopicDetail? = null,
    val favoriteIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface TopicDetailUiEvent {
    data object Retry : TopicDetailUiEvent
    data class PlaySermon(val sermon: Sermon) : TopicDetailUiEvent
    data class ReadSermon(val sermon: Sermon) : TopicDetailUiEvent
    data class ToggleFavorite(val sermon: Sermon) : TopicDetailUiEvent
    data class ClickSermon(val sermon: Sermon) : TopicDetailUiEvent
}

@HiltViewModel(assistedFactory = TopicDetailViewModel.Factory::class)
class TopicDetailViewModel @AssistedInject constructor(
    @Assisted private val slug: String,
    private val getTopicDetailsUseCase: GetTopicDetailsUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(slug: String): TopicDetailViewModel
    }

    private val _uiState = MutableStateFlow(TopicDetailUiState())
    val uiState: StateFlow<TopicDetailUiState> = _uiState.asStateFlow()

    init {
        observeFavorites()
        loadTopicDetails()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            getFavoritesUseCase().collect { favs ->
                val favIds = favs.map { it.id }.toSet()
                _uiState.update { it.copy(favoriteIds = favIds) }
            }
        }
    }

    fun onEvent(event: TopicDetailUiEvent) {
        when (event) {
            is TopicDetailUiEvent.Retry -> loadTopicDetails()
            is TopicDetailUiEvent.PlaySermon -> playbackManager.playSermon(event.sermon)
            is TopicDetailUiEvent.ReadSermon -> Unit
            is TopicDetailUiEvent.ToggleFavorite -> {
                viewModelScope.launch {
                    toggleFavoriteUseCase(event.sermon)
                }
            }
            is TopicDetailUiEvent.ClickSermon -> Unit
        }
    }

    private fun loadTopicDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getTopicDetailsUseCase(slug)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            topicDetail = detail,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load topic details."
                        )
                    }
                }
        }
    }
}
