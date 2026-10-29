package com.example.sermonindex.ui.speakers

import com.example.sermonindex.domain.usecase.GetSpeakersUseCase
import com.example.sermonindex.testing.FakeSermonRepository
import com.example.sermonindex.testing.MainDispatcherRule
import com.example.sermonindex.testing.createTestSpeaker
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
class SpeakersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var fakeRepository: FakeSermonRepository
    private lateinit var viewModel: SpeakersViewModel

    private val speaker1 = createTestSpeaker(id = "spk-1", name = "A.W. Tozer", slug = "tozer")
    private val speaker2 = createTestSpeaker(id = "spk-2", name = "Leonard Ravenhill", slug = "ravenhill")
    private val speaker3 = createTestSpeaker(id = "spk-3", name = "Charles Spurgeon", slug = "spurgeon")

    @Before
    fun setUp() {
        fakeRepository = FakeSermonRepository()
        fakeRepository.setSpeakers(listOf(speaker1, speaker2, speaker3))

        viewModel = SpeakersViewModel(
            getSpeakersUseCase = GetSpeakersUseCase(fakeRepository),
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun initialLoad_loadsAllSpeakers() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(3, state.speakers.size)
        assertEquals(3, state.filteredSpeakers.size)
    }

    @Test
    fun updateSearchQuery_filtersSpeakersCaseInsensitive() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SpeakersUiEvent.UpdateSearchQuery("toz"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("toz", state.searchQuery)
        assertEquals(1, state.filteredSpeakers.size)
        assertEquals("A.W. Tozer", state.filteredSpeakers[0].name)

        viewModel.onEvent(SpeakersUiEvent.UpdateSearchQuery(""))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.filteredSpeakers.size)
    }

    @Test
    fun networkFailure_setsErrorMessage_andRetryReloads() = runTest {
        fakeRepository.speakersResult = Result.failure(RuntimeException("Network error"))

        val errorVm = SpeakersViewModel(
            getSpeakersUseCase = GetSpeakersUseCase(fakeRepository),
            ioDispatcher = testDispatcher
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(errorVm.uiState.value.errorMessage)
        assertFalse(errorVm.uiState.value.isLoading)

        // Reset to success and retry
        fakeRepository.speakersResult = Result.success(listOf(speaker1))
        errorVm.onEvent(SpeakersUiEvent.Retry)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(errorVm.uiState.value.errorMessage)
        assertEquals(1, errorVm.uiState.value.speakers.size)
    }
}
