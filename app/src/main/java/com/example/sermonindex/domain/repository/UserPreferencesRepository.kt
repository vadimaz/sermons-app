package com.example.sermonindex.domain.repository

import com.example.sermonindex.domain.model.BibleReadingPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Pure Kotlin domain repository defining contracts for user-configurable preferences.
 * Adheres strictly to Clean Architecture (zero Android framework dependencies).
 */
interface UserPreferencesRepository {
    /**
     * Primary Bible translation ID for Scripture Explorer (default: BSB).
     */
    val firstTranslationId: Flow<String>

    /**
     * Secondary Bible translation ID for parallel Scripture viewing.
     */
    val secondTranslationId: Flow<String>

    suspend fun setFirstTranslation(id: String)
    suspend fun setSecondTranslation(id: String)

    /**
     * Flag indicating whether the offline catalog download recommendation banner in Search has been dismissed.
     */
    val isCatalogReadyBannerDismissed: Flow<Boolean>

    suspend fun setCatalogReadyBannerDismissed(dismissed: Boolean)

    /**
     * User's persistent Bible reading state and typography preferences.
     */
    val bibleReadingPreferences: Flow<BibleReadingPreferences>

    suspend fun saveBibleReadingPosition(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int
    )

    suspend fun saveBibleTypography(
        fontSizeSp: Float,
        isSerif: Boolean,
        lineSpacingMultiplier: Float
    )

    suspend fun saveBibleNarrator(narrator: String?)
}
