package com.example.sermonindex.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Reusable JUnit4 [TestWatcher] rule that overrides [Dispatchers.Main] with a [TestDispatcher]
 * for ViewModel and coroutine tests.
 *
 * Defaults to [UnconfinedTestDispatcher] for immediate execution, or accepts a
 * [kotlinx.coroutines.test.StandardTestDispatcher] when explicit virtual-time control
 * (`advanceTimeBy`, `advanceUntilIdle`) is needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
