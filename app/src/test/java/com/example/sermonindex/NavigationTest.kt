package com.example.sermonindex

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationTest {

    @Test
    fun onReselectEmitsEventWithoutModifyingStack() = runTest {
        val topLevelState = mutableStateOf<NavKey>(HomeRoute)
        val homeStack = NavBackStack<NavKey>(HomeRoute).apply {
            add(SpeakersRoute)
            add(SpeakerDetailRoute("test-speaker"))
        }
        val scriptureStack = NavBackStack<NavKey>(ScriptureRoute)
        val libraryStack = NavBackStack<NavKey>(LibraryRoute)

        val navState = NavigationState(
            startRoute = HomeRoute,
            topLevelRoute = topLevelState,
            backStacks = mapOf(
                HomeRoute to homeStack,
                ScriptureRoute to scriptureStack,
                LibraryRoute to libraryStack
            )
        )
        val navigator = Navigator(navState)

        val emittedEvents = mutableListOf<NavKey>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            navigator.reselectEvents.collect { emittedEvents.add(it) }
        }

        navigator.onReselect(HomeRoute)

        assertEquals(listOf<NavKey>(HomeRoute), emittedEvents)
        // Verify stack was NOT truncated or popped
        assertEquals(3, homeStack.size)
        assertEquals(SpeakerDetailRoute("test-speaker"), homeStack.last())

        job.cancel()
    }

    @Test
    fun navigateSwitchesTopLevelRoute() {
        val topLevelState = mutableStateOf<NavKey>(HomeRoute)
        val homeStack = NavBackStack<NavKey>(HomeRoute)
        val searchStack = NavBackStack<NavKey>(SearchRoute)
        val scriptureStack = NavBackStack<NavKey>(ScriptureRoute)
        val libraryStack = NavBackStack<NavKey>(LibraryRoute)

        val navState = NavigationState(
            startRoute = HomeRoute,
            topLevelRoute = topLevelState,
            backStacks = mapOf(
                HomeRoute to homeStack,
                SearchRoute to searchStack,
                ScriptureRoute to scriptureStack,
                LibraryRoute to libraryStack
            )
        )
        val navigator = Navigator(navState)

        navigator.navigate(SearchRoute)
        assertEquals(SearchRoute, navState.topLevelRoute)

        navigator.navigate(ScriptureRoute)
        assertEquals(ScriptureRoute, navState.topLevelRoute)

        navigator.navigate(LibraryRoute)
        assertEquals(LibraryRoute, navState.topLevelRoute)
    }

    @Test
    fun navigatePushesSubRouteToActiveStack() {
        val topLevelState = mutableStateOf<NavKey>(HomeRoute)
        val homeStack = NavBackStack<NavKey>(HomeRoute)
        val scriptureStack = NavBackStack<NavKey>(ScriptureRoute)
        val libraryStack = NavBackStack<NavKey>(LibraryRoute)

        val navState = NavigationState(
            startRoute = HomeRoute,
            topLevelRoute = topLevelState,
            backStacks = mapOf(
                HomeRoute to homeStack,
                ScriptureRoute to scriptureStack,
                LibraryRoute to libraryStack
            )
        )
        val navigator = Navigator(navState)

        navigator.navigate(SermonDetailRoute("sermon-123"))

        assertEquals(2, homeStack.size)
        assertEquals(SermonDetailRoute("sermon-123"), homeStack.last())
        assertEquals(SermonDetailRoute("sermon-123"), navState.currentRoute)
    }

    @Test
    fun goBackPopsSubRoute() {
        val topLevelState = mutableStateOf<NavKey>(HomeRoute)
        val homeStack = NavBackStack<NavKey>(HomeRoute).apply {
            add(SermonDetailRoute("sermon-123"))
        }
        val scriptureStack = NavBackStack<NavKey>(ScriptureRoute)
        val libraryStack = NavBackStack<NavKey>(LibraryRoute)

        val navState = NavigationState(
            startRoute = HomeRoute,
            topLevelRoute = topLevelState,
            backStacks = mapOf(
                HomeRoute to homeStack,
                ScriptureRoute to scriptureStack,
                LibraryRoute to libraryStack
            )
        )
        val navigator = Navigator(navState)

        navigator.goBack()

        assertEquals(1, homeStack.size)
        assertEquals(HomeRoute, homeStack.last())
        assertEquals(HomeRoute, navState.currentRoute)
    }

    @Test
    fun goBackDoesNotChangeTopLevelRouteFromSecondaryTabRoot() {
        val topLevelState = mutableStateOf<NavKey>(ScriptureRoute)
        val homeStack = NavBackStack<NavKey>(HomeRoute)
        val scriptureStack = NavBackStack<NavKey>(ScriptureRoute)
        val libraryStack = NavBackStack<NavKey>(LibraryRoute)

        val navState = NavigationState(
            startRoute = HomeRoute,
            topLevelRoute = topLevelState,
            backStacks = mapOf(
                HomeRoute to homeStack,
                ScriptureRoute to scriptureStack,
                LibraryRoute to libraryStack
            )
        )
        val navigator = Navigator(navState)

        navigator.goBack()

        // At top-level route, navigator does not redirect back to HomeRoute;
        // top-level back press is handled to finish activity instead of bouncing to Home.
        assertEquals(ScriptureRoute, navState.topLevelRoute)
        assertEquals(1, scriptureStack.size)
    }
}
