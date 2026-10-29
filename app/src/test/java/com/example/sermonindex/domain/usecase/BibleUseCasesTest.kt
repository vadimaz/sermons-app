package com.example.sermonindex.domain.usecase

import com.example.sermonindex.domain.model.BibleBook
import com.example.sermonindex.domain.model.BibleChapter
import com.example.sermonindex.domain.model.BibleChapterItem
import com.example.sermonindex.domain.model.BibleReadingPreferences
import com.example.sermonindex.testing.FakeSermonRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BibleUseCasesTest {

    private val fakeChapter = BibleChapter(
        translationId = "BSB",
        bookId = "JHN",
        bookName = "John",
        chapterNumber = 3,
        numberOfVerses = 36,
        items = listOf(
            BibleChapterItem.Heading("Jesus and Nicodemus"),
            BibleChapterItem.Verse(number = 1, text = "Now there was a man of the Pharisees...", footnoteIds = emptyList()),
            BibleChapterItem.Verse(number = 16, text = "For God so loved the world...", footnoteIds = listOf(17))
        ),
        footnotes = emptyList(),
        audioLinks = mapOf("gilbert" to "https://example.com/audio.mp3"),
        hasNextChapter = true,
        hasPreviousChapter = true
    )

    private val fakeBooks = listOf(
        BibleBook(id = "GEN", name = "Бытие", testament = "OT", chapterCount = 50, order = 1),
        BibleBook(id = "JHN", name = "Евангелие от Иоанна", testament = "NT", chapterCount = 21, order = 43)
    )

    private lateinit var fakeRepository: FakeSermonRepository

    @Before
    fun setUp() {
        fakeRepository = FakeSermonRepository().apply {
            scriptureBooksResult = Result.success(fakeBooks)
            bibleBooksForTranslationResult = Result.success(fakeBooks)
            bibleChapterResult = Result.success(fakeChapter)
            bibleReadingPreferencesFlow.value = BibleReadingPreferences(
                translationId = "BSB",
                bookId = "JHN",
                chapter = 3
            )
        }
    }

    @Test
    fun getBibleChapterUseCase_returnsSuccess() = runTest {
        val useCase = GetBibleChapterUseCase(fakeRepository)
        val result = useCase("BSB", "JHN", 3)
        assertTrue(result.isSuccess)
        val chapter = result.getOrNull()
        assertEquals("John", chapter?.bookName)
        assertEquals(3, chapter?.chapterNumber)
        assertEquals(3, chapter?.items?.size)
        assertEquals(mapOf("gilbert" to "https://example.com/audio.mp3"), chapter?.audioLinks)
    }

    @Test
    fun getBibleBooksForTranslationUseCase_returnsBooks() = runTest {
        val useCase = GetBibleBooksForTranslationUseCase(fakeRepository)
        val result = useCase("rus_syn")
        assertTrue(result.isSuccess)
        val books = result.getOrNull()
        assertEquals(2, books?.size)
        assertEquals("GEN", books?.first()?.id)
        assertEquals("Бытие", books?.first()?.name)
    }

    @Test
    fun getBibleReadingPreferencesUseCase_emitsPreferences() = runTest {
        val useCase = GetBibleReadingPreferencesUseCase(fakeRepository)
        val prefs = useCase().first()
        assertEquals("BSB", prefs.translationId)
        assertEquals("JHN", prefs.bookId)
        assertEquals(3, prefs.chapter)
    }

    @Test
    fun saveBibleReadingPositionUseCase_delegatesToRepository() = runTest {
        val useCase = SaveBibleReadingPositionUseCase(fakeRepository)
        useCase("rus_syn", "ROM", 8, 1)
        assertEquals(Triple("rus_syn", "ROM", 8), fakeRepository.savedReadingPosition)
    }

    @Test
    fun saveBibleTypographyPreferencesUseCase_delegatesToRepository() = runTest {
        val useCase = SaveBibleTypographyPreferencesUseCase(fakeRepository)
        useCase(20f, false, 1.8f)
        assertEquals(Triple(20f, false, 1.8f), fakeRepository.savedTypography)
    }

    @Test
    fun saveBibleNarratorUseCase_delegatesToRepository() = runTest {
        val useCase = SaveBibleNarratorUseCase(fakeRepository)
        useCase("hays")
        assertEquals("hays", fakeRepository.savedNarrator)
    }

    @Test
    fun defaultBibleReadingPreferences_startsAtGenesis() {
        val defaultPrefs = BibleReadingPreferences()
        assertEquals("GEN", defaultPrefs.bookId)
        assertEquals(1, defaultPrefs.chapter)
        assertEquals(1, defaultPrefs.verse)
    }
}
