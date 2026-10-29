package com.example.sermonindex.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.sermonindex.data.local.dao.FavoritesDao
import com.example.sermonindex.data.local.entities.FavoriteSermonEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavoritesDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var favoritesDao: FavoritesDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        favoritesDao = database.favoritesDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndObserveFavorites() = runTest {
        val entity1 = FavoriteSermonEntity(
            id = "sermon-1",
            title = "The Crucified Life",
            speaker = "A.W. Tozer",
            speakerImage = "",
            duration = "45:00",
            addedAt = 1000L
        )
        val entity2 = FavoriteSermonEntity(
            id = "sermon-2",
            title = "Revival Praying",
            speaker = "Leonard Ravenhill",
            speakerImage = "",
            duration = "52:00",
            addedAt = 2000L
        )

        favoritesDao.getAllFavorites().test {
            // Initially empty
            assertEquals(emptyList<FavoriteSermonEntity>(), awaitItem())

            favoritesDao.insertFavorite(entity1)
            val afterOne = awaitItem()
            assertEquals(1, afterOne.size)
            assertEquals("sermon-1", afterOne[0].id)

            favoritesDao.insertFavorite(entity2)
            val afterTwo = awaitItem()
            assertEquals(2, afterTwo.size)
            // Ordered by added_at DESC
            assertEquals("sermon-2", afterTwo[0].id)
            assertEquals("sermon-1", afterTwo[1].id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isFavorite_emitsTrueThenFalseOnDelete() = runTest {
        val entity = FavoriteSermonEntity(
            id = "sermon-fav",
            title = "A Treasury of Prayer",
            speaker = "E.M. Bounds",
            speakerImage = "",
            duration = "30:00",
            addedAt = 1000L
        )

        favoritesDao.isFavorite("sermon-fav").test {
            assertFalse(awaitItem())

            favoritesDao.insertFavorite(entity)
            assertTrue(awaitItem())

            val deletedCount = favoritesDao.deleteFavorite("sermon-fav")
            assertEquals(1, deletedCount)
            assertFalse(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
