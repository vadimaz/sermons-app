package com.example.sermonindex.ui.bible

import androidx.compose.runtime.Immutable
import com.example.sermonindex.domain.model.BibleBook
import com.example.sermonindex.domain.model.BibleChapter
import com.example.sermonindex.domain.model.BibleFootnote
import com.example.sermonindex.domain.model.BibleReadingPreferences
import com.example.sermonindex.domain.model.BibleTranslation

@Immutable
data class BibleUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val currentChapter: BibleChapter? = null,
    val currentBook: BibleBook? = null,
    val books: List<BibleBook> = emptyList(),
    val availableTranslations: List<BibleTranslation> = emptyList(),
    val preferences: BibleReadingPreferences = BibleReadingPreferences(),

    // UI Sheets & Dialogs
    val isTableOfContentsOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isTranslationPickerOpen: Boolean = false,
    val activeFootnote: BibleFootnote? = null,

    // Audio Narration
    val isAudioPlaying: Boolean = false,
    val currentAudioNarrator: String? = null
) {
    val currentTranslation: BibleTranslation?
        get() = availableTranslations.firstOrNull { it.id.equals(preferences.translationId, ignoreCase = true) }
            ?: currentChapter?.let {
                BibleTranslation(
                    id = it.translationId,
                    name = it.translationId,
                    englishName = it.translationId,
                    shortName = it.translationId,
                    language = ""
                )
            }
}

sealed interface BibleUiEvent {
    data class SelectBookAndChapter(val bookId: String, val chapter: Int) : BibleUiEvent
    data object NextChapter : BibleUiEvent
    data object PreviousChapter : BibleUiEvent
    data class SelectTranslation(val translation: BibleTranslation) : BibleUiEvent

    // Sheets & Dialogs
    data class SetTableOfContentsOpen(val isOpen: Boolean) : BibleUiEvent
    data class SetSettingsOpen(val isOpen: Boolean) : BibleUiEvent
    data class SetTranslationPickerOpen(val isOpen: Boolean) : BibleUiEvent
    data class ShowFootnote(val footnote: BibleFootnote) : BibleUiEvent
    data object DismissFootnote : BibleUiEvent

    // Typography settings
    data class UpdateFontSize(val fontSizeSp: Float) : BibleUiEvent
    data class ToggleFontSerif(val isSerif: Boolean) : BibleUiEvent
    data class UpdateLineSpacing(val lineSpacingMultiplier: Float) : BibleUiEvent

    // Audio Narration
    data class ToggleAudio(val narrator: String? = null) : BibleUiEvent
    data class SelectNarrator(val narrator: String) : BibleUiEvent
    data class SeekToVerse(val verseNumber: Int) : BibleUiEvent
    data object StopAudio : BibleUiEvent

    // Navigation to Verse Sermons
    data class OpenSermonsForVerse(
        val bookId: String,
        val bookName: String,
        val chapter: Int,
        val verse: Int
    ) : BibleUiEvent
    data object Retry : BibleUiEvent
}
