package com.example.sermonindex.ui.scripture

import com.example.sermonindex.domain.model.ChapterDetails
import com.example.sermonindex.domain.usecase.GetScriptureBooksUseCase
import com.example.sermonindex.domain.usecase.GetScriptureChapterUseCase
import com.example.sermonindex.testing.FakeSermonRepository
import com.example.sermonindex.testing.MainDispatcherRule
import com.example.sermonindex.testing.createTestBibleBook
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScriptureViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var fakeRepository: FakeSermonRepository
    private lateinit var viewModel: ScriptureViewModel

    private val otBook = createTestBibleBook(id = "GEN", name = "Genesis", testament = "OT", chapterCount = 50)
    private val ntBook = createTestBibleBook(id = "MAT", name = "Matthew", testament = "NT", chapterCount = 28)

    @Before
    fun setUp() {
        fakeRepository = FakeSermonRepository()
        fakeRepository.scriptureBooksResult = Result.success(listOf(otBook, ntBook))

        viewModel = ScriptureViewModel(
            getScriptureBooksUseCase = GetScriptureBooksUseCase(fakeRepository)
        )
    }

    @Test
    fun initialLoad_loadsBooks() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingBooks)
        assertNull(state.errorMessage)
        assertEquals(2, state.books.size)
        assertEquals(2, state.filteredBooks.size)
    }

    @Test
    fun testamentTabSelection_filtersBooks() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        // Tab 1: OT only
        viewModel.onEvent(ScriptureUiEvent.SelectTestamentTab(1))
        assertEquals(1, viewModel.uiState.value.testamentTabIndex)
        assertEquals(1, viewModel.uiState.value.filteredBooks.size)
        assertEquals("GEN", viewModel.uiState.value.filteredBooks[0].id)

        // Tab 2: NT only
        viewModel.onEvent(ScriptureUiEvent.SelectTestamentTab(2))
        assertEquals(2, viewModel.uiState.value.testamentTabIndex)
        assertEquals(1, viewModel.uiState.value.filteredBooks.size)
        assertEquals("MAT", viewModel.uiState.value.filteredBooks[0].id)

        // Tab 0: All
        viewModel.onEvent(ScriptureUiEvent.SelectTestamentTab(0))
        assertEquals(2, viewModel.uiState.value.filteredBooks.size)
    }

    @Test
    fun chapterViewModel_loadsChapterDetailsAndHeatmap() = runTest {
        val chapterDetails = ChapterDetails(
            bookId = "GEN",
            bookName = "Genesis",
            chapter = 1,
            verseCount = 31,
            verseHeatMap = mapOf(1 to 12, 2 to 5)
        )
        fakeRepository.scriptureChapterResult = Result.success(chapterDetails)

        val chapterVm = ScriptureChapterViewModel(
            args = ScriptureChapterArgs(bookId = "GEN", bookName = "Genesis", chapter = 1),
            getScriptureChapterUseCase = GetScriptureChapterUseCase(fakeRepository)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = chapterVm.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.chapterData)
        assertEquals(31, state.chapterData?.verseCount)
        assertEquals(12, state.chapterData?.verseHeatMap?.get(1))
    }
}
