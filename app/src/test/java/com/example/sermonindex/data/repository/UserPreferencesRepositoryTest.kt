package com.example.sermonindex.data.repository

import java.io.File
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.sermonindex.domain.model.BibleTranslationDefaults
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private fun createRepository(testScope: TestScope): UserPreferencesRepositoryImpl {
        val testFile = File(tmpFolder.root, "test_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { testFile }
        )
        return UserPreferencesRepositoryImpl(
            dataStore = dataStore,
            ioDispatcher = StandardTestDispatcher(testScope.testScheduler)
        )
    }

    @Test
    fun defaultPreferences_returnsExpectedValues() = runTest {
        val repository = createRepository(this)

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
    fun setFirstTranslation_persistsCorrectly() = runTest {
        val repository = createRepository(this)

        repository.setFirstTranslation("eng_kjv")
        assertEquals("eng_kjv", repository.firstTranslationId.first())
    }

    @Test
    fun setSecondTranslation_persistsCorrectly() = runTest {
        val repository = createRepository(this)

        repository.setSecondTranslation("rus_syn")
        assertEquals("rus_syn", repository.secondTranslationId.first())
    }

    @Test
    fun setCatalogReadyBannerDismissed_persistsCorrectly() = runTest {
        val repository = createRepository(this)

        assertFalse(repository.isCatalogReadyBannerDismissed.first())
        repository.setCatalogReadyBannerDismissed(true)
        assertTrue(repository.isCatalogReadyBannerDismissed.first())
    }

    @Test
    fun saveBibleReadingPosition_updatesPreferences() = runTest {
        val repository = createRepository(this)

        repository.saveBibleReadingPosition("BSB", "JHN", 3, 16)
        val prefs = repository.bibleReadingPreferences.first()

        assertEquals("BSB", prefs.translationId)
        assertEquals("JHN", prefs.bookId)
        assertEquals(3, prefs.chapter)
        assertEquals(16, prefs.verse)
        assertTrue(prefs.positionUpdatedAt > 0L)
    }

    @Test
    fun saveBibleTypography_updatesPreferences() = runTest {
        val repository = createRepository(this)

        repository.saveBibleTypography(24f, false, 1.8f)
        val prefs = repository.bibleReadingPreferences.first()

        assertEquals(24f, prefs.fontSizeSp)
        assertFalse(prefs.isSerif)
        assertEquals(1.8f, prefs.lineSpacingMultiplier)
    }

    @Test
    fun saveBibleNarrator_updatesNarrator() = runTest {
        val repository = createRepository(this)

        repository.saveBibleNarrator("Gilbert")
        assertEquals("Gilbert", repository.bibleReadingPreferences.first().selectedNarrator)
    }

    @Test
    fun saveBibleNarrator_clearsNarrator() = runTest {
        val repository = createRepository(this)

        repository.saveBibleNarrator(null)
        assertNull(repository.bibleReadingPreferences.first().selectedNarrator)
    }

    @Test
    fun setAutoCatalogUpdateCheckEnabled_persistsCorrectly() = runTest {
        val repository = createRepository(this)

        repository.setAutoCatalogUpdateCheckEnabled(false)
        assertFalse(repository.isAutoCatalogUpdateCheckEnabled.first())
    }

    @Test
    fun setLastCatalogUpdateCheckTimestamp_persistsCorrectly() = runTest {
        val repository = createRepository(this)

        val timestamp = 1729958400000L
        repository.setLastCatalogUpdateCheckTimestamp(timestamp)
        assertEquals(timestamp, repository.lastCatalogUpdateCheckTimestamp.first())
    }
}
