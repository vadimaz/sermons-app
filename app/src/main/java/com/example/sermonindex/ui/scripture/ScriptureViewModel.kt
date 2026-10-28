package com.example.sermonindex.ui.scripture

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.domain.model.BibleBook
import com.example.sermonindex.domain.usecase.GetScriptureBooksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ScriptureUiState(
    val books: List<BibleBook> = emptyList(),
    val testamentTabIndex: Int = 0, // 0: All, 1: OT, 2: NT
    val isLoadingBooks: Boolean = true,
    val errorMessage: String? = null
) {
    val filteredBooks: List<BibleBook>
        get() = when (testamentTabIndex) {
            1 -> books.filter { it.testament.equals("OT", ignoreCase = true) || it.testament.contains("Old", ignoreCase = true) }
            2 -> books.filter { it.testament.equals("NT", ignoreCase = true) || it.testament.contains("New", ignoreCase = true) }
            else -> books
        }
}

sealed interface ScriptureUiEvent {
    data object RetryBooks : ScriptureUiEvent
    data class SelectTestamentTab(val index: Int) : ScriptureUiEvent
    data class SelectBook(val book: BibleBook) : ScriptureUiEvent
}

@HiltViewModel
class ScriptureViewModel @Inject constructor(
    private val getScriptureBooksUseCase: GetScriptureBooksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScriptureUiState())
    val uiState: StateFlow<ScriptureUiState> = _uiState.asStateFlow()

    init {
        loadBooks()
    }

    fun onEvent(event: ScriptureUiEvent) {
        when (event) {
            is ScriptureUiEvent.RetryBooks -> loadBooks()
            is ScriptureUiEvent.SelectTestamentTab -> {
                _uiState.update { it.copy(testamentTabIndex = event.index) }
            }
            is ScriptureUiEvent.SelectBook -> {
                // Handled by UI navigation callback
            }
        }
    }

    private fun loadBooks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBooks = true, errorMessage = null) }
            getScriptureBooksUseCase()
                .onSuccess { bookList ->
                    _uiState.update {
                        it.copy(
                            books = bookList,
                            isLoadingBooks = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingBooks = false,
                            errorMessage = error.localizedMessage ?: "Failed to load scripture books."
                        )
                    }
                }
        }
    }
}
