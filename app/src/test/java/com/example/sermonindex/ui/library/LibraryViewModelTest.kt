package com.example.sermonindex.ui.library

import app.cash.turbine.test
import com.example.sermonindex.domain.usecase.ClearHistoryUseCase
import com.example.sermonindex.domain.usecase.DeleteAllDownloadsUseCase
import com.example.sermonindex.domain.usecase.DeleteBibleBookmarkUseCase
import com.example.sermonindex.domain.usecase.DeleteDownloadUseCase
import com.example.sermonindex.domain.usecase.GetAllBibleBookmarksUseCase
import com.example.sermonindex.domain.usecase.GetDownloadsUseCase
import com.example.sermonindex.domain.usecase.GetFavoritesUseCase
import com.example.sermonindex.domain.usecase.GetHistoryUseCase
import com.example.sermonindex.domain.usecase.GetSermonDetailsUseCase
import com.example.sermonindex.domain.usecase.SaveBibleReadingPositionUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import com.example.sermonindex.testing.FakePlaybackManager
import com.example.sermonindex.testing.FakeSermonRepository
import com.example.sermonindex.testing.MainDispatcherRule
import com.example.sermonindex.testing.createTestBibleBookmark
import com.example.sermonindex.testing.createTestDownload
import com.example.sermonindex.testing.createTestFavorite
import com.example.sermonindex.testing.createTestHistory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var fakeRepository: FakeSermonRepository
    private lateinit var fakePlaybackManager: FakePlaybackManager
    private lateinit var viewModel: LibraryViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeSermonRepository()
        fakePlaybackManager = FakePlaybackManager()

        viewModel = LibraryViewModel(
            getFavoritesUseCase = GetFavoritesUseCase(fakeRepository),
            getDownloadsUseCase = GetDownloadsUseCase(fakeRepository),
            deleteDownloadUseCase = DeleteDownloadUseCase(fakeRepository),
            deleteAllDownloadsUseCase = DeleteAllDownloadsUseCase(fakeRepository),
            getHistoryUseCase = GetHistoryUseCase(fakeRepository),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(fakeRepository),
            getSermonDetailsUseCase = GetSermonDetailsUseCase(fakeRepository),
            getAllBibleBookmarksUseCase = GetAllBibleBookmarksUseCase(fakeRepository),
            deleteBibleBookmarkUseCase = DeleteBibleBookmarkUseCase(fakeRepository),
            saveBibleReadingPositionUseCase = SaveBibleReadingPositionUseCase(fakeRepository),
            clearHistoryUseCase = ClearHistoryUseCase(fakeRepository),
            playbackManager = fakePlaybackManager
        )
    }

    @Test
    fun selectTab_updatesState() {
        assertEquals(0, viewModel.uiState.value.selectedTab)

        viewModel.onEvent(LibraryUiEvent.SelectTab(2))
        assertEquals(2, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun downloadsCollection_calculatesTotalFormattedSize() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val download = createTestDownload(id = "dl-1", fileSizeBytes = 25 * 1024 * 1024L) // 25 MB
        fakeRepository.downloadsFlow.value = listOf(download)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.downloads.size)
            assertTrue(state.totalDownloadsSizeFormatted.contains("25.0 MB"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteAllDownloads_clearsDownloadsInRepository() = runTest {
        fakeRepository.downloadsFlow.value = listOf(createTestDownload(id = "dl-1"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(LibraryUiEvent.DeleteAllDownloads)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeRepository.downloadsFlow.value.isEmpty())
        assertTrue(fakeRepository.deleteAllDownloadsCalled)
    }

    @Test
    fun clearHistory_clearsHistoryInRepository() = runTest {
        fakeRepository.historyFlow.value = listOf(createTestHistory(sermonId = "hist-1"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(LibraryUiEvent.ClearHistory)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeRepository.historyFlow.value.isEmpty())
        assertTrue(fakeRepository.clearHistoryCalled)
    }

    @Test
    fun deleteBookmark_removesBookmarkFromRepository() = runTest {
        val bookmark = createTestBibleBookmark(id = "bm-1")
        fakeRepository.bibleBookmarksFlow.value = listOf(bookmark)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(LibraryUiEvent.DeleteBookmark("bm-1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeRepository.bibleBookmarksFlow.value.isEmpty())
        assertTrue(fakeRepository.deletedBookmarkIds.contains("bm-1"))
    }

    @Test
    fun openBookmarkInBible_savesReadingPosition() = runTest {
        val bookmark = createTestBibleBookmark(
            id = "bm-1",
            translationId = "BSB",
            bookId = "ROM",
            chapter = 8,
            verse = 28
        )

        viewModel.onEvent(LibraryUiEvent.OpenBookmarkInBible(bookmark))
        testDispatcher.scheduler.advanceUntilIdle()

        val prefs = fakeRepository.bibleReadingPreferencesFlow.value
        assertEquals("BSB", prefs.translationId)
        assertEquals("ROM", prefs.bookId)
        assertEquals(8, prefs.chapter)
        assertEquals(28, prefs.verse)
    }

    @Test
    fun recentItems_mapsDirectlyFromHistory() = runTest {
        val historyItem1 = createTestHistory(sermonId = "hist-1", title = "History Sermon 1")
        val historyItem2 = createTestHistory(sermonId = "hist-2", title = "History Sermon 2")
        val downloadItem = createTestDownload(id = "hist-1", title = "Downloaded History Sermon")

        fakeRepository.historyFlow.value = listOf(historyItem1, historyItem2)
        fakeRepository.downloadsFlow.value = listOf(downloadItem)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.recentItems.size)
            assertEquals("hist-1", state.recentItems[0].id)
            assertTrue(state.recentItems[0].isDownloaded)
            assertEquals("hist-2", state.recentItems[1].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun playRecent_playsDownloadedOrStreamedSermon() = runTest {
        val downloadItem = createTestDownload(id = "dl-1", title = "Downloaded Sermon")
        fakeRepository.historyFlow.value = listOf(createTestHistory(sermonId = "dl-1", title = "Downloaded Sermon"))
        fakeRepository.downloadsFlow.value = listOf(downloadItem)
        testDispatcher.scheduler.advanceUntilIdle()

        val recentItem = viewModel.uiState.value.recentItems.first()
        viewModel.onEvent(LibraryUiEvent.PlayRecent(recentItem))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("dl-1", fakePlaybackManager.lastPlayedSermon?.id)
    }
}
