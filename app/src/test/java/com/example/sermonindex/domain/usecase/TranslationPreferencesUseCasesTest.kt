package com.example.sermonindex.domain.usecase

import com.example.sermonindex.domain.model.BibleReadingPreferences
import com.example.sermonindex.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TranslationPreferencesUseCasesTest {

    private lateinit var fakeRepository: FakeUserPreferencesRepository

    @Before
    fun setup() {
        fakeRepository = FakeUserPreferencesRepository()
    }

    @Test
    fun getAndSetFirstTranslation_operatesCorrectly() = runTest {
        val getUseCase = GetFirstTranslationUseCase(fakeRepository)
        val setUseCase = SetFirstTranslationUseCase(fakeRepository)

        assertEquals("BSB", getUseCase().first())

        setUseCase("eng_kjv")
        assertEquals("eng_kjv", getUseCase().first())
    }

    @Test
    fun getAndSetSecondTranslation_operatesCorrectly() = runTest {
        val getUseCase = GetSecondTranslationUseCase(fakeRepository)
        val setUseCase = SetSecondTranslationUseCase(fakeRepository)

        assertEquals("rus_syn", getUseCase().first())

        setUseCase("spa_r09")
        assertEquals("spa_r09", getUseCase().first())
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        private val _first = MutableStateFlow("BSB")
        private val _second = MutableStateFlow("rus_syn")
        private val _dismissed = MutableStateFlow(false)
        private val _readingPrefs = MutableStateFlow(BibleReadingPreferences())

        override val firstTranslationId: Flow<String> = _first
        override val secondTranslationId: Flow<String> = _second

        override suspend fun setFirstTranslation(id: String) {
            _first.value = id
        }

        override suspend fun setSecondTranslation(id: String) {
            _second.value = id
        }

        override val isCatalogReadyBannerDismissed: Flow<Boolean> = _dismissed

        override suspend fun setCatalogReadyBannerDismissed(dismissed: Boolean) {
            _dismissed.value = dismissed
        }

        override val bibleReadingPreferences: Flow<BibleReadingPreferences> = _readingPrefs

        override suspend fun saveBibleReadingPosition(
            translationId: String,
            bookId: String,
            chapter: Int,
            verse: Int
        ) {
            _readingPrefs.value = _readingPrefs.value.copy(
                translationId = translationId,
                bookId = bookId,
                chapter = chapter,
                verse = verse
            )
        }

        override suspend fun saveBibleTypography(
            fontSizeSp: Float,
            isSerif: Boolean,
            lineSpacingMultiplier: Float
        ) {
            _readingPrefs.value = _readingPrefs.value.copy(
                fontSizeSp = fontSizeSp,
                isSerif = isSerif,
                lineSpacingMultiplier = lineSpacingMultiplier
            )
        }

        override suspend fun saveBibleNarrator(narrator: String?) {
            _readingPrefs.value = _readingPrefs.value.copy(selectedNarrator = narrator)
        }
    }
}
