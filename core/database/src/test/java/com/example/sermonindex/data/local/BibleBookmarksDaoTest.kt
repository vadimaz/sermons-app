package com.example.sermonindex.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.sermonindex.data.local.dao.BibleBookmarksDao
import com.example.sermonindex.data.local.entities.BibleBookmarkEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BibleBookmarksDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var bookmarksDao: BibleBookmarksDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        bookmarksDao = database.bibleBookmarksDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAndFilterBookmarksByChapter() = runTest {
        val bm1 = BibleBookmarkEntity(
            id = "BSB:GEN:1:1",
            translationId = "BSB",
            bookId = "GEN",
            bookName = "Genesis",
            chapter = 1,
            verse = 1,
            text = "In the beginning God created the heavens and the earth.",
            colorName = "YELLOW",
            createdAt = 1000L
        )
        val bm2 = BibleBookmarkEntity(
            id = "BSB:GEN:1:3",
            translationId = "BSB",
            bookId = "GEN",
            bookName = "Genesis",
            chapter = 1,
            verse = 3,
            text = "And God said, 'Let there be light,' and there was light.",
            colorName = "GREEN",
            createdAt = 2000L
        )
        val bmOtherChapter = BibleBookmarkEntity(
            id = "BSB:GEN:2:1",
            translationId = "BSB",
            bookId = "GEN",
            bookName = "Genesis",
            chapter = 2,
            verse = 1,
            text = "Thus the heavens and the earth were completed.",
            colorName = "BLUE",
            createdAt = 3000L
        )

        bookmarksDao.getBookmarksForChapter("BSB", "GEN", 1).test {
            assertEquals(emptyList<BibleBookmarkEntity>(), awaitItem())

            bookmarksDao.upsertBookmark(bm1)
            assertEquals(1, awaitItem().size)

            bookmarksDao.upsertBookmark(bm2)
            assertEquals(2, awaitItem().size)

            // Insert bookmark for chapter 2 - must not affect chapter 1 query
            bookmarksDao.upsertBookmark(bmOtherChapter)
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }

        // Verify getAllBookmarks contains all 3
        bookmarksDao.getAllBookmarks().test {
            val all = awaitItem()
            assertEquals(3, all.size)
            // Ordered by created_at DESC
            assertEquals("BSB:GEN:2:1", all[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteBookmarkForVerse_removesTargetOnly() = runTest {
        val bm = BibleBookmarkEntity(
            id = "BSB:JHN:3:16",
            translationId = "BSB",
            bookId = "JHN",
            bookName = "John",
            chapter = 3,
            verse = 16,
            text = "For God so loved the world...",
            colorName = "CORAL",
            createdAt = 1000L
        )
        bookmarksDao.upsertBookmark(bm)

        val deletedCount = bookmarksDao.deleteBookmarkForVerse("BSB", "JHN", 3, 16)
        assertEquals(1, deletedCount)

        bookmarksDao.getBookmarksForChapter("BSB", "JHN", 3).test {
            assertEquals(emptyList<BibleBookmarkEntity>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
