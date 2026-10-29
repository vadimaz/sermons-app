package com.example.sermonindex.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.sermonindex.LibraryRoute
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.BibleBookmark
import com.example.sermonindex.domain.model.FavoriteSermon
import com.example.sermonindex.theme.SermonIndexTheme
import com.example.sermonindex.theme.ThemePreviews
import com.example.sermonindex.ui.bible.components.QuoteCardSheet
import com.example.sermonindex.ui.components.PreviewFixtures
import com.example.sermonindex.ui.library.components.BookmarksTabContent
import com.example.sermonindex.ui.library.components.DownloadsTabContent
import com.example.sermonindex.ui.library.components.FavoritesTabContent
import com.example.sermonindex.ui.library.components.HistoryTabContent
import kotlinx.coroutines.flow.Flow

/**
 * Stateful entry point for LibraryScreen.
 */
@Composable
fun LibraryScreen(
    onSermonSelect: (String) -> Unit,
    onNavigateToBible: () -> Unit = {},
    reselectEvents: Flow<NavKey>? = null,
    modifier: Modifier = Modifier
) {
    val viewModel: LibraryViewModel = hiltViewModel()

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LibraryContent(
        state = state,
        onEvent = { event ->
            when (event) {
                is LibraryUiEvent.SelectSermon -> onSermonSelect(event.sermonId)
                is LibraryUiEvent.OpenBookmarkInBible -> {
                    viewModel.onEvent(event)
                    onNavigateToBible()
                }
                else -> viewModel.onEvent(event)
            }
        },
        reselectEvents = reselectEvents,
        modifier = modifier
    )
}

/**
 * Stateless LibraryContent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(
    state: LibraryUiState,
    onEvent: (LibraryUiEvent) -> Unit,
    reselectEvents: Flow<NavKey>? = null,
    modifier: Modifier = Modifier
) {
    val favoritesListState = rememberLazyListState()
    val downloadsListState = rememberLazyListState()
    val historyListState = rememberLazyListState()
    val bookmarksListState = rememberLazyListState()
    var sharingBookmark by remember { mutableStateOf<BibleBookmark?>(null) }

    LaunchedEffect(reselectEvents) {
        reselectEvents?.collect { route ->
            if (route == LibraryRoute) {
                when (state.selectedTab) {
                    0 -> favoritesListState.animateScrollToItem(0)
                    1 -> downloadsListState.animateScrollToItem(0)
                    2 -> historyListState.animateScrollToItem(0)
                    3 -> bookmarksListState.animateScrollToItem(0)
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        PrimaryTabRow(
            selectedTabIndex = state.selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = state.selectedTab == 0,
                onClick = { onEvent(LibraryUiEvent.SelectTab(0)) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (state.favorites.isNotEmpty()) {
                                Badge { Text("${state.favorites.size}") }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null)
                    }
                },
                text = { Text(stringResource(R.string.tab_favorites)) }
            )
            Tab(
                selected = state.selectedTab == 1,
                onClick = { onEvent(LibraryUiEvent.SelectTab(1)) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (state.downloads.isNotEmpty()) {
                                Badge { Text("${state.downloads.size}") }
                            }
                        }
                    ) {
                        Icon(Icons.Default.DownloadDone, contentDescription = null)
                    }
                },
                text = { Text(stringResource(R.string.tab_downloads)) }
            )
            Tab(
                selected = state.selectedTab == 2,
                onClick = { onEvent(LibraryUiEvent.SelectTab(2)) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (state.history.isNotEmpty()) {
                                Badge { Text("${state.history.size}") }
                            }
                        }
                    ) {
                        Icon(Icons.Default.History, contentDescription = null)
                    }
                },
                text = { Text(stringResource(R.string.tab_history)) }
            )
            Tab(
                selected = state.selectedTab == 3,
                onClick = { onEvent(LibraryUiEvent.SelectTab(3)) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (state.bookmarks.isNotEmpty()) {
                                Badge { Text("${state.bookmarks.size}") }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = null)
                    }
                },
                text = { Text(stringResource(R.string.tab_bookmarks)) }
            )
        }

        when (state.selectedTab) {
            0 -> FavoritesTabContent(
                favorites = state.favorites,
                listState = favoritesListState,
                onPlay = { onEvent(LibraryUiEvent.PlayFavorite(it)) },
                onSelect = { onEvent(LibraryUiEvent.SelectSermon(it)) },
                onRemove = { onEvent(LibraryUiEvent.RemoveFavorite(it)) }
            )
            1 -> DownloadsTabContent(
                downloads = state.downloads,
                listState = downloadsListState,
                totalStorageFormatted = state.totalDownloadsSizeFormatted,
                onPlay = { onEvent(LibraryUiEvent.PlayDownload(it)) },
                onSelect = { onEvent(LibraryUiEvent.SelectSermon(it)) },
                onDelete = { onEvent(LibraryUiEvent.DeleteDownload(it)) },
                onDeleteAll = { onEvent(LibraryUiEvent.DeleteAllDownloads) }
            )
            2 -> HistoryTabContent(
                history = state.history,
                listState = historyListState,
                onPlay = { onEvent(LibraryUiEvent.ResumeHistory(it)) },
                onSelect = { onEvent(LibraryUiEvent.SelectSermon(it)) },
                onClearAll = { onEvent(LibraryUiEvent.ClearHistory) }
            )
            3 -> BookmarksTabContent(
                bookmarks = state.bookmarks,
                listState = bookmarksListState,
                onSelectBookmark = { onEvent(LibraryUiEvent.OpenBookmarkInBible(it)) },
                onDelete = { onEvent(LibraryUiEvent.DeleteBookmark(it)) },
                onShare = { bm -> sharingBookmark = bm }
            )
        }
    }

    sharingBookmark?.let { bm ->
        QuoteCardSheet(
            verseText = bm.text,
            reference = "${bm.bookName} ${bm.chapter}:${bm.verse}",
            translationName = bm.translationId,
            onDismissRequest = { sharingBookmark = null }
        )
    }
}

@ThemePreviews
@Composable
private fun LibraryContentPreview() {
    SermonIndexTheme {
        Surface {
            LibraryContent(
                state = LibraryUiState(
                    selectedTab = 0,
                    favorites = listOf(
                        FavoriteSermon(
                            id = "fav_1",
                            title = PreviewFixtures.previewAudioSermon.title,
                            speaker = PreviewFixtures.previewAudioSermon.speaker,
                            speakerImage = PreviewFixtures.previewAudioSermon.speakerImage,
                            duration = PreviewFixtures.previewAudioSermon.duration
                        )
                    )
                ),
                onEvent = {}
            )
        }
    }
}
