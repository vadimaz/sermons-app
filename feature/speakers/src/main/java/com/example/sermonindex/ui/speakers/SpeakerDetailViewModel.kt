package com.example.sermonindex.ui.speakers

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.usecase.GetFavoritesUseCase
import com.example.sermonindex.domain.usecase.GetSpeakerDetailsUseCase
import com.example.sermonindex.domain.usecase.GetSpeakerSermonsPageUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import com.example.sermonindex.playback.PlaybackManager
import com.example.sermonindex.ui.speakers.paging.SpeakerSermonsPagingSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class SpeakerDetailUiState(
    val speaker: Speaker? = null,
    val isLoadingSpeaker: Boolean = true,
    val favoriteIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

sealed interface SpeakerDetailUiEvent {
    data object Retry : SpeakerDetailUiEvent
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

    val sermonsPagingFlow: Flow<PagingData<Sermon>> = Pager(
        config = PagingConfig(
            pageSize = 50,
            enablePlaceholders = false,
            prefetchDistance = 5
        ),
        pagingSourceFactory = { SpeakerSermonsPagingSource(getSpeakerSermonsPageUseCase, slug) }
    ).flow.cachedIn(viewModelScope)

    init {
        observeFavorites()
        loadSpeaker()
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
            is SpeakerDetailUiEvent.Retry -> loadSpeaker()
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

    private fun loadSpeaker() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSpeaker = true, errorMessage = null) }
            getSpeakerDetailsUseCase(slug)
                .onSuccess { sp ->
                    _uiState.update {
                        it.copy(
                            speaker = sp,
                            isLoadingSpeaker = false,
                            errorMessage = null
                        )
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
}
