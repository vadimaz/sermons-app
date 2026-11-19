package com.example.sermonindex.ui.scripture

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.domain.model.ChapterDetails
import com.example.sermonindex.domain.usecase.GetScriptureChapterUseCase
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
data class ScriptureChapterUiState(
    val bookId: String = "",
    val bookName: String = "",
    val chapter: Int = 1,
    val chapterData: ChapterDetails? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface ScriptureChapterUiEvent {
    data object Retry : ScriptureChapterUiEvent
    data class SelectVerse(val verse: Int) : ScriptureChapterUiEvent
}

data class ScriptureChapterArgs(
    val bookId: String,
    val bookName: String,
    val chapter: Int
)

@HiltViewModel(assistedFactory = ScriptureChapterViewModel.Factory::class)
class ScriptureChapterViewModel @AssistedInject constructor(
    @Assisted private val args: ScriptureChapterArgs,
    private val getScriptureChapterUseCase: GetScriptureChapterUseCase
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(args: ScriptureChapterArgs): ScriptureChapterViewModel
    }

    private val _uiState = MutableStateFlow(
        ScriptureChapterUiState(
            bookId = args.bookId,
            bookName = args.bookName,
            chapter = args.chapter
        )
    )
    val uiState: StateFlow<ScriptureChapterUiState> = _uiState.asStateFlow()

    init {
        loadChapterData()
    }

    fun onEvent(event: ScriptureChapterUiEvent) {
        when (event) {
            ScriptureChapterUiEvent.Retry -> loadChapterData()
            is ScriptureChapterUiEvent.SelectVerse -> {
                // Handled by UI navigation callback
            }
        }
    }

    private fun loadChapterData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getScriptureChapterUseCase(args.bookId, args.chapter)
                .onSuccess { details ->
                    _uiState.update {
                        it.copy(
                            chapterData = details,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            chapterData = null,
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load chapter details."
                        )
                    }
                }
        }
    }
}
