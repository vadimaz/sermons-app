package com.example.sermonindex

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
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
import com.example.sermonindex.theme.SermonIndexTheme
import com.example.sermonindex.theme.ThemePreviews
import com.example.sermonindex.ui.bible.BibleScreen
import com.example.sermonindex.ui.bible.sermons.BibleVerseSermonsScreen
import com.example.sermonindex.ui.components.MiniPlayer
import com.example.sermonindex.ui.components.OfflineBanner
import com.example.sermonindex.ui.home.HomeScreen
import com.example.sermonindex.ui.library.BibleBookmarksScreen
import com.example.sermonindex.ui.library.DownloadsScreen
import com.example.sermonindex.ui.library.FavoritesScreen
import com.example.sermonindex.ui.library.HistoryScreen
import com.example.sermonindex.ui.library.LibraryScreen
import com.example.sermonindex.ui.player.FullscreenPlayerScreen
import com.example.sermonindex.ui.reader.TextSermonScreen
import com.example.sermonindex.ui.search.SearchScreen
import com.example.sermonindex.ui.settings.SettingsScreen
import com.example.sermonindex.ui.scripture.ScriptureChaptersScreen
import com.example.sermonindex.ui.scripture.ScriptureScreen
import com.example.sermonindex.ui.scripture.ScriptureVerseHeatMapScreen
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

private val TOP_LEVEL_CONTENT_KEYS: Set<Any> = TOP_LEVEL_ROUTES.flatMapTo(linkedSetOf()) {
    listOf(it, it.toString())
}

internal fun isTopLevelTransition(initialKey: Any?, targetKey: Any?): Boolean =
    initialKey != null && targetKey != null &&
        initialKey in TOP_LEVEL_CONTENT_KEYS &&
        targetKey in TOP_LEVEL_CONTENT_KEYS

/**
 * Material 3 Expressive Fade Through transition with spring physics for switching between peer top-level destinations.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun topLevelFadeThroughTransition(): ContentTransform {
    val expressiveMotion = MotionScheme.expressive()
    return (
        fadeIn(animationSpec = expressiveMotion.defaultEffectsSpec()) +
            scaleIn(
                initialScale = 0.92f,
                animationSpec = expressiveMotion.defaultSpatialSpec()
            )
        ) togetherWith fadeOut(animationSpec = expressiveMotion.fastEffectsSpec())
}

/**
 * Material 3 Expressive Shared X-Axis forward transition with spring physics for drilling down into sub-screens.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun hierarchicalForwardTransition(): ContentTransform {
    val expressiveMotion = MotionScheme.expressive()
    return (
        slideInHorizontally(
            initialOffsetX = { fullWidth -> (fullWidth * 0.22f).toInt() },
            animationSpec = expressiveMotion.defaultSpatialSpec()
        ) + fadeIn(animationSpec = expressiveMotion.defaultEffectsSpec())
        ) togetherWith (
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -(fullWidth * 0.18f).toInt() },
            animationSpec = expressiveMotion.defaultSpatialSpec()
        ) + fadeOut(animationSpec = expressiveMotion.fastEffectsSpec())
        )
}

/**
 * Material 3 Expressive Shared X-Axis pop transition with spring physics for returning from sub-screens.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun hierarchicalPopTransition(): ContentTransform {
    val expressiveMotion = MotionScheme.expressive()
    return (
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -(fullWidth * 0.18f).toInt() },
            animationSpec = expressiveMotion.defaultSpatialSpec()
        ) + fadeIn(animationSpec = expressiveMotion.defaultEffectsSpec())
        ) togetherWith (
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> (fullWidth * 0.22f).toInt() },
            animationSpec = expressiveMotion.defaultSpatialSpec()
        ) + fadeOut(animationSpec = expressiveMotion.fastEffectsSpec())
        )
}

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
        if (currentRoute == state.topLevelRoute) {
            if (state.topLevelRoute != state.startRoute) {
                state.topLevelRoute = state.startRoute
            }
        } else {
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
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                rememberViewModelStoreNavEntryDecorator<NavKey>()
            )
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = decorators,
                entryProvider = entryProvider
            )
        }

        return getTopLevelRoutesInUse()
            .flatMap { decoratedEntries[it] ?: emptyList() }
    }

    internal fun getTopLevelRoutesInUse(): List<NavKey> =
        if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }
}

@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
    initialTopLevelRoute: NavKey = startRoute
): NavigationState {
    val topLevelRoute = rememberSaveable(
        startRoute,
        initialTopLevelRoute,
        saver = Saver(
            save = { it.value.toString() },
            restore = { savedString ->
                mutableStateOf(
                    topLevelRoutes.firstOrNull { it.toString() == savedString } ?: startRoute
                )
            }
        )
    ) {
        mutableStateOf(initialTopLevelRoute)
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainNavigation(
    playbackManager: PlaybackManager,
    networkMonitor: NetworkMonitor,
    initialRoute: NavKey = HomeRoute,
    navigateToRoute: NavKey? = null
) {
    val navigationState = rememberNavigationState(
        startRoute = HomeRoute,
        topLevelRoutes = TOP_LEVEL_ROUTES,
        initialTopLevelRoute = initialRoute
    )
    val navigator = remember { Navigator(navigationState) }

    LaunchedEffect(navigateToRoute) {
        if (navigateToRoute != null) {
            navigator.navigate(navigateToRoute)
        }
    }

    val activeMediaInfo by playbackManager.activeMediaInfo.collectAsStateWithLifecycle()
    val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle(initialValue = true)

    val currentRoute = navigationState.currentRoute
    val isFullscreenPlayer = currentRoute is PlayerRoute || currentRoute is TextSermonRoute || currentRoute is FullscreenVideoRoute
    var isBibleBottomBarVisible by rememberSaveable { mutableStateOf(true) }

    val isAtTopLevel = currentRoute in TOP_LEVEL_ROUTES
    val isBibleTrack = activeMediaInfo.isBibleTrack
    val isBibleTab = navigationState.topLevelRoute == BibleRoute
    val effectivelyShowBottomBar = if (isBibleTab) isBibleBottomBarVisible else true

    val showBottomBar = isAtTopLevel
    val showMiniPlayer = !isFullscreenPlayer && activeMediaInfo.hasActiveMedia && !isBibleTrack

    val isFullscreenVideo = currentRoute is FullscreenVideoRoute

    Scaffold(
        contentWindowInsets = if (isFullscreenVideo) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
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
                            mediaInfo = activeMediaInfo,
                            progressProvider = { playbackManager.progressFraction },
                            onTogglePlayPause = { playbackManager.togglePlayPause() },
                            onExpandClick = { navigator.navigate(PlayerRoute) },
                            onCloseClick = { playbackManager.stop() }
                        )
                    }

                    // Expressive Short Navigation Bar
                    AnimatedVisibility(
                        visible = showBottomBar && effectivelyShowBottomBar,
                        enter = expandVertically(
                            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                            expandFrom = Alignment.Top
                        ) + fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                        exit = shrinkVertically(
                            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                            shrinkTowards = Alignment.Top
                        ) + fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec())
                    ) {
                        SermonIndexBottomBar(
                            topLevelRoute = navigationState.topLevelRoute,
                            onNavigate = { route -> navigator.navigate(route) },
                            onReselect = { route -> navigator.onReselect(route) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreenVideo) PaddingValues(0.dp) else innerPadding)
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
                                onBookSelect = { book ->
                                    navigator.navigate(
                                        ScriptureChaptersRoute(
                                            bookId = book.id,
                                            bookName = book.name,
                                            chapterCount = book.chapterCount
                                        )
                                    )
                                },
                                reselectEvents = navigator.reselectEvents
                            )
                        }

                        entry<ScriptureChaptersRoute> { route ->
                            ScriptureChaptersScreen(
                                bookId = route.bookId,
                                bookName = route.bookName,
                                chapterCount = route.chapterCount,
                                onChapterSelect = { chapter ->
                                    navigator.navigate(
                                        ScriptureVerseHeatMapRoute(
                                            bookId = route.bookId,
                                            bookName = route.bookName,
                                            chapter = chapter
                                        )
                                    )
                                },
                                onBack = { navigator.goBack() }
                            )
                        }

                        entry<ScriptureVerseHeatMapRoute> { route ->
                            ScriptureVerseHeatMapScreen(
                                bookId = route.bookId,
                                bookName = route.bookName,
                                chapter = route.chapter,
                                onVerseSelect = { verse ->
                                    navigator.navigate(
                                        BibleVerseSermonsRoute(
                                            bookId = route.bookId,
                                            bookName = route.bookName,
                                            chapter = route.chapter,
                                            verse = verse
                                        )
                                    )
                                },
                                onBack = { navigator.goBack() }
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
                                onNavigateToFavorites = {
                                    navigator.navigate(FavoritesRoute)
                                },
                                onNavigateToDownloads = {
                                    navigator.navigate(DownloadsRoute)
                                },
                                onNavigateToHistory = {
                                    navigator.navigate(HistoryRoute)
                                },
                                onNavigateToBookmarks = {
                                    navigator.navigate(BibleBookmarksRoute)
                                },
                                onNavigateToSettings = {
                                    navigator.navigate(SettingsRoute)
                                },
                                reselectEvents = navigator.reselectEvents
                            )
                        }

                        entry<FavoritesRoute> {
                            FavoritesScreen(
                                onBack = { navigator.goBack() },
                                onSermonSelect = { sermonId ->
                                    navigator.navigate(SermonDetailRoute(sermonId))
                                }
                            )
                        }

                        entry<DownloadsRoute> {
                            DownloadsScreen(
                                onBack = { navigator.goBack() },
                                onSermonSelect = { sermonId ->
                                    navigator.navigate(SermonDetailRoute(sermonId))
                                }
                            )
                        }

                        entry<HistoryRoute> {
                            HistoryScreen(
                                onBack = { navigator.goBack() },
                                onSermonSelect = { sermonId ->
                                    navigator.navigate(SermonDetailRoute(sermonId))
                                }
                            )
                        }

                        entry<BibleBookmarksRoute> {
                            BibleBookmarksScreen(
                                onBack = { navigator.goBack() },
                                onNavigateToBible = {
                                    navigator.navigate(BibleRoute)
                                }
                            )
                        }

                        entry<SettingsRoute> {
                            SettingsScreen(
                                onBack = { navigator.goBack() },
                                onNavigateToDownloads = {
                                    navigator.navigate(DownloadsRoute)
                                }
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
                                },
                                onPlayClick = {
                                    navigator.navigate(PlayerRoute)
                                }
                            )
                        }

                        entry<PlayerRoute> {
                            FullscreenPlayerScreen(
                                onBack = { navigator.goBack() },
                                onFullscreenVideoClick = { navigator.navigate(FullscreenVideoRoute) }
                            )
                        }

                        entry<FullscreenVideoRoute> {
                            com.example.sermonindex.ui.player.FullscreenVideoScreen(
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
                    onBack = { navigator.goBack() },
                    transitionSpec = {
                        val initialKey = initialState.entries.lastOrNull()?.contentKey
                        val targetKey = targetState.entries.lastOrNull()?.contentKey
                        if (isTopLevelTransition(initialKey, targetKey)) {
                            topLevelFadeThroughTransition()
                        } else {
                            hierarchicalForwardTransition()
                        }
                    },
                    popTransitionSpec = {
                        val initialKey = initialState.entries.lastOrNull()?.contentKey
                        val targetKey = targetState.entries.lastOrNull()?.contentKey
                        if (isTopLevelTransition(initialKey, targetKey)) {
                            topLevelFadeThroughTransition()
                        } else {
                            hierarchicalPopTransition()
                        }
                    },
                    predictivePopTransitionSpec = {
                        val initialKey = initialState.entries.lastOrNull()?.contentKey
                        val targetKey = targetState.entries.lastOrNull()?.contentKey
                        if (isTopLevelTransition(initialKey, targetKey)) {
                            topLevelFadeThroughTransition()
                        } else {
                            hierarchicalPopTransition()
                        }
                    }
                )
            }
        }
    }
}

@Composable
internal fun SermonIndexBottomBar(
    topLevelRoute: NavKey,
    onNavigate: (NavKey) -> Unit,
    onReselect: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    // Note: Do NOT pass Modifier.fillMaxWidth() directly to ShortNavigationBar,
    // because ShortNavigationBar's internal Surface propagates minWidth constraints
    // to EqualWeightContentMeasurePolicy, which clamps each item's width to minWidth.
    ShortNavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    ) {
        val isHome = topLevelRoute == HomeRoute
        val isSearch = topLevelRoute == SearchRoute
        val isBible = topLevelRoute == BibleRoute
        val isScripture = topLevelRoute == ScriptureRoute
        val isLibrary = topLevelRoute == LibraryRoute

        val homeLabel = stringResource(R.string.nav_home)
        val searchLabel = stringResource(R.string.nav_search)
        val bibleLabel = stringResource(R.string.nav_bible)
        val scriptureLabel = stringResource(R.string.nav_scripture)
        val libraryLabel = stringResource(R.string.nav_library)

        ShortNavigationBarItem(
            selected = isHome,
            onClick = {
                if (isHome) onReselect(HomeRoute) else onNavigate(HomeRoute)
            },
            icon = {
                Icon(
                    imageVector = if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = homeLabel
                )
            },
            label = { Text(homeLabel) }
        )

        ShortNavigationBarItem(
            selected = isSearch,
            onClick = {
                if (isSearch) onReselect(SearchRoute) else onNavigate(SearchRoute)
            },
            icon = {
                Icon(
                    imageVector = if (isSearch) Icons.Filled.Search else Icons.Outlined.Search,
                    contentDescription = searchLabel
                )
            },
            label = { Text(searchLabel) }
        )

        ShortNavigationBarItem(
            selected = isBible,
            onClick = {
                if (isBible) onReselect(BibleRoute) else onNavigate(BibleRoute)
            },
            icon = {
                Icon(
                    imageVector = if (isBible) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = bibleLabel
                )
            },
            label = { Text(bibleLabel) }
        )

        ShortNavigationBarItem(
            selected = isScripture,
            onClick = {
                if (isScripture) onReselect(ScriptureRoute) else onNavigate(ScriptureRoute)
            },
            icon = {
                Icon(
                    imageVector = if (isScripture) Icons.Filled.Explore else Icons.Outlined.Explore,
                    contentDescription = scriptureLabel
                )
            },
            label = { Text(scriptureLabel) }
        )

        ShortNavigationBarItem(
            selected = isLibrary,
            onClick = {
                if (isLibrary) onReselect(LibraryRoute) else onNavigate(LibraryRoute)
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

@ThemePreviews
@Composable
private fun SermonIndexBottomBarPreview() {
    SermonIndexTheme {
        Surface {
            SermonIndexBottomBar(
                topLevelRoute = HomeRoute,
                onNavigate = {},
                onReselect = {}
            )
        }
    }
}
