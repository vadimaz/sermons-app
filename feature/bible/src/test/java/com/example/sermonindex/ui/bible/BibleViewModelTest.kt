package com.example.sermonindex.ui.bible

import com.example.sermonindex.domain.model.HighlightColor
import com.example.sermonindex.domain.usecase.DeleteBibleBookmarkForVerseUseCase
import com.example.sermonindex.domain.usecase.DeleteBibleBookmarkUseCase
import com.example.sermonindex.domain.usecase.GetBibleBookmarksForChapterUseCase
import com.example.sermonindex.domain.usecase.GetBibleBooksForTranslationUseCase
import com.example.sermonindex.domain.usecase.GetBibleChapterUseCase
import com.example.sermonindex.domain.usecase.GetBibleReadingPreferencesUseCase
import com.example.sermonindex.domain.usecase.GetBibleTranslationsUseCase
import com.example.sermonindex.domain.usecase.SaveBibleBookmarkUseCase
import com.example.sermonindex.domain.usecase.SaveBibleNarratorUseCase
import com.example.sermonindex.domain.usecase.SaveBibleReadingPositionUseCase
import com.example.sermonindex.domain.usecase.SaveBibleTypographyPreferencesUseCase
import com.example.sermonindex.testing.FakePlaybackManager
import com.example.sermonindex.testing.FakeSermonRepository
import com.example.sermonindex.testing.MainDispatcherRule
import com.example.sermonindex.testing.createTestBibleBook
import com.example.sermonindex.testing.createTestBibleChapter
import com.example.sermonindex.testing.createTestBibleTranslation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BibleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var fakeRepository: FakeSermonRepository
    private lateinit var fakePlaybackManager: FakePlaybackManager
    private lateinit var viewModel: BibleViewModel

    private val testTranslation = createTestBibleTranslation(id = "BSB")
    private val genesisBook = createTestBibleBook(id = "GEN", name = "Genesis", chapterCount = 50)
    private val exodusBook = createTestBibleBook(id = "EXO", name = "Exodus", chapterCount = 40)
    private val testChapter = createTestBibleChapter(translationId = "BSB", bookId = "GEN", chapterNumber = 1)

    @Before
    fun setUp() {
        fakeRepository = FakeSermonRepository()
        fakePlaybackManager = FakePlaybackManager()

        fakeRepository.availableBibleTranslationsResult = Result.success(listOf(testTranslation))
        fakeRepository.bibleBooksForTranslationResult = Result.success(listOf(genesisBook, exodusBook))
        fakeRepository.bibleChapterResult = Result.success(testChapter)

        viewModel = BibleViewModel(
            getBibleChapterUseCase = GetBibleChapterUseCase(fakeRepository),
            getBibleTranslationsUseCase = GetBibleTranslationsUseCase(fakeRepository),
            getBibleBooksForTranslationUseCase = GetBibleBooksForTranslationUseCase(fakeRepository),
            getBibleReadingPreferencesUseCase = GetBibleReadingPreferencesUseCase(fakeRepository),
            saveBibleReadingPositionUseCase = SaveBibleReadingPositionUseCase(fakeRepository),
            saveBibleTypographyPreferencesUseCase = SaveBibleTypographyPreferencesUseCase(fakeRepository),
            saveBibleNarratorUseCase = SaveBibleNarratorUseCase(fakeRepository),
            getBibleBookmarksForChapterUseCase = GetBibleBookmarksForChapterUseCase(fakeRepository),
            saveBibleBookmarkUseCase = SaveBibleBookmarkUseCase(fakeRepository),
            deleteBibleBookmarkUseCase = DeleteBibleBookmarkUseCase(fakeRepository),
            deleteBibleBookmarkForVerseUseCase = DeleteBibleBookmarkForVerseUseCase(fakeRepository),
            playbackManager = fakePlaybackManager
        )
    }

    @Test
    fun initialLoad_loadsTranslationsBooksAndCurrentChapter() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(1, state.availableTranslations.size)
        assertEquals(2, state.books.size)
        assertNotNull(state.currentChapter)
        assertEquals("GEN", state.currentChapter?.bookId)
        assertEquals(1, state.currentChapter?.chapterNumber)
    }

    @Test
    fun selectBookAndChapter_updatesReadingPosition() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BibleUiEvent.SelectBookAndChapter(bookId = "EXO", chapter = 5))
        testDispatcher.scheduler.advanceUntilIdle()

        val prefs = fakeRepository.bibleReadingPreferencesFlow.value
        assertEquals("EXO", prefs.bookId)
        assertEquals(5, prefs.chapter)
        assertFalse(viewModel.uiState.value.isTableOfContentsOpen)
    }

    @Test
    fun typographyEvents_updatePreferences() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(BibleUiEvent.UpdateFontSize(24f))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(24f, fakeRepository.bibleReadingPreferencesFlow.value.fontSizeSp)

        viewModel.onEvent(BibleUiEvent.ToggleFontSerif(false))
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(fakeRepository.bibleReadingPreferencesFlow.value.isSerif)

        viewModel.onEvent(BibleUiEvent.UpdateLineSpacing(1.8f))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1.8f, fakeRepository.bibleReadingPreferencesFlow.value.lineSpacingMultiplier)
    }

    @Test
    fun verseSelectionAndBookmarkHighlight_savesBookmark() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        // Start selecting verse 1
        viewModel.onEvent(BibleUiEvent.StartVerseSelection(1))
        assertTrue(viewModel.uiState.value.isSelectionMode)
        assertEquals(1, viewModel.uiState.value.selectedVerse)

        // Apply yellow highlight
        viewModel.onEvent(BibleUiEvent.ApplyHighlight(HighlightColor.YELLOW))
        testDispatcher.scheduler.advanceUntilIdle()

        // Selection mode clears after applying highlight
        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertNull(viewModel.uiState.value.selectedVerse)

        // Verify bookmark was saved to repository
        assertTrue(fakeRepository.savedBookmarks.isNotEmpty())
        assertEquals(HighlightColor.YELLOW, fakeRepository.savedBookmarks.first().color)
        assertEquals(1, fakeRepository.savedBookmarks.first().verse)
    }

    @Test
    fun clearSelection_clearsSelectionState() {
        viewModel.onEvent(BibleUiEvent.StartVerseSelection(2))
        assertTrue(viewModel.uiState.value.isSelectionMode)

        viewModel.onEvent(BibleUiEvent.ClearSelection)
        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertNull(viewModel.uiState.value.selectedVerse)
    }

    @Test
    fun tableOfContentsAndSettings_toggleSheets() {
        viewModel.onEvent(BibleUiEvent.SetTableOfContentsOpen(true))
        assertTrue(viewModel.uiState.value.isTableOfContentsOpen)

        viewModel.onEvent(BibleUiEvent.SetTableOfContentsOpen(false))
        assertFalse(viewModel.uiState.value.isTableOfContentsOpen)

        viewModel.onEvent(BibleUiEvent.SetSettingsOpen(true))
        assertTrue(viewModel.uiState.value.isSettingsOpen)
    }
}
