package com.example.sermonindex.ui.bible.sermons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.domain.model.BibleTranslation
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.usecase.GetBibleTranslationsUseCase
import com.example.sermonindex.domain.usecase.GetBibleVerseTextUseCase
import com.example.sermonindex.domain.usecase.GetFavoritesUseCase
import com.example.sermonindex.domain.usecase.GetFirstTranslationUseCase
import com.example.sermonindex.domain.usecase.GetSecondTranslationUseCase
import com.example.sermonindex.domain.usecase.GetScriptureVerseUseCase
import com.example.sermonindex.domain.usecase.SetFirstTranslationUseCase
import com.example.sermonindex.domain.usecase.SetSecondTranslationUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import com.example.sermonindex.playback.PlaybackManager
import com.example.sermonindex.ui.scripture.TranslationPickerTarget
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = BibleVerseSermonsViewModel.Factory::class)
class BibleVerseSermonsViewModel @AssistedInject constructor(
    @Assisted private val args: BibleVerseArgs,
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

    @AssistedFactory
    interface Factory {
        fun create(args: BibleVerseArgs): BibleVerseSermonsViewModel
    }

    private val _uiState = MutableStateFlow(
        BibleVerseSermonsUiState(
            bookId = args.bookId,
            bookName = args.bookName,
            chapter = args.chapter,
            verse = args.verse
        )
    )
    val uiState: StateFlow<BibleVerseSermonsUiState> = _uiState.asStateFlow()

    init {
        observeFavorites()
        observeTranslations()
        loadTranslations()
        loadSermons()
        loadBothVerseTexts()
    }

    private fun observeTranslations() {
        viewModelScope.launch {
            getFirstTranslationUseCase().collect { id ->
                val prev = _uiState.value.firstTranslationId
                _uiState.update { it.copy(firstTranslationId = id) }
                if (prev != id) {
                    loadFirstVerseText(args.bookId, args.chapter, args.verse)
                }
            }
        }
        viewModelScope.launch {
            getSecondTranslationUseCase().collect { id ->
                val prev = _uiState.value.secondTranslationId
                _uiState.update { it.copy(secondTranslationId = id) }
                if (prev != id) {
                    loadSecondVerseText(args.bookId, args.chapter, args.verse)
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
            getBibleTranslationsUseCase().onSuccess { translations ->
                _uiState.update { it.copy(availableTranslations = translations) }
            }
        }
    }

    fun loadSermons() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSermons = true, errorMessage = null) }
            getScriptureVerseUseCase(args.bookId, args.chapter, args.verse)
                .onSuccess { sermons ->
                    _uiState.update {
                        it.copy(
                            isLoadingSermons = false,
                            errorMessage = null,
                            sermons = sermons
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingSermons = false,
                            errorMessage = error.message ?: "Failed to load sermons"
                        )
                    }
                }
        }
    }

    private fun loadBothVerseTexts() {
        loadFirstVerseText(args.bookId, args.chapter, args.verse)
        loadSecondVerseText(args.bookId, args.chapter, args.verse)
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

    fun onEvent(event: BibleVerseSermonsUiEvent) {
        when (event) {
            BibleVerseSermonsUiEvent.Retry -> {
                loadSermons()
                loadBothVerseTexts()
            }
            is BibleVerseSermonsUiEvent.PlaySermon -> {
                playbackManager.playSermon(event.sermon)
            }
            is BibleVerseSermonsUiEvent.ToggleFavorite -> {
                viewModelScope.launch {
                    toggleFavoriteUseCase(event.sermon)
                }
            }
            is BibleVerseSermonsUiEvent.OpenTranslationPicker -> {
                _uiState.update {
                    it.copy(
                        isTranslationPickerOpen = true,
                        translationPickerTarget = event.target
                    )
                }
            }
            BibleVerseSermonsUiEvent.DismissTranslationPicker -> {
                _uiState.update { it.copy(isTranslationPickerOpen = false) }
            }
            is BibleVerseSermonsUiEvent.SelectTranslation -> {
                viewModelScope.launch {
                    if (event.target == TranslationPickerTarget.FIRST) {
                        setFirstTranslationUseCase(event.translation.id)
                        _uiState.update {
                            it.copy(
                                firstTranslationId = event.translation.id,
                                isTranslationPickerOpen = false
                            )
                        }
                        loadFirstVerseText(args.bookId, args.chapter, args.verse)
                    } else {
                        setSecondTranslationUseCase(event.translation.id)
                        _uiState.update {
                            it.copy(
                                secondTranslationId = event.translation.id,
                                isTranslationPickerOpen = false
                            )
                        }
                        loadSecondVerseText(args.bookId, args.chapter, args.verse)
                    }
                }
            }
            is BibleVerseSermonsUiEvent.ClickSermon,
            is BibleVerseSermonsUiEvent.ReadSermon -> {
                // Handled in UI
            }
        }
    }
}
