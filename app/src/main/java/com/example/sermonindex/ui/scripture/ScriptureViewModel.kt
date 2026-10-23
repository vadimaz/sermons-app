package com.example.sermonindex.ui.scripture

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.domain.model.BibleBook
import com.example.sermonindex.domain.model.BibleTranslation
import com.example.sermonindex.domain.model.BibleTranslationDefaults
import com.example.sermonindex.domain.model.ChapterDetails
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.usecase.GetBibleTranslationsUseCase
import com.example.sermonindex.domain.usecase.GetBibleVerseTextUseCase
import com.example.sermonindex.domain.usecase.GetFavoritesUseCase
import com.example.sermonindex.domain.usecase.GetFirstTranslationUseCase
import com.example.sermonindex.domain.usecase.GetSecondTranslationUseCase
import com.example.sermonindex.domain.usecase.GetScriptureBooksUseCase
import com.example.sermonindex.domain.usecase.GetScriptureChapterUseCase
import com.example.sermonindex.domain.usecase.GetScriptureVerseUseCase
import com.example.sermonindex.domain.usecase.SetFirstTranslationUseCase
import com.example.sermonindex.domain.usecase.SetSecondTranslationUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import com.example.sermonindex.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TranslationPickerTarget {
    FIRST,
    SECOND
}

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
    val errorMessage: String? = null,

    // Dual Translation & Verse Quote State
    val availableTranslations: List<BibleTranslation> = emptyList(),
    val firstTranslationId: String = BibleTranslationDefaults.DEFAULT_FIRST,
    val secondTranslationId: String = BibleTranslationDefaults.getDefaultSecondaryTranslation(),
    val firstVerseText: String? = null,
    val secondVerseText: String? = null,
    val isLoadingFirstVerseText: Boolean = false,
    val isLoadingSecondVerseText: Boolean = false,
    val isTranslationPickerOpen: Boolean = false,
    val translationPickerTarget: TranslationPickerTarget = TranslationPickerTarget.FIRST
) {
    val firstTranslation: BibleTranslation?
        get() = availableTranslations.find { it.id.equals(firstTranslationId, ignoreCase = true) }
            ?: if (firstTranslationId.equals(BibleTranslationDefaults.DEFAULT_FIRST, ignoreCase = true)) {
                BibleTranslation(
                    id = "BSB",
                    name = "Berean Standard Bible",
                    englishName = "Berean Standard Bible",
                    shortName = "BSB",
                    language = "eng"
                )
            } else null

    val secondTranslation: BibleTranslation?
        get() = availableTranslations.find { it.id.equals(secondTranslationId, ignoreCase = true) }
            ?: when (secondTranslationId.lowercase()) {
                "rus_syn" -> BibleTranslation(
                    id = "rus_syn",
                    name = "Синодальный перевод",
                    englishName = "Russian Synodal Bible",
                    shortName = "SYN",
                    language = "rus"
                )
                "ukr_1996" -> BibleTranslation(
                    id = "ukr_1996",
                    name = "Біблія 1996",
                    englishName = "Ukrainian Bible 1996",
                    shortName = "UKR",
                    language = "ukr"
                )
                "spa_r09" -> BibleTranslation(
                    id = "spa_r09",
                    name = "Reina Valera 1909",
                    englishName = "Reina Valera 1909",
                    shortName = "R09",
                    language = "spa"
                )
                "deu_elbbk" -> BibleTranslation(
                    id = "deu_elbbk",
                    name = "Elberfelder Bibel",
                    englishName = "Elberfelder Bible",
                    shortName = "ELB",
                    language = "deu"
                )
                else -> BibleTranslation(
                    id = "eng_kjv",
                    name = "King James (Authorized) Version",
                    englishName = "King James Version",
                    shortName = "KJV",
                    language = "eng"
                )
            }

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
    data class ReadSermon(val sermon: Sermon) : ScriptureUiEvent
    data class ToggleFavorite(val sermon: Sermon) : ScriptureUiEvent
    data class ClickSermon(val sermon: Sermon) : ScriptureUiEvent

    // Translation Selection
    data class OpenTranslationPicker(val target: TranslationPickerTarget) : ScriptureUiEvent
    data object DismissTranslationPicker : ScriptureUiEvent
    data class SelectTranslation(val translation: BibleTranslation, val target: TranslationPickerTarget) : ScriptureUiEvent
}

@HiltViewModel
class ScriptureViewModel @Inject constructor(
    private val getScriptureBooksUseCase: GetScriptureBooksUseCase,
    private val getScriptureChapterUseCase: GetScriptureChapterUseCase,
    private val getScriptureVerseUseCase: GetScriptureVerseUseCase,
    private val getBibleTranslationsUseCase: GetBibleTranslationsUseCase,
    private val getBibleVerseTextUseCase: GetBibleVerseTextUseCase,
    private val getFirstTranslationUseCase: GetFirstTranslationUseCase,
    private val getSecondTranslationUseCase: GetSecondTranslationUseCase,
    private val setFirstTranslationUseCase: SetFirstTranslationUseCase,
    private val setSecondTranslationUseCase: SetSecondTranslationUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScriptureUiState())
    val uiState: StateFlow<ScriptureUiState> = _uiState.asStateFlow()

    init {
        observeFavorites()
        observeTranslations()
        loadBooks()
        loadTranslations()
    }

    private fun observeTranslations() {
        viewModelScope.launch {
            getFirstTranslationUseCase().collect { id ->
                val prev = _uiState.value.firstTranslationId
                _uiState.update { it.copy(firstTranslationId = id) }
                if (prev != id) {
                    _uiState.value.selectedVerse?.let { verse ->
                        val book = _uiState.value.selectedBook ?: return@let
                        val chapter = _uiState.value.selectedChapter ?: return@let
                        loadFirstVerseText(book.id, chapter, verse)
                    }
                }
            }
        }
        viewModelScope.launch {
            getSecondTranslationUseCase().collect { id ->
                val prev = _uiState.value.secondTranslationId
                _uiState.update { it.copy(secondTranslationId = id) }
                if (prev != id) {
                    _uiState.value.selectedVerse?.let { verse ->
                        val book = _uiState.value.selectedBook ?: return@let
                        val chapter = _uiState.value.selectedChapter ?: return@let
                        loadSecondVerseText(book.id, chapter, verse)
                    }
                }
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            getFavoritesUseCase().collect { favs ->
                val favIds = favs.map { it.id }.toSet()
                _uiState.update { it.copy(favoriteIds = favIds) }
            }
        }
    }

    private fun loadTranslations() {
        viewModelScope.launch {
            getBibleTranslationsUseCase()
                .onSuccess { translations ->
                    _uiState.update { it.copy(availableTranslations = translations) }
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
                        verseSermons = emptyList(),
                        firstVerseText = null,
                        secondVerseText = null
                    )
                }
            }
            is ScriptureUiEvent.SelectChapter -> {
                _uiState.update {
                    it.copy(
                        selectedChapter = event.chapter,
                        selectedVerse = null,
                        verseSermons = emptyList(),
                        firstVerseText = null,
                        secondVerseText = null
                    )
                }
                loadChapterData(event.chapter)
            }
            is ScriptureUiEvent.SelectVerse -> {
                _uiState.update { it.copy(selectedVerse = event.verse) }
                loadVerseSermons(event.verse)
                loadBothVerseTexts(event.verse)
            }
            is ScriptureUiEvent.ClearVerse -> {
                _uiState.update {
                    it.copy(
                        selectedVerse = null,
                        verseSermons = emptyList(),
                        firstVerseText = null,
                        secondVerseText = null
                    )
                }
            }
            is ScriptureUiEvent.ClearChapter -> {
                _uiState.update {
                    it.copy(
                        selectedChapter = null,
                        selectedVerse = null,
                        chapterData = null,
                        verseSermons = emptyList(),
                        firstVerseText = null,
                        secondVerseText = null
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
                        verseSermons = emptyList(),
                        firstVerseText = null,
                        secondVerseText = null
                    )
                }
            }
            is ScriptureUiEvent.PlaySermon -> playbackManager.playSermon(event.sermon)
            is ScriptureUiEvent.ReadSermon -> Unit
            is ScriptureUiEvent.ToggleFavorite -> {
                viewModelScope.launch {
                    toggleFavoriteUseCase(event.sermon)
                }
            }
            is ScriptureUiEvent.ClickSermon -> Unit

            // Translation Picker Events
            is ScriptureUiEvent.OpenTranslationPicker -> {
                _uiState.update {
                    it.copy(
                        isTranslationPickerOpen = true,
                        translationPickerTarget = event.target
                    )
                }
            }
            is ScriptureUiEvent.DismissTranslationPicker -> {
                _uiState.update { it.copy(isTranslationPickerOpen = false) }
            }
            is ScriptureUiEvent.SelectTranslation -> {
                viewModelScope.launch {
                    if (event.target == TranslationPickerTarget.FIRST) {
                        setFirstTranslationUseCase(event.translation.id)
                        _uiState.update {
                            it.copy(
                                firstTranslationId = event.translation.id,
                                isTranslationPickerOpen = false
                            )
                        }
                        _uiState.value.selectedVerse?.let { verse ->
                            val book = _uiState.value.selectedBook ?: return@let
                            val chapter = _uiState.value.selectedChapter ?: return@let
                            loadFirstVerseText(book.id, chapter, verse)
                        }
                    } else {
                        setSecondTranslationUseCase(event.translation.id)
                        _uiState.update {
                            it.copy(
                                secondTranslationId = event.translation.id,
                                isTranslationPickerOpen = false
                            )
                        }
                        _uiState.value.selectedVerse?.let { verse ->
                            val book = _uiState.value.selectedBook ?: return@let
                            val chapter = _uiState.value.selectedChapter ?: return@let
                            loadSecondVerseText(book.id, chapter, verse)
                        }
                    }
                }
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

    private fun loadBothVerseTexts(verse: Int) {
        val book = _uiState.value.selectedBook ?: return
        val chapter = _uiState.value.selectedChapter ?: return
        loadFirstVerseText(book.id, chapter, verse)
        loadSecondVerseText(book.id, chapter, verse)
    }

    private fun loadFirstVerseText(bookId: String, chapter: Int, verse: Int) {
        val translationId = _uiState.value.firstTranslationId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFirstVerseText = true, firstVerseText = null) }
            getBibleVerseTextUseCase(translationId, bookId, chapter, verse)
                .onSuccess { text ->
                    _uiState.update { it.copy(isLoadingFirstVerseText = false, firstVerseText = text) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingFirstVerseText = false, firstVerseText = null) }
                }
        }
    }

    private fun loadSecondVerseText(bookId: String, chapter: Int, verse: Int) {
        val translationId = _uiState.value.secondTranslationId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSecondVerseText = true, secondVerseText = null) }
            getBibleVerseTextUseCase(translationId, bookId, chapter, verse)
                .onSuccess { text ->
                    _uiState.update { it.copy(isLoadingSecondVerseText = false, secondVerseText = text) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingSecondVerseText = false, secondVerseText = null) }
                }
        }
    }
}
