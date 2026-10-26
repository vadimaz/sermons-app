package com.example.sermonindex.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.sermonindex.domain.model.BibleTranslationDefaults
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_user_preferences.preferences_pb") }
        )
    }

    @Test
    fun defaultPreferences_returnsExpectedValues() = testScope.runTest {
        val repository = UserPreferencesRepositoryImpl(
            dataStore = dataStore,
            ioDispatcher = testDispatcher
        )

        assertEquals(BibleTranslationDefaults.DEFAULT_FIRST, repository.firstTranslationId.first())
        assertEquals(BibleTranslationDefaults.getDefaultSecondaryTranslation(), repository.secondTranslationId.first())
        assertFalse(repository.isCatalogReadyBannerDismissed.first())

        val readingPrefs = repository.bibleReadingPreferences.first()
        assertEquals("GEN", readingPrefs.bookId)
        assertEquals(1, readingPrefs.chapter)
        assertEquals(1, readingPrefs.verse)
        assertEquals(18.0f, readingPrefs.fontSizeSp)
        assertTrue(readingPrefs.isSerif)
        assertEquals(1.5f, readingPrefs.lineSpacingMultiplier)
        assertNull(readingPrefs.selectedNarrator)
        assertTrue(repository.isAutoCatalogUpdateCheckEnabled.first())
        assertEquals(0L, repository.lastCatalogUpdateCheckTimestamp.first())
    }

    @Test
    fun setFirstAndSecondTranslations_persistsCorrectly() = testScope.runTest {
        val repository = UserPreferencesRepositoryImpl(
            dataStore = dataStore,
            ioDispatcher = testDispatcher
        )

        repository.setFirstTranslation("eng_kjv")
        assertEquals("eng_kjv", repository.firstTranslationId.first())

        repository.setSecondTranslation("rus_syn")
        assertEquals("rus_syn", repository.secondTranslationId.first())
    }

    @Test
    fun setCatalogReadyBannerDismissed_persistsCorrectly() = testScope.runTest {
        val repository = UserPreferencesRepositoryImpl(
            dataStore = dataStore,
            ioDispatcher = testDispatcher
        )

        assertFalse(repository.isCatalogReadyBannerDismissed.first())
        repository.setCatalogReadyBannerDismissed(true)
        assertTrue(repository.isCatalogReadyBannerDismissed.first())
    }

    @Test
    fun saveBibleReadingPosition_updatesPreferences() = testScope.runTest {
        val repository = UserPreferencesRepositoryImpl(
            dataStore = dataStore,
            ioDispatcher = testDispatcher
        )

        repository.saveBibleReadingPosition("BSB", "JHN", 3, 16)
        val prefs = repository.bibleReadingPreferences.first()

        assertEquals("BSB", prefs.translationId)
        assertEquals("JHN", prefs.bookId)
        assertEquals(3, prefs.chapter)
        assertEquals(16, prefs.verse)
    }

    @Test
    fun saveBibleTypography_updatesPreferences() = testScope.runTest {
        val repository = UserPreferencesRepositoryImpl(
            dataStore = dataStore,
            ioDispatcher = testDispatcher
        )

        repository.saveBibleTypography(24f, false, 1.8f)
        val prefs = repository.bibleReadingPreferences.first()

        assertEquals(24f, prefs.fontSizeSp)
        assertFalse(prefs.isSerif)
        assertEquals(1.8f, prefs.lineSpacingMultiplier)
    }

    @Test
    fun saveBibleNarrator_updatesAndRemovesNarrator() = testScope.runTest {
        val repository = UserPreferencesRepositoryImpl(
            dataStore = dataStore,
            ioDispatcher = testDispatcher
        )

        repository.saveBibleNarrator("Gilbert")
        assertEquals("Gilbert", repository.bibleReadingPreferences.first().selectedNarrator)

        repository.saveBibleNarrator(null)
        assertNull(repository.bibleReadingPreferences.first().selectedNarrator)
    }

    @Test
    fun setAutoCatalogUpdateCheckEnabled_persistsCorrectly() = testScope.runTest {
        val repository = UserPreferencesRepositoryImpl(
            dataStore = dataStore,
            ioDispatcher = testDispatcher
        )

        repository.setAutoCatalogUpdateCheckEnabled(false)
        assertFalse(repository.isAutoCatalogUpdateCheckEnabled.first())

        repository.setAutoCatalogUpdateCheckEnabled(true)
        assertTrue(repository.isAutoCatalogUpdateCheckEnabled.first())
    }

    @Test
    fun setLastCatalogUpdateCheckTimestamp_persistsCorrectly() = testScope.runTest {
        val repository = UserPreferencesRepositoryImpl(
            dataStore = dataStore,
            ioDispatcher = testDispatcher
        )

        val timestamp = 1729958400000L
        repository.setLastCatalogUpdateCheckTimestamp(timestamp)
        assertEquals(timestamp, repository.lastCatalogUpdateCheckTimestamp.first())
    }
}
