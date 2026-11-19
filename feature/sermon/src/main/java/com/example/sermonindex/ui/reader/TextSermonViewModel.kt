package com.example.sermonindex.ui.reader

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.domain.usecase.GetSermonDetailsUseCase
import com.example.sermonindex.domain.usecase.IsFavoriteUseCase
import com.example.sermonindex.domain.usecase.RecordSermonViewUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
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
data class TextSermonUiState(
    val detail: SermonDetail? = null,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val paragraphs: List<String> = emptyList(),
    val highlightQuery: String? = null
)

sealed interface TextSermonUiEvent {
    data object Retry : TextSermonUiEvent
    data object ToggleFavorite : TextSermonUiEvent
    data class ClickSpeaker(val slug: String) : TextSermonUiEvent
}

@HiltViewModel(assistedFactory = TextSermonViewModel.Factory::class)
class TextSermonViewModel @AssistedInject constructor(
    @Assisted("sermonId") private val sermonId: String,
    @Assisted("highlightQuery") private val initialHighlightQuery: String?,
    private val getSermonDetailsUseCase: GetSermonDetailsUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val recordSermonViewUseCase: RecordSermonViewUseCase
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("sermonId") sermonId: String,
            @Assisted("highlightQuery") initialHighlightQuery: String? = null
        ): TextSermonViewModel
    }

    private val _uiState = MutableStateFlow(
        TextSermonUiState(highlightQuery = initialHighlightQuery?.trim()?.ifBlank { null })
    )
    val uiState: StateFlow<TextSermonUiState> = _uiState.asStateFlow()

    init {
        observeFavorite()
        loadDetails()
    }

    private fun observeFavorite() {
        viewModelScope.launch {
            isFavoriteUseCase(sermonId).collect { isFav ->
                _uiState.update { it.copy(isFavorite = isFav) }
            }
        }
    }

    fun onEvent(event: TextSermonUiEvent) {
        when (event) {
            is TextSermonUiEvent.Retry -> loadDetails()
            is TextSermonUiEvent.ToggleFavorite -> {
                _uiState.value.detail?.let { detail ->
                    viewModelScope.launch {
                        toggleFavoriteUseCase(detail.toSermon())
                    }
                }
            }
            is TextSermonUiEvent.ClickSpeaker -> Unit
        }
    }

    private fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getSermonDetailsUseCase(sermonId)
                .onSuccess { detail ->
                    recordSermonViewUseCase(
                        sermonId = detail.id,
                        title = detail.title,
                        speaker = detail.speaker,
                        speakerImage = detail.speakerImage
                    )
                    val cleanText = when {
                        detail.transcript.isNotBlank() -> detail.transcript
                        detail.transcriptHtml.isNotBlank() -> detail.transcriptHtml.replace(Regex("<[^>]*>"), "")
                        detail.description.isNotBlank() -> detail.description
                        else -> ""
                    }
                    val paragraphs = cleanText.split(Regex("(\r?\n){2,}")).map { it.trim() }.filter { it.isNotBlank() }

                    _uiState.update {
                        it.copy(
                            detail = detail,
                            paragraphs = paragraphs,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load sermon details"
                        )
                    }
                }
        }
    }
}
