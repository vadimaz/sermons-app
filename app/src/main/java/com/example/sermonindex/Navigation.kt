package com.example.sermonindex

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.sermonindex.domain.repository.NetworkMonitor
import com.example.sermonindex.playback.PlaybackManager
import com.example.sermonindex.ui.bible.BibleScreen
import com.example.sermonindex.ui.bible.sermons.BibleVerseSermonsScreen
import com.example.sermonindex.ui.components.MiniPlayer
import com.example.sermonindex.ui.components.OfflineBanner
import com.example.sermonindex.ui.home.HomeScreen
import com.example.sermonindex.ui.library.LibraryScreen
import com.example.sermonindex.ui.player.FullscreenPlayerScreen
import com.example.sermonindex.ui.reader.TextSermonScreen
import com.example.sermonindex.ui.search.SearchScreen
import com.example.sermonindex.ui.scripture.ScriptureScreen
import com.example.sermonindex.ui.sermon.SermonDetailScreen
import com.example.sermonindex.ui.speakers.SpeakerDetailScreen
import com.example.sermonindex.ui.speakers.SpeakersScreen
import com.example.sermonindex.ui.topics.TopicDetailScreen
import com.example.sermonindex.ui.topics.TopicsScreen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

private val TOP_LEVEL_ROUTES = setOf<NavKey>(
    HomeRoute,
    SearchRoute,
    BibleRoute,
    ScriptureRoute,
    LibraryRoute
)

/**
 * Handles navigation events (forward, back, and reselect) across multiple back stacks.
 */
class Navigator(val state: NavigationState) {
    private val _reselectEvents = MutableSharedFlow<NavKey>(extraBufferCapacity = 1)
    val reselectEvents = _reselectEvents.asSharedFlow()

    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun onReselect(route: NavKey) {
        _reselectEvents.tryEmit(route)
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute] ?: return
        val currentRoute = currentStack.lastOrNull()
        if (currentRoute != state.topLevelRoute) {
            currentStack.removeLastOrNull()
        }
    }
}

/**
 * Navigation state holding independent back stacks per top level route,
 * with state preservation across tabs via SaveableStateHolder.
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>
) {
    var topLevelRoute: NavKey by topLevelRoute

    val currentRoute: NavKey?
        get() = backStacks[topLevelRoute]?.lastOrNull()

    @Composable
    fun toDecoratedEntries(
        entryProvider: (NavKey) -> NavEntry<NavKey>
    ): List<NavEntry<NavKey>> {
        val decoratedEntries = backStacks.mapValues { (_, stack) ->
            val decorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>()
            )
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = decorators,
                entryProvider = entryProvider
            )
        }

        return decoratedEntries[topLevelRoute] ?: emptyList()
    }
}

@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>
): NavigationState {
    val topLevelRoute = rememberSaveable(
        startRoute,
        saver = Saver(
            save = { it.value.toString() },
            restore = { savedString ->
                mutableStateOf(
                    topLevelRoutes.firstOrNull { it.toString() == savedString } ?: startRoute
                )
            }
        )
    ) {
        mutableStateOf(startRoute)
    }

    val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks
        )
    }
}

@Composable
fun MainNavigation(
    playbackManager: PlaybackManager,
    networkMonitor: NetworkMonitor
) {
    val navigationState = rememberNavigationState(
        startRoute = HomeRoute,
        topLevelRoutes = TOP_LEVEL_ROUTES
    )
    val navigator = remember { Navigator(navigationState) }

    val playbackState by playbackManager.playbackState.collectAsStateWithLifecycle()
    val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle(initialValue = true)

    val currentRoute = navigationState.currentRoute
    val isFullscreenPlayer = currentRoute is PlayerRoute || currentRoute is TextSermonRoute
    var isScriptureAtRoot by rememberSaveable { mutableStateOf(true) }
    var isBibleBottomBarVisible by rememberSaveable { mutableStateOf(true) }

    val isAtTopLevel = when (currentRoute) {
        HomeRoute -> true
        SearchRoute -> true
        BibleRoute -> true
        LibraryRoute -> true
        ScriptureRoute -> isScriptureAtRoot
        else -> false
    }
    val isBibleTrack = playbackState.currentSermon?.id?.startsWith("bible_") == true
    val isBibleTab = navigationState.topLevelRoute == BibleRoute
    val effectivelyShowBottomBar = if (isBibleTab) isBibleBottomBarVisible else true

    val showBottomBar = isAtTopLevel
    val showMiniPlayer = !isFullscreenPlayer && playbackState.hasActiveMedia && !isBibleTrack

    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val totalBottomBarHeight = 80.dp + navBarInset
    val animatedHeight by animateDpAsState(
        targetValue = if (effectivelyShowBottomBar) totalBottomBarHeight else 0.dp,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "bottomBarHeight"
    )
    val animatedTranslationY by animateDpAsState(
        targetValue = if (effectivelyShowBottomBar) 0.dp else totalBottomBarHeight,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "bottomBarTranslationY"
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar || showMiniPlayer) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (!showBottomBar) Modifier.navigationBarsPadding() else Modifier)
                ) {
                    // Persistent MiniPlayer floating above bottom navigation or anchored above system nav bar
                    if (showMiniPlayer) {
                        MiniPlayer(
                            playbackState = playbackState,
                            onTogglePlayPause = { playbackManager.togglePlayPause() },
                            onExpandClick = { navigator.navigate(PlayerRoute) },
                            onCloseClick = { playbackManager.stop() }
                        )
                    }

                    // Bottom Navigation Bar
                    if (showBottomBar) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(animatedHeight)
                                .clipToBounds()
                        ) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(totalBottomBarHeight)
                                    .graphicsLayer {
                                        translationY = animatedTranslationY.toPx()
                                    }
                            ) {
                            val isHome = navigationState.topLevelRoute == HomeRoute
                            val isSearch = navigationState.topLevelRoute == SearchRoute
                            val isBible = navigationState.topLevelRoute == BibleRoute
                            val isScripture = navigationState.topLevelRoute == ScriptureRoute
                            val isLibrary = navigationState.topLevelRoute == LibraryRoute

                            val homeLabel = stringResource(R.string.nav_home)
                            val searchLabel = stringResource(R.string.nav_search)
                            val bibleLabel = stringResource(R.string.nav_bible)
                            val scriptureLabel = stringResource(R.string.nav_scripture)
                            val libraryLabel = stringResource(R.string.nav_library)

                            NavigationBarItem(
                                selected = isHome,
                                onClick = {
                                    if (isHome) {
                                        navigator.onReselect(HomeRoute)
                                    } else {
                                        navigator.navigate(HomeRoute)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                                        contentDescription = homeLabel
                                    )
                                },
                                label = { Text(homeLabel) }
                            )

                            NavigationBarItem(
                                selected = isSearch,
                                onClick = {
                                    if (isSearch) {
                                        navigator.onReselect(SearchRoute)
                                    } else {
                                        navigator.navigate(SearchRoute)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSearch) Icons.Filled.Search else Icons.Outlined.Search,
                                        contentDescription = searchLabel
                                    )
                                },
                                label = { Text(searchLabel) }
                            )

                            NavigationBarItem(
                                selected = isBible,
                                onClick = {
                                    if (isBible) {
                                        navigator.onReselect(BibleRoute)
                                    } else {
                                        navigator.navigate(BibleRoute)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isBible) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook,
                                        contentDescription = bibleLabel
                                    )
                                },
                                label = { Text(bibleLabel) }
                            )

                            NavigationBarItem(
                                selected = isScripture,
                                onClick = {
                                    if (isScripture) {
                                        navigator.onReselect(ScriptureRoute)
                                    } else {
                                        navigator.navigate(ScriptureRoute)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isScripture) Icons.Filled.Explore else Icons.Outlined.Explore,
                                        contentDescription = scriptureLabel
                                    )
                                },
                                label = { Text(scriptureLabel) }
                            )

                            NavigationBarItem(
                                selected = isLibrary,
                                onClick = {
                                    if (isLibrary) {
                                        navigator.onReselect(LibraryRoute)
                                    } else {
                                        navigator.navigate(LibraryRoute)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isLibrary) Icons.Filled.Bookmarks else Icons.Outlined.Bookmarks,
                                        contentDescription = libraryLabel
                                    )
                                },
                                label = { Text(libraryLabel) }
                            )
                        }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isFullscreenPlayer) {
                OfflineBanner(isOnline = isOnline)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val entries = navigationState.toDecoratedEntries(
                    entryProvider {
                        entry<HomeRoute> {
                            HomeScreen(
                                onSermonSelect = { sermon ->
                                    navigator.navigate(SermonDetailRoute(sermon.id))
                                },
                                onTextSermonSelect = { sermon ->
                                    navigator.navigate(TextSermonRoute(sermon.id))
                                },
                                onSpeakerSelect = { speaker ->
                                    navigator.navigate(SpeakerDetailRoute(speaker.slug))
                                },
                                onSeeAllSpeakers = {
                                    navigator.navigate(SpeakersRoute)
                                },
                                onTopicSelect = { slug ->
                                    navigator.navigate(TopicDetailRoute(slug))
                                },
                                onSeeAllTopics = {
                                    navigator.navigate(TopicsRoute)
                                },
                                reselectEvents = navigator.reselectEvents
                            )
                        }

                        entry<SearchRoute> {
                            SearchScreen(
                                onSpeakerSelect = { slug ->
                                    navigator.navigate(SpeakerDetailRoute(slug))
                                },
                                onTopicSelect = { slug ->
                                    navigator.navigate(TopicDetailRoute(slug))
                                },
                                onSermonSelect = { sermonId, query ->
                                    navigator.navigate(SermonDetailRoute(sermonId, highlightQuery = query))
                                },
                                onTextSermonSelect = { sermonId, query ->
                                    navigator.navigate(TextSermonRoute(sermonId, highlightQuery = query))
                                },
                                reselectEvents = navigator.reselectEvents
                            )
                        }

                        entry<BibleRoute> {
                            BibleScreen(
                                onNavigateToVerseSermons = { bookId, bookName, chapter, verse ->
                                    navigator.navigate(BibleVerseSermonsRoute(bookId, bookName, chapter, verse))
                                },
                                onBottomBarVisibilityChange = { isBibleBottomBarVisible = it },
                                reselectEvents = navigator.reselectEvents
                            )
                        }

                        entry<BibleVerseSermonsRoute> { route ->
                            BibleVerseSermonsScreen(
                                bookId = route.bookId,
                                bookName = route.bookName,
                                chapter = route.chapter,
                                verse = route.verse,
                                onBack = { navigator.goBack() },
                                onSermonClick = { sermon ->
                                    navigator.navigate(SermonDetailRoute(sermon.id))
                                },
                                onTextSermonClick = { sermon ->
                                    navigator.navigate(TextSermonRoute(sermon.id))
                                }
                            )
                        }

                        entry<ScriptureRoute> {
                            ScriptureScreen(
                                onSermonClick = { sermon ->
                                    navigator.navigate(SermonDetailRoute(sermon.id))
                                },
                                onTextSermonClick = { sermon ->
                                    navigator.navigate(TextSermonRoute(sermon.id))
                                },
                                reselectEvents = navigator.reselectEvents,
                                onIsAtRootChanged = { isScriptureAtRoot = it }
                            )
                        }

                        entry<SpeakersRoute> {
                            SpeakersScreen(
                                onSpeakerSelect = { slug ->
                                    navigator.navigate(SpeakerDetailRoute(slug))
                                },
                                onBackClick = { navigator.goBack() },
                                reselectEvents = navigator.reselectEvents
                            )
                        }

                        entry<SpeakerDetailRoute> { route ->
                            SpeakerDetailScreen(
                                slug = route.slug,
                                onBack = { navigator.goBack() },
                                onSermonClick = { sermon ->
                                    navigator.navigate(SermonDetailRoute(sermon.id))
                                },
                                onTextSermonClick = { sermon ->
                                    navigator.navigate(TextSermonRoute(sermon.id))
                                },
                                onTopicClick = { slug ->
                                    navigator.navigate(TopicDetailRoute(slug))
                                }
                            )
                        }

                        entry<TopicsRoute> {
                            TopicsScreen(
                                onTopicSelect = { slug ->
                                    navigator.navigate(TopicDetailRoute(slug))
                                },
                                onBackClick = { navigator.goBack() },
                                reselectEvents = navigator.reselectEvents
                            )
                        }

                        entry<TopicDetailRoute> { route ->
                            TopicDetailScreen(
                                slug = route.slug,
                                onBack = { navigator.goBack() },
                                onSermonClick = { sermon ->
                                    navigator.navigate(SermonDetailRoute(sermon.id))
                                },
                                onTextSermonClick = { sermon ->
                                    navigator.navigate(TextSermonRoute(sermon.id))
                                }
                            )
                        }

                        entry<LibraryRoute> {
                            LibraryScreen(
                                onSermonSelect = { sermonId ->
                                    navigator.navigate(SermonDetailRoute(sermonId))
                                },
                                reselectEvents = navigator.reselectEvents
                            )
                        }

                        entry<SermonDetailRoute> { route ->
                            SermonDetailScreen(
                                sermonId = route.sermonId,
                                highlightQuery = route.highlightQuery,
                                onBack = { navigator.goBack() },
                                onSpeakerClick = { slug ->
                                    navigator.navigate(SpeakerDetailRoute(slug))
                                },
                                onReadClick = { sermonId ->
                                    navigator.navigate(TextSermonRoute(sermonId, highlightQuery = route.highlightQuery))
                                }
                            )
                        }

                        entry<PlayerRoute> {
                            FullscreenPlayerScreen(
                                onBack = { navigator.goBack() }
                            )
                        }

                        entry<TextSermonRoute> { route ->
                            TextSermonScreen(
                                sermonId = route.sermonId,
                                highlightQuery = route.highlightQuery,
                                onBack = { navigator.goBack() },
                                onSpeakerClick = { slug ->
                                    navigator.navigate(SpeakerDetailRoute(slug))
                                }
                            )
                        }
                    }
                )

                NavDisplay(
                    entries = entries,
                    onBack = { navigator.goBack() }
                )
            }
        }
    }
}
