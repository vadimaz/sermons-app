package com.example.sermonindex.ui.scripture

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.domain.model.BibleBook
import com.example.sermonindex.domain.model.ChapterDetails
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.usecase.GetFavoritesUseCase
import com.example.sermonindex.domain.usecase.GetScriptureBooksUseCase
import com.example.sermonindex.domain.usecase.GetScriptureChapterUseCase
import com.example.sermonindex.domain.usecase.GetScriptureVerseUseCase
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
data class ScriptureUiState(
    val books: List<BibleBook> = emptyList(),
    val selectedBook: BibleBook? = null,
    val selectedChapter: Int? = null,
    val selectedVerse: Int? = null,
    val chapterData: ChapterDetails? = null,
    val verseSermons: List<Sermon> = emptyList(),
    val testamentTabIndex: Int = 0, // 0: All, 1: OT, 2: NT
    val favoriteIds: Set<String> = emptySet(),
    val isLoadingBooks: Boolean = true,
    val isLoadingChapter: Boolean = false,
    val isLoadingVerseSermons: Boolean = false,
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
    data class SelectChapter(val chapter: Int) : ScriptureUiEvent
    data class SelectVerse(val verse: Int) : ScriptureUiEvent
    data object ClearVerse : ScriptureUiEvent
    data object ClearChapter : ScriptureUiEvent
    data object ClearBook : ScriptureUiEvent
    data class PlaySermon(val sermon: Sermon) : ScriptureUiEvent
    data class ToggleFavorite(val sermon: Sermon) : ScriptureUiEvent
    data class ClickSermon(val sermon: Sermon) : ScriptureUiEvent
}

@HiltViewModel
class ScriptureViewModel @Inject constructor(
    private val getScriptureBooksUseCase: GetScriptureBooksUseCase,
    private val getScriptureChapterUseCase: GetScriptureChapterUseCase,
    private val getScriptureVerseUseCase: GetScriptureVerseUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScriptureUiState())
    val uiState: StateFlow<ScriptureUiState> = _uiState.asStateFlow()

    init {
        observeFavorites()
        loadBooks()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            getFavoritesUseCase().collect { favs ->
                val favIds = favs.map { it.id }.toSet()
                _uiState.update { it.copy(favoriteIds = favIds) }
            }
        }
    }

    fun onEvent(event: ScriptureUiEvent) {
        when (event) {
            is ScriptureUiEvent.RetryBooks -> loadBooks()
            is ScriptureUiEvent.SelectTestamentTab -> {
                _uiState.update { it.copy(testamentTabIndex = event.index) }
            }
            is ScriptureUiEvent.SelectBook -> {
                _uiState.update {
                    it.copy(
                        selectedBook = event.book,
                        selectedChapter = null,
                        selectedVerse = null,
                        chapterData = null,
                        verseSermons = emptyList()
                    )
                }
            }
            is ScriptureUiEvent.SelectChapter -> {
                _uiState.update {
                    it.copy(
                        selectedChapter = event.chapter,
                        selectedVerse = null,
                        verseSermons = emptyList()
                    )
                }
                loadChapterData(event.chapter)
            }
            is ScriptureUiEvent.SelectVerse -> {
                _uiState.update { it.copy(selectedVerse = event.verse) }
                loadVerseSermons(event.verse)
            }
            is ScriptureUiEvent.ClearVerse -> {
                _uiState.update { it.copy(selectedVerse = null, verseSermons = emptyList()) }
            }
            is ScriptureUiEvent.ClearChapter -> {
                _uiState.update {
                    it.copy(
                        selectedChapter = null,
                        selectedVerse = null,
                        chapterData = null,
                        verseSermons = emptyList()
                    )
                }
            }
            is ScriptureUiEvent.ClearBook -> {
                _uiState.update {
                    it.copy(
                        selectedBook = null,
                        selectedChapter = null,
                        selectedVerse = null,
                        chapterData = null,
                        verseSermons = emptyList()
                    )
                }
            }
            is ScriptureUiEvent.PlaySermon -> playbackManager.playSermon(event.sermon)
            is ScriptureUiEvent.ToggleFavorite -> {
                viewModelScope.launch {
                    toggleFavoriteUseCase(event.sermon)
                }
            }
            is ScriptureUiEvent.ClickSermon -> Unit
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

    private fun loadChapterData(chapter: Int) {
        val book = _uiState.value.selectedBook ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChapter = true) }
            getScriptureChapterUseCase(book.id, chapter)
                .onSuccess { details ->
                    _uiState.update {
                        it.copy(
                            chapterData = details,
                            isLoadingChapter = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            chapterData = ChapterDetails(bookId = book.id, bookName = book.name, chapter = chapter, verseCount = 0),
                            isLoadingChapter = false
                        )
                    }
                }
        }
    }

    private fun loadVerseSermons(verse: Int) {
        val book = _uiState.value.selectedBook ?: return
        val chapter = _uiState.value.selectedChapter ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingVerseSermons = true) }
            getScriptureVerseUseCase(book.id, chapter, verse)
                .onSuccess { sermons ->
                    _uiState.update {
                        it.copy(
                            verseSermons = sermons,
                            isLoadingVerseSermons = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            verseSermons = emptyList(),
                            isLoadingVerseSermons = false
                        )
                    }
                }
        }
    }
}
