package com.example.sermonindex.domain.usecase

import com.example.sermonindex.domain.model.BibleReadingPreferences
import com.example.sermonindex.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CatalogPreferencesUseCasesTest {

    private lateinit var fakeRepository: FakeUserPreferencesRepository

    @Before
    fun setup() {
        fakeRepository = FakeUserPreferencesRepository()
    }

    @Test
    fun getAndSetAutoCatalogUpdateCheckEnabled_operatesCorrectly() = runTest {
        val getUseCase = GetAutoCatalogUpdateCheckEnabledUseCase(fakeRepository)
        val setUseCase = SetAutoCatalogUpdateCheckEnabledUseCase(fakeRepository)

        assertTrue(getUseCase().first())

        setUseCase(false)
        assertFalse(getUseCase().first())

        setUseCase(true)
        assertTrue(getUseCase().first())
    }

    @Test
    fun getAndSetLastCatalogUpdateCheckTimestamp_operatesCorrectly() = runTest {
        val getUseCase = GetLastCatalogUpdateCheckTimestampUseCase(fakeRepository)
        val setUseCase = SetLastCatalogUpdateCheckTimestampUseCase(fakeRepository)

        assertEquals(0L, getUseCase().first())

        val testTimestamp = 1729958400000L
        setUseCase(testTimestamp)
        assertEquals(testTimestamp, getUseCase().first())
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        private val _autoUpdateCheck = MutableStateFlow(true)
        private val _lastCheckTimestamp = MutableStateFlow(0L)

        override val isAutoCatalogUpdateCheckEnabled: Flow<Boolean> = _autoUpdateCheck
        override suspend fun setAutoCatalogUpdateCheckEnabled(enabled: Boolean) {
            _autoUpdateCheck.value = enabled
        }

        override val lastCatalogUpdateCheckTimestamp: Flow<Long> = _lastCheckTimestamp
        override suspend fun setLastCatalogUpdateCheckTimestamp(timestamp: Long) {
            _lastCheckTimestamp.value = timestamp
        }

        override val firstTranslationId: Flow<String> = MutableStateFlow("BSB")
        override val secondTranslationId: Flow<String> = MutableStateFlow("rus_syn")
        override suspend fun setFirstTranslation(id: String) {}
        override suspend fun setSecondTranslation(id: String) {}
        override val isCatalogReadyBannerDismissed: Flow<Boolean> = MutableStateFlow(false)
        override suspend fun setCatalogReadyBannerDismissed(dismissed: Boolean) {}
        override val bibleReadingPreferences: Flow<BibleReadingPreferences> = MutableStateFlow(BibleReadingPreferences())
        override suspend fun saveBibleReadingPosition(translationId: String, bookId: String, chapter: Int, verse: Int) {}
        override suspend fun saveBibleTypography(fontSizeSp: Float, isSerif: Boolean, lineSpacingMultiplier: Float) {}
        override suspend fun saveBibleNarrator(narrator: String?) {}
    }
}
