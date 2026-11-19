package com.example.sermonindex.domain.usecase

import app.cash.turbine.test
import com.example.sermonindex.domain.model.BibleBookmark
import com.example.sermonindex.domain.model.HighlightColor
import com.example.sermonindex.testing.FakeSermonRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BibleBookmarkUseCasesTest {

    private val sampleBookmark = BibleBookmark(
        id = "rus_syn:JHN:3:16",
        translationId = "rus_syn",
        bookId = "JHN",
        bookName = "Иоанна",
        chapter = 3,
        verse = 16,
        text = "Ибо так возлюбил Бог мир...",
        color = HighlightColor.YELLOW,
        createdAt = 1700000000000L
    )

    private lateinit var fakeRepository: FakeSermonRepository

    @Before
    fun setUp() {
        fakeRepository = FakeSermonRepository().apply {
            bibleBookmarksFlow.value = listOf(sampleBookmark)
        }
    }

    @Test
    fun getAllBibleBookmarksUseCase_returnsBookmarksFlow() = runTest {
        val useCase = GetAllBibleBookmarksUseCase(fakeRepository)
        useCase().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("rus_syn:JHN:3:16", result.first().id)
            assertEquals(16, result.first().verse)
            assertEquals(HighlightColor.YELLOW, result.first().color)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getBibleBookmarksForChapterUseCase_filtersByChapter() = runTest {
        val useCase = GetBibleBookmarksForChapterUseCase(fakeRepository)
        val matching = useCase("rus_syn", "JHN", 3).first()
        assertEquals(1, matching.size)

        val nonMatching = useCase("rus_syn", "JHN", 4).first()
        assertTrue(nonMatching.isEmpty())
    }

    @Test
    fun saveBibleBookmarkUseCase_delegatesToRepository() = runTest {
        val useCase = SaveBibleBookmarkUseCase(fakeRepository)
        val newBookmark = sampleBookmark.copy(
            id = "rus_syn:ROM:8:28",
            bookId = "ROM",
            chapter = 8,
            verse = 28,
            color = HighlightColor.GREEN
        )
        useCase(newBookmark)
        assertEquals(1, fakeRepository.savedBookmarks.size)
        assertEquals("rus_syn:ROM:8:28", fakeRepository.savedBookmarks.first().id)
    }

    @Test
    fun deleteBibleBookmarkUseCase_delegatesToRepository() = runTest {
        val useCase = DeleteBibleBookmarkUseCase(fakeRepository)
        useCase("rus_syn:JHN:3:16")
        assertEquals(listOf("rus_syn:JHN:3:16"), fakeRepository.deletedBookmarkIds)
    }

    @Test
    fun deleteBibleBookmarkForVerseUseCase_delegatesToRepository() = runTest {
        val useCase = DeleteBibleBookmarkForVerseUseCase(fakeRepository)
        useCase("rus_syn", "JHN", 3, 16)
        assertEquals(1, fakeRepository.deletedBookmarkVerses.size)
        assertEquals(Triple("rus_syn", "JHN", Pair(3, 16)), fakeRepository.deletedBookmarkVerses.first())
    }
}
