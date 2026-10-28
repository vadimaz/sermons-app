package com.example.sermonindex.ui.bible

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.domain.model.BibleBookmark
import com.example.sermonindex.domain.model.BibleBook
import com.example.sermonindex.domain.model.BibleChapterItem
import com.example.sermonindex.domain.model.BibleReadingPreferences
import com.example.sermonindex.domain.model.BibleTranslation
import com.example.sermonindex.domain.model.HighlightColor
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.usecase.DeleteBibleBookmarkForVerseUseCase
import com.example.sermonindex.domain.usecase.DeleteBibleBookmarkUseCase
import com.example.sermonindex.domain.usecase.GetBibleBookmarksForChapterUseCase
import com.example.sermonindex.domain.usecase.GetBibleBooksForTranslationUseCase
import com.example.sermonindex.domain.usecase.GetBibleChapterUseCase
import com.example.sermonindex.domain.usecase.GetBibleReadingPreferencesUseCase
import com.example.sermonindex.domain.usecase.GetBibleTranslationsUseCase
import com.example.sermonindex.domain.usecase.SaveBibleBookmarkUseCase
import com.example.sermonindex.domain.usecase.SaveBibleNarratorUseCase
import com.example.sermonindex.domain.usecase.SaveBibleReadingPositionUseCase
import com.example.sermonindex.domain.usecase.SaveBibleTypographyPreferencesUseCase
import com.example.sermonindex.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BibleViewModel @Inject constructor(
    private val getBibleChapterUseCase: GetBibleChapterUseCase,
    private val getBibleTranslationsUseCase: GetBibleTranslationsUseCase,
    private val getBibleBooksForTranslationUseCase: GetBibleBooksForTranslationUseCase,
    private val getBibleReadingPreferencesUseCase: GetBibleReadingPreferencesUseCase,
    private val saveBibleReadingPositionUseCase: SaveBibleReadingPositionUseCase,
    private val saveBibleTypographyPreferencesUseCase: SaveBibleTypographyPreferencesUseCase,
    private val saveBibleNarratorUseCase: SaveBibleNarratorUseCase,
    private val getBibleBookmarksForChapterUseCase: GetBibleBookmarksForChapterUseCase,
    private val saveBibleBookmarkUseCase: SaveBibleBookmarkUseCase,
    private val deleteBibleBookmarkUseCase: DeleteBibleBookmarkUseCase,
    private val deleteBibleBookmarkForVerseUseCase: DeleteBibleBookmarkForVerseUseCase,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BibleUiState())
    val uiState: StateFlow<BibleUiState> = _uiState.asStateFlow()

    private var bookmarksJob: Job? = null

    init {
        observePreferences()
        observePlayback()
        loadTranslations()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            getBibleReadingPreferencesUseCase().distinctUntilChanged().collectLatest { prefs ->
                val prev = _uiState.value.preferences
                val isExplicitPositionChange = prev.positionUpdatedAt != prefs.positionUpdatedAt ||
                    prev.translationId != prefs.translationId ||
                    prev.bookId != prefs.bookId ||
                    prev.chapter != prefs.chapter ||
                    prev.verse != prefs.verse

                val targetVerse = if (isExplicitPositionChange) {
                    when {
                        prefs.verse > 1 -> prefs.verse
                        prev.positionUpdatedAt != prefs.positionUpdatedAt &&
                            prefs.positionUpdatedAt > 0L &&
                            prev.bookId.equals(prefs.bookId, ignoreCase = true) &&
                            prev.chapter == prefs.chapter -> prefs.verse
                        else -> null
                    }
                } else null

                _uiState.update {
                    it.copy(
                        preferences = prefs,
                        targetScrollVerse = targetVerse ?: it.targetScrollVerse
                    )
                }

                if (_uiState.value.currentChapter == null ||
                    prev.translationId != prefs.translationId ||
                    prev.bookId != prefs.bookId ||
                    prev.chapter != prefs.chapter
                ) {
                    loadBooksAndChapter(prefs.translationId, prefs.bookId, prefs.chapter)
                }
            }
        }
    }

    private fun observePlayback() {
        viewModelScope.launch {
            playbackManager.playbackState.collectLatest { state ->
                val currentChapter = _uiState.value.currentChapter
                val isPlayingCurrent = if (currentChapter != null) {
                    val currentTrackId = "bible_${currentChapter.translationId}_${currentChapter.bookId}_${currentChapter.chapterNumber}"
                    state.currentSermon?.id == currentTrackId && state.isPlaying
                } else false

                _uiState.update { it.copy(isAudioPlaying = isPlayingCurrent) }
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

    private fun loadBooksAndChapter(translationId: String, bookId: String, chapter: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Load books for translation
            val books = getBibleBooksForTranslationUseCase(translationId).getOrElse { emptyList() }
            val currentBook = books.firstOrNull { it.id.equals(bookId, ignoreCase = true) }
                ?: books.firstOrNull()

            val effectiveBookId = currentBook?.id ?: bookId

            // 2. Load chapter
            getBibleChapterUseCase(translationId, effectiveBookId, chapter)
                .onSuccess { chapterData ->
                    observeBookmarksForChapter(translationId, effectiveBookId, chapter)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            books = books,
                            currentBook = currentBook,
                            currentChapter = chapterData
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load chapter",
                            books = books,
                            currentBook = currentBook
                        )
                    }
                }
        }
    }

    fun onEvent(event: BibleUiEvent) {
        when (event) {
            is BibleUiEvent.SelectBookAndChapter -> {
                stopChapterAudio()
                saveReadingPosition(
                    translationId = _uiState.value.preferences.translationId,
                    bookId = event.bookId,
                    chapter = event.chapter
                )
                _uiState.update { it.copy(isTableOfContentsOpen = false) }
            }
            is BibleUiEvent.NextChapter -> {
                stopChapterAudio()
                navigateNextChapter()
            }
            is BibleUiEvent.PreviousChapter -> {
                stopChapterAudio()
                navigatePreviousChapter()
            }
            is BibleUiEvent.SelectTranslation -> {
                stopChapterAudio()
                val translationId = event.translation.id
                val currentBookId = _uiState.value.currentBook?.id ?: _uiState.value.preferences.bookId
                val currentChapter = _uiState.value.preferences.chapter
                saveReadingPosition(translationId, currentBookId, currentChapter)
                _uiState.update { it.copy(isTranslationPickerOpen = false) }
            }
            is BibleUiEvent.SetTableOfContentsOpen -> {
                _uiState.update { it.copy(isTableOfContentsOpen = event.isOpen) }
            }
            is BibleUiEvent.SetSettingsOpen -> {
                _uiState.update { it.copy(isSettingsOpen = event.isOpen) }
            }
            is BibleUiEvent.SetTranslationPickerOpen -> {
                _uiState.update { it.copy(isTranslationPickerOpen = event.isOpen) }
            }
            is BibleUiEvent.ShowFootnote -> {
                _uiState.update { it.copy(activeFootnote = event.footnote) }
            }
            is BibleUiEvent.DismissFootnote -> {
                _uiState.update { it.copy(activeFootnote = null) }
            }
            is BibleUiEvent.UpdateFontSize -> {
                val prefs = _uiState.value.preferences
                viewModelScope.launch {
                    saveBibleTypographyPreferencesUseCase(
                        fontSizeSp = event.fontSizeSp,
                        isSerif = prefs.isSerif,
                        lineSpacingMultiplier = prefs.lineSpacingMultiplier
                    )
                }
            }
            is BibleUiEvent.ToggleFontSerif -> {
                val prefs = _uiState.value.preferences
                viewModelScope.launch {
                    saveBibleTypographyPreferencesUseCase(
                        fontSizeSp = prefs.fontSizeSp,
                        isSerif = event.isSerif,
                        lineSpacingMultiplier = prefs.lineSpacingMultiplier
                    )
                }
            }
            is BibleUiEvent.UpdateLineSpacing -> {
                val prefs = _uiState.value.preferences
                viewModelScope.launch {
                    saveBibleTypographyPreferencesUseCase(
                        fontSizeSp = prefs.fontSizeSp,
                        isSerif = prefs.isSerif,
                        lineSpacingMultiplier = event.lineSpacingMultiplier
                    )
                }
            }
            is BibleUiEvent.ToggleAudio -> {
                toggleChapterAudio(event.narrator)
            }
            is BibleUiEvent.SelectNarrator -> {
                viewModelScope.launch {
                    saveBibleNarratorUseCase(event.narrator)
                    toggleChapterAudio(event.narrator)
                }
            }
            is BibleUiEvent.SeekToVerse -> {
                seekToVerse(event.verseNumber)
            }
            is BibleUiEvent.StopAudio -> {
                stopChapterAudio()
            }
            is BibleUiEvent.Retry -> {
                val prefs = _uiState.value.preferences
                loadBooksAndChapter(prefs.translationId, prefs.bookId, prefs.chapter)
            }
            is BibleUiEvent.OpenSermonsForVerse -> {
                // Handled in UI navigation
            }
            is BibleUiEvent.StartVerseSelection -> {
                _uiState.update {
                    it.copy(
                        isSelectionMode = true,
                        selectedVerse = event.verseNumber
                    )
                }
            }
            is BibleUiEvent.ClearSelection -> {
                _uiState.update {
                    it.copy(
                        isSelectionMode = false,
                        selectedVerse = null
                    )
                }
            }
            is BibleUiEvent.ApplyHighlight -> {
                applyHighlightToSelection(event.color)
            }
            is BibleUiEvent.DeleteBookmarkForCurrentVerse -> {
                deleteBookmarkForSelection()
            }
            is BibleUiEvent.SetQuoteCardSheetOpen -> {
                _uiState.update { it.copy(showQuoteCardSheet = event.isOpen) }
            }
            is BibleUiEvent.ClearTargetScrollVerse -> {
                _uiState.update { it.copy(targetScrollVerse = null) }
            }
        }
    }

    private fun observeBookmarksForChapter(translationId: String, bookId: String, chapter: Int) {
        bookmarksJob?.cancel()
        bookmarksJob = viewModelScope.launch {
            getBibleBookmarksForChapterUseCase(translationId, bookId, chapter).collectLatest { bookmarks ->
                _uiState.update { it.copy(chapterBookmarks = bookmarks) }
            }
        }
    }

    private fun applyHighlightToSelection(color: HighlightColor) {
        val chapter = _uiState.value.currentChapter ?: return
        val selected = _uiState.value.selectedVerse ?: return

        val existing = _uiState.value.chapterBookmarks.firstOrNull { it.verse == selected }
        val verseItem = chapter.items.filterIsInstance<BibleChapterItem.Verse>()
            .firstOrNull { it.number == selected } ?: return

        viewModelScope.launch {
            val bookmark = existing?.copy(color = color) ?: BibleBookmark(
                id = "${chapter.translationId}:${chapter.bookId}:${chapter.chapterNumber}:$selected",
                translationId = chapter.translationId,
                bookId = chapter.bookId,
                bookName = chapter.bookName,
                chapter = chapter.chapterNumber,
                verse = selected,
                text = verseItem.text,
                color = color
            )
            saveBibleBookmarkUseCase(bookmark)
            _uiState.update { it.copy(isSelectionMode = false, selectedVerse = null) }
        }
    }

    private fun deleteBookmarkForSelection() {
        val selected = _uiState.value.selectedVerse ?: return
        val existing = _uiState.value.chapterBookmarks.firstOrNull { it.verse == selected }

        viewModelScope.launch {
            if (existing != null) {
                deleteBibleBookmarkUseCase(existing.id)
            }
            _uiState.update { it.copy(isSelectionMode = false, selectedVerse = null) }
        }
    }

    private fun navigateNextChapter() {
        val chapter = _uiState.value.currentChapter ?: return
        val currentBook = _uiState.value.currentBook
        val books = _uiState.value.books

        if (currentBook != null && chapter.chapterNumber < currentBook.chapterCount) {
            saveReadingPosition(
                translationId = chapter.translationId,
                bookId = currentBook.id,
                chapter = chapter.chapterNumber + 1
            )
        } else if (books.isNotEmpty()) {
            val currentIdx = books.indexOfFirst { it.id.equals(currentBook?.id, ignoreCase = true) }
            if (currentIdx != -1 && currentIdx + 1 < books.size) {
                val nextBook = books[currentIdx + 1]
                saveReadingPosition(
                    translationId = chapter.translationId,
                    bookId = nextBook.id,
                    chapter = 1
                )
            }
        }
    }

    private fun navigatePreviousChapter() {
        val chapter = _uiState.value.currentChapter ?: return
        val currentBook = _uiState.value.currentBook
        val books = _uiState.value.books

        if (chapter.chapterNumber > 1) {
            saveReadingPosition(
                translationId = chapter.translationId,
                bookId = chapter.bookId,
                chapter = chapter.chapterNumber - 1
            )
        } else if (books.isNotEmpty()) {
            val currentIdx = books.indexOfFirst { it.id.equals(currentBook?.id, ignoreCase = true) }
            if (currentIdx > 0) {
                val prevBook = books[currentIdx - 1]
                saveReadingPosition(
                    translationId = chapter.translationId,
                    bookId = prevBook.id,
                    chapter = prevBook.chapterCount.coerceAtLeast(1)
                )
            }
        }
    }

    private fun saveReadingPosition(translationId: String, bookId: String, chapter: Int) {
        viewModelScope.launch {
            saveBibleReadingPositionUseCase(
                translationId = translationId,
                bookId = bookId,
                chapter = chapter,
                verse = 1
            )
        }
    }

    private fun toggleChapterAudio(narratorOverride: String? = null) {
        val chapter = _uiState.value.currentChapter ?: return
        val audioLinks = chapter.audioLinks
        if (audioLinks.isEmpty()) return

        val preferredNarrator = narratorOverride
            ?: _uiState.value.preferences.selectedNarrator
            ?: audioLinks.keys.firstOrNull()
            ?: return

        val audioUrl = audioLinks[preferredNarrator] ?: audioLinks.values.firstOrNull() ?: return
        val trackId = "bible_${chapter.translationId}_${chapter.bookId}_${chapter.chapterNumber}"

        val isCurrentlyPlaying = playbackManager.playbackState.value.currentSermon?.id == trackId &&
            playbackManager.playbackState.value.isPlaying

        if (isCurrentlyPlaying) {
            playbackManager.pause()
        } else {
            val narratorLabel = preferredNarrator.replaceFirstChar { it.uppercase() }
            val chapterTitle = "${chapter.bookName} ${chapter.chapterNumber}"
            val pseudoSermon = Sermon(
                id = trackId,
                title = chapterTitle,
                speaker = "$narratorLabel (${chapter.translationId.uppercase()})",
                mp3Url = audioUrl,
                cdnMp3Url = audioUrl,
                duration = ""
            )
            playbackManager.playSermon(pseudoSermon)
        }
    }

    private fun seekToVerse(verseNumber: Int) {
        val chapter = _uiState.value.currentChapter ?: return
        val currentTrackId = "bible_${chapter.translationId}_${chapter.bookId}_${chapter.chapterNumber}"
        val playback = playbackManager.playbackState.value
        if (playback.currentSermon?.id != currentTrackId || !playback.isPlaying) return

        val totalDurationMs = playback.durationMs
        if (totalDurationMs <= 0L) return

        val verses = chapter.items.filterIsInstance<BibleChapterItem.Verse>()
        val totalChars = verses.sumOf { it.text.length.coerceAtLeast(1) }
        if (totalChars <= 0) return

        var charsBefore = 0
        for (v in verses) {
            if (v.number == verseNumber) break
            charsBefore += v.text.length.coerceAtLeast(1)
        }

        val fraction = charsBefore.toDouble() / totalChars.toDouble()
        val targetPositionMs = (totalDurationMs * fraction).toLong().coerceIn(0L, totalDurationMs)
        playbackManager.seekTo(targetPositionMs)
    }

    private fun stopChapterAudio() {
        val chapter = _uiState.value.currentChapter ?: return
        val currentTrackId = "bible_${chapter.translationId}_${chapter.bookId}_${chapter.chapterNumber}"
        if (playbackManager.playbackState.value.currentSermon?.id == currentTrackId) {
            playbackManager.stop()
        }
    }
}
