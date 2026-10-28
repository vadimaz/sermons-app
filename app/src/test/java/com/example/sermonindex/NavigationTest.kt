package com.example.sermonindex

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun navigatePushesPlayerRouteFromSermonDetailAndPopsCleanly() {
        val topLevelState = mutableStateOf<NavKey>(HomeRoute)
        val homeStack = NavBackStack<NavKey>(HomeRoute).apply {
            add(SermonDetailRoute("sermon-42"))
        }
        val navState = NavigationState(
            startRoute = HomeRoute,
            topLevelRoute = topLevelState,
            backStacks = mapOf(HomeRoute to homeStack)
        )
        val navigator = Navigator(navState)

        // Act: User taps Play from Sermon Details
        navigator.navigate(PlayerRoute)

        // Assert: Player is current route, stack size is 3 (HomeRoute, SermonDetailRoute, PlayerRoute)
        assertEquals(3, homeStack.size)
        assertEquals(PlayerRoute, navState.currentRoute)

        // Act: User exits Player screen
        navigator.goBack()

        // Assert: Restored to Sermon Details
        assertEquals(2, homeStack.size)
        assertEquals(SermonDetailRoute("sermon-42"), navState.currentRoute)
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
    fun goBackReturnsToStartRouteFromSecondaryTabRoot() {
        val topLevelState = mutableStateOf<NavKey>(ScriptureRoute)
        val homeStack = NavBackStack<NavKey>(HomeRoute)
        val searchStack = NavBackStack<NavKey>(SearchRoute)
        val bibleStack = NavBackStack<NavKey>(BibleRoute)
        val scriptureStack = NavBackStack<NavKey>(ScriptureRoute)
        val libraryStack = NavBackStack<NavKey>(LibraryRoute)

        val navState = NavigationState(
            startRoute = HomeRoute,
            topLevelRoute = topLevelState,
            backStacks = mapOf(
                HomeRoute to homeStack,
                SearchRoute to searchStack,
                BibleRoute to bibleStack,
                ScriptureRoute to scriptureStack,
                LibraryRoute to libraryStack
            )
        )
        val navigator = Navigator(navState)

        // From ScriptureRoute root -> returns to HomeRoute ("exit through home")
        navigator.goBack()
        assertEquals(HomeRoute, navState.topLevelRoute)
        assertEquals(1, scriptureStack.size)

        // From SearchRoute root -> returns to HomeRoute
        navigator.navigate(SearchRoute)
        assertEquals(SearchRoute, navState.topLevelRoute)
        navigator.goBack()
        assertEquals(HomeRoute, navState.topLevelRoute)

        // At HomeRoute root -> remains at HomeRoute with size 1
        navigator.goBack()
        assertEquals(HomeRoute, navState.topLevelRoute)
        assertEquals(1, homeStack.size)
    }

    @Test
    fun getTopLevelRoutesInUseIncludesStartRouteUnderSecondaryTab() {
        val topLevelState = mutableStateOf<NavKey>(HomeRoute)
        val homeStack = NavBackStack<NavKey>(HomeRoute)
        val bibleStack = NavBackStack<NavKey>(BibleRoute)

        val navState = NavigationState(
            startRoute = HomeRoute,
            topLevelRoute = topLevelState,
            backStacks = mapOf(
                HomeRoute to homeStack,
                BibleRoute to bibleStack
            )
        )
        val navigator = Navigator(navState)

        assertEquals(listOf<NavKey>(HomeRoute), navState.getTopLevelRoutesInUse())

        navigator.navigate(BibleRoute)
        assertEquals(listOf<NavKey>(HomeRoute, BibleRoute), navState.getTopLevelRoutesInUse())

        navigator.goBack()
        assertEquals(listOf<NavKey>(HomeRoute), navState.getTopLevelRoutesInUse())
    }

    @Test
    fun isTopLevelTransitionReturnsTrueBetweenAllTopLevelRoutesAndContentKeys() {
        val topLevelRoutes = listOf(
            HomeRoute,
            SearchRoute,
            BibleRoute,
            ScriptureRoute,
            LibraryRoute
        )

        for (initial in topLevelRoutes) {
            for (target in topLevelRoutes) {
                assertTrue(isTopLevelTransition(initial, target))
                assertTrue(isTopLevelTransition(initial.toString(), target.toString()))
                assertTrue(isTopLevelTransition(initial, target.toString()))
            }
        }
    }

    @Test
    fun isTopLevelTransitionReturnsFalseForHierarchicalAndDetailTransitions() {
        assertFalse(isTopLevelTransition(HomeRoute, SermonDetailRoute("123")))
        assertFalse(isTopLevelTransition(SermonDetailRoute("123").toString(), HomeRoute.toString()))
        assertFalse(isTopLevelTransition(BibleRoute.toString(), BibleVerseSermonsRoute("JHN", "John", 3, 16).toString()))
        assertFalse(isTopLevelTransition(SearchRoute, SpeakerDetailRoute("spurgeon")))
        assertFalse(isTopLevelTransition(SpeakersRoute, SpeakerDetailRoute("spurgeon")))
        assertFalse(isTopLevelTransition(SermonDetailRoute("123"), PlayerRoute))
        assertFalse(isTopLevelTransition(null, HomeRoute))
        assertFalse(isTopLevelTransition(HomeRoute, null))
    }
}
