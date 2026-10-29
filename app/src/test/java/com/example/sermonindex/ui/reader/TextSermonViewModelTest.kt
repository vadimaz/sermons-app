package com.example.sermonindex.ui.reader

import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.domain.usecase.GetSermonDetailsUseCase
import com.example.sermonindex.domain.usecase.IsFavoriteUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import com.example.sermonindex.testing.FakeSermonRepository
import com.example.sermonindex.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TextSermonViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val fakeDetail = SermonDetail(
        id = "spurgeon-1",
        title = "Faith and Life",
        speaker = "Charles Spurgeon",
        transcript = "Faith is the victory that overcomes the world.\n\nWithout faith it is impossible to please God.\n\nLiving faith produces holy works."
    )

    private val fakeRepo = FakeSermonRepository().apply {
        sermonDetailsProvider = { Result.success(fakeDetail) }
    }

    @Test
    fun loadDetails_splitsParagraphsAndPreservesHighlightQuery() = runTest(testDispatcher) {
        val viewModel = TextSermonViewModel(
            sermonId = "spurgeon-1",
            initialHighlightQuery = "faith",
            getSermonDetailsUseCase = GetSermonDetailsUseCase(fakeRepo),
            isFavoriteUseCase = IsFavoriteUseCase(fakeRepo),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(fakeRepo)
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals(3, state.paragraphs.size)
        assertEquals("faith", state.highlightQuery)
        assertEquals("Faith is the victory that overcomes the world.", state.paragraphs[0])
    }
}
