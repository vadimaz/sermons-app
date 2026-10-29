package com.example.sermonindex.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.sermonindex.data.local.dao.SearchDao
import com.example.sermonindex.data.local.entities.CatalogMetadataEntity
import com.example.sermonindex.data.local.entities.SearchHistoryEntity
import com.example.sermonindex.data.local.entities.SermonFtsEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchDaoAndFtsTest {

    private lateinit var database: AppDatabase
    private lateinit var searchDao: SearchDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        searchDao = database.searchDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertFtsBatch_andPrefixSearch() = runTest {
        val records = listOf(
            SermonFtsEntity(
                rowid = 1,
                sermonId = "sermon-1",
                title = "The Crucified Life",
                speaker = "A.W. Tozer",
                summary = "Living a life centered on Christ",
                duration = "45:00",
                speakerImage = "",
                hasAudio = 1,
                mp3Url = "https://example.com/1.mp3",
                cdnMp3Url = ""
            ),
            SermonFtsEntity(
                rowid = 2,
                sermonId = "sermon-2",
                title = "Revival Praying",
                speaker = "Leonard Ravenhill",
                summary = "An urgent call to prayer and repentance",
                duration = "52:00",
                speakerImage = "",
                hasAudio = 1,
                mp3Url = "https://example.com/2.mp3",
                cdnMp3Url = ""
            ),
            SermonFtsEntity(
                rowid = 3,
                sermonId = "sermon-3",
                title = "Возрождение и молитва",
                speaker = "Василий Марцинковский",
                summary = "Проповедь о духовном пробуждении",
                duration = "38:00",
                speakerImage = "",
                hasAudio = 1,
                mp3Url = "https://example.com/3.mp3",
                cdnMp3Url = ""
            )
        )

        searchDao.insertSermonsFtsBatch(records)
        assertEquals(3, searchDao.getFtsSermonCount())

        // Search by speaker prefix "toz*"
        val tozerResults = searchDao.searchSermonsFts("toz*", limit = 10)
        assertEquals(1, tozerResults.size)
        assertEquals("sermon-1", tozerResults[0].sermonId)

        // Search by keyword in summary "repentance*"
        val prayerResults = searchDao.searchSermonsFts("repentance*", limit = 10)
        assertEquals(1, prayerResults.size)
        assertEquals("sermon-2", prayerResults[0].sermonId)

        // Multilingual Cyrillic search via unicode61 tokenizer
        val cyrillicResults = searchDao.searchSermonsFts("Возрождение*", limit = 10)
        assertEquals(1, cyrillicResults.size)
        assertEquals("sermon-3", cyrillicResults[0].sermonId)

        // Clear FTS table
        searchDao.clearSermonsFts()
        assertEquals(0, searchDao.getFtsSermonCount())
    }

    @Test
    fun catalogMetadata_lifecycle() = runTest {
        assertNull(searchDao.getCatalogMetadata())

        val metadata = CatalogMetadataEntity(
            id = 1,
            sermonCount = 70419,
            generatedAt = "2026-09-21T02:29:31Z",
            downloadedAt = 5000L
        )

        searchDao.saveCatalogMetadata(metadata)

        val retrieved = searchDao.getCatalogMetadata()
        assertNotNull(retrieved)
        assertEquals(70419, retrieved?.sermonCount)
        assertEquals("2026-09-21T02:29:31Z", retrieved?.generatedAt)

        searchDao.clearCatalogMetadata()
        assertNull(searchDao.getCatalogMetadata())
    }

    @Test
    fun searchHistory_andDeduplication() = runTest {
        val query1 = SearchHistoryEntity(query = "Tozer", timestamp = 1000L)
        val query2 = SearchHistoryEntity(query = "Prayer", timestamp = 2000L)
        val queryUpdate = SearchHistoryEntity(query = "Tozer", timestamp = 3000L)

        searchDao.getRecentSearchQueries().test {
            assertEquals(emptyList<String>(), awaitItem())

            searchDao.insertSearchQuery(query1)
            assertEquals(listOf("Tozer"), awaitItem())

            searchDao.insertSearchQuery(query2)
            // Ordered by timestamp DESC
            assertEquals(listOf("Prayer", "Tozer"), awaitItem())

            // Upsert existing query with newer timestamp -> updates timestamp and moves to front
            searchDao.insertSearchQuery(queryUpdate)
            assertEquals(listOf("Tozer", "Prayer"), awaitItem())

            searchDao.deleteSearchQuery("Prayer")
            assertEquals(listOf("Tozer"), awaitItem())

            searchDao.clearSearchHistory()
            assertEquals(emptyList<String>(), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
