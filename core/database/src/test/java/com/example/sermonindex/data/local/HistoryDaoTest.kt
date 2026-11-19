package com.example.sermonindex.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.sermonindex.data.local.dao.HistoryDao
import com.example.sermonindex.data.local.entities.HistoryEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var historyDao: HistoryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        historyDao = database.historyDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAndObserveHistory() = runTest {
        val history1 = HistoryEntity(
            sermonId = "sermon-1",
            title = "The Power of the Blood",
            speaker = "Andrew Murray",
            speakerImage = "https://example.com/murray.jpg",
            lastPositionMs = 15000L,
            durationMs = 60000L,
            updatedAt = 1000L,
            completed = false
        )
        val history2 = HistoryEntity(
            sermonId = "sermon-2",
            title = "Born After Midnight",
            speaker = "A.W. Tozer",
            speakerImage = "",
            lastPositionMs = 45000L,
            durationMs = 90000L,
            updatedAt = 2000L,
            completed = false
        )

        historyDao.getRecentHistory().test {
            assertEquals(emptyList<HistoryEntity>(), awaitItem())

            historyDao.upsertHistory(history1)
            val list1 = awaitItem()
            assertEquals(1, list1.size)
            assertEquals("sermon-1", list1[0].sermonId)

            historyDao.upsertHistory(history2)
            val list2 = awaitItem()
            assertEquals(2, list2.size)
            // Ordered by updated_at DESC
            assertEquals("sermon-2", list2[0].sermonId)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getRecentHistory_excludesBibleNarrationTracks() = runTest {
        val sermonHistory = HistoryEntity(
            sermonId = "sermon-regular",
            title = "Holiness",
            speaker = "J.C. Ryle",
            speakerImage = "",
            lastPositionMs = 1000L,
            durationMs = 5000L,
            updatedAt = 1000L,
            completed = false
        )
        val bibleHistory = HistoryEntity(
            sermonId = "bible_BSB_GEN_1",
            title = "Genesis 1 Narration",
            speaker = "Narrator",
            speakerImage = "",
            lastPositionMs = 500L,
            durationMs = 1000L,
            updatedAt = 2000L,
            completed = false
        )

        historyDao.upsertHistory(sermonHistory)
        historyDao.upsertHistory(bibleHistory)

        historyDao.getRecentHistory().test {
            val list = awaitItem()
            // bible_% must be strictly excluded from recent history query
            assertEquals(1, list.size)
            assertEquals("sermon-regular", list[0].sermonId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getHistoryById_andClearHistory() = runTest {
        val entry = HistoryEntity(
            sermonId = "sermon-lookup",
            title = "Test",
            speaker = "Speaker",
            speakerImage = "",
            lastPositionMs = 100L,
            durationMs = 200L,
            updatedAt = 100L,
            completed = false
        )

        assertNull(historyDao.getHistory("sermon-lookup"))
        historyDao.upsertHistory(entry)

        val retrieved = historyDao.getHistory("sermon-lookup")
        assertNotNull(retrieved)
        assertEquals(100L, retrieved?.lastPositionMs)

        historyDao.clearHistory()
        assertNull(historyDao.getHistory("sermon-lookup"))
    }
}
