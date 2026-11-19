package com.example.sermonindex.ui.bible.sermons

import androidx.compose.runtime.Immutable
import com.example.sermonindex.domain.model.BibleTranslation
import com.example.sermonindex.domain.model.BibleTranslationDefaults
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.ui.scripture.TranslationPickerTarget

@Immutable
data class BibleVerseSermonsUiState(
    val bookId: String = "",
    val bookName: String = "",
    val chapter: Int = 1,
    val verse: Int = 1,
    val sermons: List<Sermon> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val isLoadingSermons: Boolean = true,
    val errorMessage: String? = null,

    // Dual Translations
    val firstTranslationId: String = BibleTranslationDefaults.DEFAULT_FIRST,
    val secondTranslationId: String = BibleTranslationDefaults.getDefaultSecondaryTranslation(),
    val availableTranslations: List<BibleTranslation> = emptyList(),
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
}

sealed interface BibleVerseSermonsUiEvent {
    data object Retry : BibleVerseSermonsUiEvent
    data class PlaySermon(val sermon: Sermon) : BibleVerseSermonsUiEvent
    data class ReadSermon(val sermon: Sermon) : BibleVerseSermonsUiEvent
    data class ToggleFavorite(val sermon: Sermon) : BibleVerseSermonsUiEvent
    data class ClickSermon(val sermon: Sermon) : BibleVerseSermonsUiEvent

    // Translation Selection
    data class OpenTranslationPicker(val target: TranslationPickerTarget) : BibleVerseSermonsUiEvent
    data object DismissTranslationPicker : BibleVerseSermonsUiEvent
    data class SelectTranslation(val translation: BibleTranslation, val target: TranslationPickerTarget) : BibleVerseSermonsUiEvent
}

data class BibleVerseArgs(
    val bookId: String,
    val bookName: String,
    val chapter: Int,
    val verse: Int
)
