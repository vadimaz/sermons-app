package com.example.sermonindex.ui.library

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImage
import com.example.sermonindex.LibraryRoute
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.BibleBookmark
import com.example.sermonindex.domain.model.DownloadedSermon
import com.example.sermonindex.domain.model.FavoriteSermon
import com.example.sermonindex.ui.bible.components.QuoteCardSheet
import com.example.sermonindex.domain.model.HighlightColor
import com.example.sermonindex.domain.model.ListeningHistory
import com.example.sermonindex.ui.components.EmptyStateView
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
    val context = LocalContext.current
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
            0 -> FavoritesList(
                favorites = state.favorites,
                listState = favoritesListState,
                onPlay = { onEvent(LibraryUiEvent.PlayFavorite(it)) },
                onSelect = { onEvent(LibraryUiEvent.SelectSermon(it)) },
                onRemove = { onEvent(LibraryUiEvent.RemoveFavorite(it)) }
            )
            1 -> DownloadsList(
                downloads = state.downloads,
                listState = downloadsListState,
                totalStorageFormatted = state.totalDownloadsSizeFormatted,
                onPlay = { onEvent(LibraryUiEvent.PlayDownload(it)) },
                onSelect = { onEvent(LibraryUiEvent.SelectSermon(it)) },
                onDelete = { onEvent(LibraryUiEvent.DeleteDownload(it)) },
                onDeleteAll = { onEvent(LibraryUiEvent.DeleteAllDownloads) }
            )
            2 -> HistoryList(
                history = state.history,
                listState = historyListState,
                onPlay = { onEvent(LibraryUiEvent.ResumeHistory(it)) },
                onSelect = { onEvent(LibraryUiEvent.SelectSermon(it)) },
                onClearAll = { onEvent(LibraryUiEvent.ClearHistory) }
            )
            3 -> BookmarksList(
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

@Composable
private fun FavoritesList(
    favorites: List<FavoriteSermon>,
    listState: LazyListState = rememberLazyListState(),
    onPlay: (FavoriteSermon) -> Unit,
    onSelect: (String) -> Unit,
    onRemove: (FavoriteSermon) -> Unit
) {
    if (favorites.isEmpty()) {
        EmptyStateView(
            title = stringResource(R.string.no_favorites_title),
            subtitle = stringResource(R.string.no_favorites_subtitle)
        )
    } else {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(favorites, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item.id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.speakerImage,
                            contentDescription = item.speaker,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.speaker,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (item.duration.isNotBlank()) {
                                Text(
                                    text = item.duration,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onRemove(item) }) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = stringResource(R.string.remove),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        FilledIconButton(
                            onClick = { onPlay(item) },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryList(
    history: List<ListeningHistory>,
    listState: LazyListState = rememberLazyListState(),
    onPlay: (ListeningHistory) -> Unit,
    onSelect: (String) -> Unit,
    onClearAll: () -> Unit
) {
    if (history.isEmpty()) {
        EmptyStateView(
            title = stringResource(R.string.no_history_title),
            subtitle = stringResource(R.string.no_history_subtitle)
        )
    } else {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClearAll) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.clear_history))
                    }
                }
            }

            items(history, key = { it.sermonId }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item.sermonId) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = item.speakerImage,
                                contentDescription = item.speaker,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.speaker,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledIconButton(
                                onClick = { onPlay(item) },
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { item.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsList(
    downloads: List<DownloadedSermon>,
    totalStorageFormatted: String,
    listState: LazyListState = rememberLazyListState(),
    onPlay: (DownloadedSermon) -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (DownloadedSermon) -> Unit,
    onDeleteAll: () -> Unit
) {
    var sermonToDelete by remember { mutableStateOf<DownloadedSermon?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.delete_all_downloads_title)) },
            text = { Text(stringResource(R.string.delete_all_downloads_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAll()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text(stringResource(R.string.delete_all), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (sermonToDelete != null) {
        AlertDialog(
            onDismissRequest = { sermonToDelete = null },
            title = { Text(stringResource(R.string.delete_download)) },
            text = { Text(stringResource(R.string.delete_download_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        sermonToDelete?.let { onDelete(it) }
                        sermonToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sermonToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (downloads.isEmpty()) {
        EmptyStateView(
            title = stringResource(R.string.no_downloads_title),
            subtitle = stringResource(R.string.no_downloads_subtitle)
        )
    } else {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.total_downloads_size, totalStorageFormatted),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { showDeleteAllDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.delete_all_downloads))
                    }
                }
            }

            items(downloads, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item.id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.speakerImage,
                            contentDescription = item.speaker,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.speaker,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (item.duration.isNotBlank()) {
                                    Text(
                                        text = item.duration,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = item.formattedSize,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { sermonToDelete = item }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_download),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        FilledIconButton(
                            onClick = { onPlay(item) },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarksList(
    bookmarks: List<BibleBookmark>,
    listState: LazyListState,
    onSelectBookmark: (BibleBookmark) -> Unit,
    onDelete: (String) -> Unit,
    onShare: (BibleBookmark) -> Unit,
    modifier: Modifier = Modifier
) {
    if (bookmarks.isEmpty()) {
        EmptyStateView(
            title = stringResource(R.string.empty_bookmarks_title),
            subtitle = stringResource(R.string.empty_bookmarks_desc),
            modifier = modifier
        )
    } else {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(bookmarks, key = { it.id }) { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectBookmark(item) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                val dotColor = when (item.color) {
                                    HighlightColor.YELLOW -> Color(0xFFFFD54F)
                                    HighlightColor.GREEN -> Color(0xFF81C784)
                                    HighlightColor.BLUE -> Color(0xFF64B5F6)
                                    HighlightColor.CORAL -> Color(0xFFE57373)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${item.bookName} ${item.chapter}:${item.verse}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = item.translationId,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = { onShare(item) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = stringResource(R.string.action_share),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(item.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete_bookmark),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "«${item.text}»",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic
                            ),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
