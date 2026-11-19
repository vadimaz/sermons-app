package com.example.sermonindex.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.sermonindex.data.local.dao.DownloadsDao
import com.example.sermonindex.data.local.entities.DownloadedSermonEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DownloadsDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var downloadsDao: DownloadsDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        downloadsDao = database.downloadsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndObserveDownloads() = runTest {
        val download = DownloadedSermonEntity(
            id = "sermon-dl-1",
            title = "Ten Shekels and a Shirt",
            speaker = "Paris Reidhead",
            speakerImage = "",
            duration = "50:00",
            localPath = "/data/user/0/audio_downloads/sermon-dl-1.mp3",
            fileSizeBytes = 20_000_000L,
            downloadedAt = 1000L
        )

        downloadsDao.isDownloaded("sermon-dl-1").test {
            assertFalse(awaitItem())

            downloadsDao.insertDownload(download)
            assertTrue(awaitItem())

            val item = downloadsDao.getDownload("sermon-dl-1")
            assertNotNull(item)
            assertEquals(20_000_000L, item?.fileSizeBytes)

            val totalBytes = downloadsDao.getTotalStorageBytes()
            assertEquals(20_000_000L, totalBytes)

            downloadsDao.deleteDownload("sermon-dl-1")
            assertFalse(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteAllDownloads_clearsAllAndResetsTotalBytes() = runTest {
        val dl1 = DownloadedSermonEntity(
            id = "dl-1",
            title = "Title 1",
            speaker = "Speaker 1",
            speakerImage = "",
            duration = "10:00",
            localPath = "/path/1.mp3",
            fileSizeBytes = 5_000_000L
        )
        val dl2 = DownloadedSermonEntity(
            id = "dl-2",
            title = "Title 2",
            speaker = "Speaker 2",
            speakerImage = "",
            duration = "20:00",
            localPath = "/path/2.mp3",
            fileSizeBytes = 10_000_000L
        )

        downloadsDao.insertDownload(dl1)
        downloadsDao.insertDownload(dl2)

        assertEquals(15_000_000L, downloadsDao.getTotalStorageBytes())

        val deletedCount = downloadsDao.deleteAllDownloads()
        assertEquals(2, deletedCount)
        assertEquals(0L, downloadsDao.getTotalStorageBytes())
        assertNull(downloadsDao.getDownload("dl-1"))
        assertNull(downloadsDao.getDownload("dl-2"))
    }
}
