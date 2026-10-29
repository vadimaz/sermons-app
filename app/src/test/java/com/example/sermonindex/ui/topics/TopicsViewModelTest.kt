package com.example.sermonindex.ui.topics

import com.example.sermonindex.domain.usecase.GetTopicsUseCase
import com.example.sermonindex.testing.FakeSermonRepository
import com.example.sermonindex.testing.MainDispatcherRule
import com.example.sermonindex.testing.createTestTopic
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
class TopicsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var fakeRepository: FakeSermonRepository
    private lateinit var viewModel: TopicsViewModel

    private val topic1 = createTestTopic(id = "top-1", name = "Prayer", slug = "prayer")
    private val topic2 = createTestTopic(id = "top-2", name = "Repentance", slug = "repentance")
    private val topic3 = createTestTopic(id = "top-3", name = "Holy Spirit", slug = "holy-spirit")

    @Before
    fun setUp() {
        fakeRepository = FakeSermonRepository()
        fakeRepository.setTopics(listOf(topic1, topic2, topic3))

        viewModel = TopicsViewModel(
            getTopicsUseCase = GetTopicsUseCase(fakeRepository),
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun initialLoad_loadsAllTopics() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(3, state.topics.size)
        assertEquals(3, state.filteredTopics.size)
    }

    @Test
    fun updateSearchQuery_filtersTopicsCaseInsensitive() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(TopicsUiEvent.UpdateSearchQuery("pray"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("pray", state.searchQuery)
        assertEquals(1, state.filteredTopics.size)
        assertEquals("Prayer", state.filteredTopics[0].name)

        viewModel.onEvent(TopicsUiEvent.UpdateSearchQuery(""))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.filteredTopics.size)
    }

    @Test
    fun networkFailure_setsErrorMessage_andRetryReloads() = runTest {
        fakeRepository.topicsResult = Result.failure(RuntimeException("Network error"))

        val errorVm = TopicsViewModel(
            getTopicsUseCase = GetTopicsUseCase(fakeRepository),
            ioDispatcher = testDispatcher
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(errorVm.uiState.value.errorMessage)
        assertFalse(errorVm.uiState.value.isLoading)

        fakeRepository.topicsResult = Result.success(listOf(topic1))
        errorVm.onEvent(TopicsUiEvent.Retry)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(errorVm.uiState.value.errorMessage)
        assertEquals(1, errorVm.uiState.value.topics.size)
    }
}
