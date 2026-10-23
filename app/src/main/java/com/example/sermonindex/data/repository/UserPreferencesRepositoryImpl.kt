package com.example.sermonindex.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.sermonindex.di.IoDispatcher
import com.example.sermonindex.domain.model.BibleReadingPreferences
import com.example.sermonindex.domain.model.BibleTranslationDefaults
import com.example.sermonindex.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val FIRST_TRANSLATION = stringPreferencesKey("first_translation_v2")
        val SECOND_TRANSLATION = stringPreferencesKey("second_translation_v2")
        val CATALOG_READY_DISMISSED = booleanPreferencesKey("catalog_ready_dismissed")

        val BIBLE_TRANSLATION_ID = stringPreferencesKey("bible_reading_translation_id")
        val BIBLE_BOOK_ID = stringPreferencesKey("bible_reading_book_id")
        val BIBLE_CHAPTER = intPreferencesKey("bible_reading_chapter")
        val BIBLE_VERSE = intPreferencesKey("bible_reading_verse")
        val BIBLE_FONT_SIZE_SP = floatPreferencesKey("bible_reading_font_size_sp")
        val BIBLE_IS_SERIF = booleanPreferencesKey("bible_reading_is_serif")
        val BIBLE_LINE_SPACING = floatPreferencesKey("bible_reading_line_spacing_multiplier")
        val BIBLE_SELECTED_NARRATOR = stringPreferencesKey("bible_reading_selected_narrator")
    }

    private val safePreferencesFlow: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    override val firstTranslationId: Flow<String> = safePreferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.FIRST_TRANSLATION] ?: BibleTranslationDefaults.DEFAULT_FIRST
        }
        .flowOn(ioDispatcher)

    override val secondTranslationId: Flow<String> = safePreferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.SECOND_TRANSLATION]
                ?: BibleTranslationDefaults.getDefaultSecondaryTranslation()
        }
        .flowOn(ioDispatcher)

    override suspend fun setFirstTranslation(id: String) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.FIRST_TRANSLATION] = id
            }
        }
    }

    override suspend fun setSecondTranslation(id: String) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.SECOND_TRANSLATION] = id
            }
        }
    }

    override val isCatalogReadyBannerDismissed: Flow<Boolean> = safePreferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.CATALOG_READY_DISMISSED] ?: false
        }
        .flowOn(ioDispatcher)

    override suspend fun setCatalogReadyBannerDismissed(dismissed: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.CATALOG_READY_DISMISSED] = dismissed
            }
        }
    }

    override val bibleReadingPreferences: Flow<BibleReadingPreferences> = safePreferencesFlow
        .map { preferences ->
            val defaultTranslation = BibleTranslationDefaults.getDefaultSecondaryTranslation().let {
                if (it.startsWith("rus") || it.startsWith("ukr") || it.startsWith("spa")) it else "BSB"
            }
            BibleReadingPreferences(
                translationId = preferences[PreferencesKeys.BIBLE_TRANSLATION_ID] ?: defaultTranslation,
                bookId = preferences[PreferencesKeys.BIBLE_BOOK_ID] ?: "GEN",
                chapter = preferences[PreferencesKeys.BIBLE_CHAPTER] ?: 1,
                verse = preferences[PreferencesKeys.BIBLE_VERSE] ?: 1,
                fontSizeSp = preferences[PreferencesKeys.BIBLE_FONT_SIZE_SP] ?: 18.0f,
                isSerif = preferences[PreferencesKeys.BIBLE_IS_SERIF] ?: true,
                lineSpacingMultiplier = preferences[PreferencesKeys.BIBLE_LINE_SPACING] ?: 1.5f,
                selectedNarrator = preferences[PreferencesKeys.BIBLE_SELECTED_NARRATOR]
            )
        }
        .flowOn(ioDispatcher)

    override suspend fun saveBibleReadingPosition(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int
    ) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.BIBLE_TRANSLATION_ID] = translationId
                preferences[PreferencesKeys.BIBLE_BOOK_ID] = bookId
                preferences[PreferencesKeys.BIBLE_CHAPTER] = chapter
                preferences[PreferencesKeys.BIBLE_VERSE] = verse
            }
        }
    }

    override suspend fun saveBibleTypography(
        fontSizeSp: Float,
        isSerif: Boolean,
        lineSpacingMultiplier: Float
    ) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.BIBLE_FONT_SIZE_SP] = fontSizeSp
                preferences[PreferencesKeys.BIBLE_IS_SERIF] = isSerif
                preferences[PreferencesKeys.BIBLE_LINE_SPACING] = lineSpacingMultiplier
            }
        }
    }

    override suspend fun saveBibleNarrator(narrator: String?) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                if (narrator == null) {
                    preferences.remove(PreferencesKeys.BIBLE_SELECTED_NARRATOR)
                } else {
                    preferences[PreferencesKeys.BIBLE_SELECTED_NARRATOR] = narrator
                }
            }
        }
    }
}
