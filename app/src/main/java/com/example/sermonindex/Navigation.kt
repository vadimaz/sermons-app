package com.example.sermonindex

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.sermonindex.ui.components.MiniPlayer
import com.example.sermonindex.ui.home.HomeScreen
import com.example.sermonindex.ui.library.LibraryScreen
import com.example.sermonindex.ui.player.FullscreenPlayerScreen
import com.example.sermonindex.ui.scripture.ScriptureScreen
import com.example.sermonindex.ui.sermon.SermonDetailScreen
import com.example.sermonindex.ui.speakers.SpeakerDetailScreen
import com.example.sermonindex.ui.speakers.SpeakersScreen
import com.example.sermonindex.ui.topics.TopicDetailScreen
import com.example.sermonindex.ui.topics.TopicsScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(HomeRoute as NavKey)
    val playbackManager = SermonApp.instance.playbackManager
    val playbackState by playbackManager.playbackState.collectAsStateWithLifecycle()

    val currentRoute = backStack.lastOrNull()
    val isFullscreenPlayer = currentRoute is PlayerRoute

    Scaffold(
        bottomBar = {
            if (!isFullscreenPlayer) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Persistent MiniPlayer floating above bottom navigation
                    if (playbackState.hasActiveMedia) {
                        MiniPlayer(
                            playbackState = playbackState,
                            onTogglePlayPause = { playbackManager.togglePlayPause() },
                            onExpandClick = { backStack.add(PlayerRoute) }
                        )
                    }

                    // Bottom Navigation Bar
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        val isHome = currentRoute is HomeRoute
                        val isScripture = currentRoute is ScriptureRoute
                        val isSpeakers = currentRoute is SpeakersRoute || currentRoute is SpeakerDetailRoute
                        val isTopics = currentRoute is TopicsRoute || currentRoute is TopicDetailRoute
                        val isLibrary = currentRoute is LibraryRoute

                        NavigationBarItem(
                            selected = isHome,
                            onClick = {
                                if (!isHome) {
                                    backStack.clear()
                                    backStack.add(HomeRoute)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Home"
                                )
                            },
                            label = { Text("Home") }
                        )

                        NavigationBarItem(
                            selected = isScripture,
                            onClick = {
                                if (!isScripture) {
                                    backStack.clear()
                                    backStack.add(ScriptureRoute)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isScripture) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook,
                                    contentDescription = "Scripture"
                                )
                            },
                            label = { Text("Scripture") }
                        )

                        NavigationBarItem(
                            selected = isSpeakers,
                            onClick = {
                                if (!isSpeakers) {
                                    backStack.clear()
                                    backStack.add(SpeakersRoute)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSpeakers) Icons.Filled.Person else Icons.Outlined.Person,
                                    contentDescription = "Speakers"
                                )
                            },
                            label = { Text("Speakers") }
                        )

                        NavigationBarItem(
                            selected = isTopics,
                            onClick = {
                                if (!isTopics) {
                                    backStack.clear()
                                    backStack.add(TopicsRoute)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isTopics) Icons.AutoMirrored.Filled.Label else Icons.AutoMirrored.Outlined.Label,
                                    contentDescription = "Topics"
                                )
                            },
                            label = { Text("Topics") }
                        )

                        NavigationBarItem(
                            selected = isLibrary,
                            onClick = {
                                if (!isLibrary) {
                                    backStack.clear()
                                    backStack.add(LibraryRoute)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isLibrary) Icons.Filled.Bookmarks else Icons.Outlined.Bookmarks,
                                    contentDescription = "Library"
                                )
                            },
                            label = { Text("Library") }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = {
                    if (backStack.size > 1) {
                        backStack.removeLastOrNull()
                    }
                },
                entryProvider = entryProvider {
                    entry<HomeRoute> {
                        HomeScreen(
                            onSermonSelect = { sermon ->
                                backStack.add(SermonDetailRoute(sermon.id))
                            },
                            onTopicSelect = { slug ->
                                backStack.add(TopicDetailRoute(slug))
                            }
                        )
                    }

                    entry<ScriptureRoute> {
                        ScriptureScreen(
                            onSermonClick = { sermon ->
                                backStack.add(SermonDetailRoute(sermon.id))
                            }
                        )
                    }

                    entry<SpeakersRoute> {
                        SpeakersScreen(
                            onSpeakerSelect = { slug ->
                                backStack.add(SpeakerDetailRoute(slug))
                            }
                        )
                    }

                    entry<SpeakerDetailRoute> { route ->
                        SpeakerDetailScreen(
                            slug = route.slug,
                            onBack = { backStack.removeLastOrNull() },
                            onSermonClick = { sermon ->
                                backStack.add(SermonDetailRoute(sermon.id))
                            },
                            onTopicClick = { slug ->
                                backStack.add(TopicDetailRoute(slug))
                            }
                        )
                    }

                    entry<TopicsRoute> {
                        TopicsScreen(
                            onTopicSelect = { slug ->
                                backStack.add(TopicDetailRoute(slug))
                            }
                        )
                    }

                    entry<TopicDetailRoute> { route ->
                        TopicDetailScreen(
                            slug = route.slug,
                            onBack = { backStack.removeLastOrNull() },
                            onSermonClick = { sermon ->
                                backStack.add(SermonDetailRoute(sermon.id))
                            }
                        )
                    }

                    entry<LibraryRoute> {
                        LibraryScreen(
                            onSermonSelect = { sermonId ->
                                backStack.add(SermonDetailRoute(sermonId))
                            }
                        )
                    }

                    entry<SermonDetailRoute> { route ->
                        SermonDetailScreen(
                            sermonId = route.sermonId,
                            onBack = { backStack.removeLastOrNull() },
                            onSpeakerClick = { slug ->
                                backStack.add(SpeakerDetailRoute(slug))
                            }
                        )
                    }

                    entry<PlayerRoute> {
                        FullscreenPlayerScreen(
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
            )
        }
    }
}
