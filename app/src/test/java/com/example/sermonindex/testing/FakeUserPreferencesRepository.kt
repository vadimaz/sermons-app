package com.example.sermonindex.testing

import com.example.sermonindex.domain.model.AppThemeMode
import com.example.sermonindex.domain.model.BibleReadingPreferences
import com.example.sermonindex.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * State-backed Fake implementation of [UserPreferencesRepository] for unit tests.
 */
class FakeUserPreferencesRepository(
    initialFirstTranslation: String = "BSB",
    initialSecondTranslation: String = "rus_syn",
    initialCatalogBannerDismissed: Boolean = false,
    initialReadingPreferences: BibleReadingPreferences = BibleReadingPreferences(),
    initialAutoUpdateCheckEnabled: Boolean = false,
    initialLastUpdateCheckTimestamp: Long = 0L,
    initialThemeMode: AppThemeMode = AppThemeMode.SYSTEM
) : UserPreferencesRepository {

    val firstTranslationFlow = MutableStateFlow(initialFirstTranslation)
    val secondTranslationFlow = MutableStateFlow(initialSecondTranslation)
    val catalogBannerDismissedFlow = MutableStateFlow(initialCatalogBannerDismissed)
    val readingPreferencesFlow = MutableStateFlow(initialReadingPreferences)
    val autoUpdateCheckEnabledFlow = MutableStateFlow(initialAutoUpdateCheckEnabled)
    val lastUpdateCheckTimestampFlow = MutableStateFlow(initialLastUpdateCheckTimestamp)
    val themeModeFlow = MutableStateFlow(initialThemeMode)

    override val firstTranslationId: Flow<String> = firstTranslationFlow.asStateFlow()
    override val secondTranslationId: Flow<String> = secondTranslationFlow.asStateFlow()

    override suspend fun setFirstTranslation(id: String) {
        firstTranslationFlow.value = id
    }

    override suspend fun setSecondTranslation(id: String) {
        secondTranslationFlow.value = id
    }

    override val isCatalogReadyBannerDismissed: Flow<Boolean> = catalogBannerDismissedFlow.asStateFlow()

    override suspend fun setCatalogReadyBannerDismissed(dismissed: Boolean) {
        catalogBannerDismissedFlow.value = dismissed
    }

    override val bibleReadingPreferences: Flow<BibleReadingPreferences> = readingPreferencesFlow.asStateFlow()

    override suspend fun saveBibleReadingPosition(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int
    ) {
        readingPreferencesFlow.update {
            it.copy(
                translationId = translationId,
                bookId = bookId,
                chapter = chapter,
                verse = verse
            )
        }
    }

    override suspend fun saveBibleTypography(
        fontSizeSp: Float,
        isSerif: Boolean,
        lineSpacingMultiplier: Float
    ) {
        readingPreferencesFlow.update {
            it.copy(
                fontSizeSp = fontSizeSp,
                isSerif = isSerif,
                lineSpacingMultiplier = lineSpacingMultiplier
            )
        }
    }

    override suspend fun saveBibleNarrator(narrator: String?) {
        readingPreferencesFlow.update {
            it.copy(selectedNarrator = narrator)
        }
    }

    override val isAutoCatalogUpdateCheckEnabled: Flow<Boolean> = autoUpdateCheckEnabledFlow.asStateFlow()

    override suspend fun setAutoCatalogUpdateCheckEnabled(enabled: Boolean) {
        autoUpdateCheckEnabledFlow.value = enabled
    }

    override val lastCatalogUpdateCheckTimestamp: Flow<Long> = lastUpdateCheckTimestampFlow.asStateFlow()

    override suspend fun setLastCatalogUpdateCheckTimestamp(timestamp: Long) {
        lastUpdateCheckTimestampFlow.value = timestamp
    }

    override val themeMode: Flow<AppThemeMode> = themeModeFlow.asStateFlow()

    override suspend fun setThemeMode(mode: AppThemeMode) {
        themeModeFlow.value = mode
    }
}
