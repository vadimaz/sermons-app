package com.example.sermonindex.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.sermonindex.domain.model.FavoriteSermon
import com.example.sermonindex.domain.model.ListeningHistory
import com.example.sermonindex.domain.model.OfflineSearchResult
import com.example.sermonindex.ui.components.EmptyStateView
import com.example.sermonindex.ui.components.SearchTextField

/**
 * Stateful entry point for LibraryScreen.
 */
@Composable
fun LibraryScreen(
    onSermonSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: LibraryViewModel = hiltViewModel()

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LibraryContent(
        state = state,
        onEvent = { event ->
            when (event) {
                is LibraryUiEvent.SelectSermon -> onSermonSelect(event.sermonId)
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier
    )
}

/**
 * Stateless LibraryContent.
 */
@Composable
fun LibraryContent(
    state: LibraryUiState,
    onEvent: (LibraryUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
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
                icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                text = { Text("Favorites (${state.favorites.size})") }
            )
            Tab(
                selected = state.selectedTab == 1,
                onClick = { onEvent(LibraryUiEvent.SelectTab(1)) },
                icon = { Icon(Icons.Default.History, contentDescription = null) },
                text = { Text("History (${state.history.size})") }
            )
            Tab(
                selected = state.selectedTab == 2,
                onClick = { onEvent(LibraryUiEvent.SelectTab(2)) },
                icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                text = { Text("Offline Search") }
            )
        }

        when (state.selectedTab) {
            0 -> FavoritesList(
                favorites = state.favorites,
                onPlay = { onEvent(LibraryUiEvent.PlayFavorite(it)) },
                onSelect = { onEvent(LibraryUiEvent.SelectSermon(it)) },
                onRemove = { onEvent(LibraryUiEvent.RemoveFavorite(it)) }
            )
            1 -> HistoryList(
                history = state.history,
                onPlay = { onEvent(LibraryUiEvent.ResumeHistory(it)) },
                onSelect = { onEvent(LibraryUiEvent.SelectSermon(it)) },
                onClearAll = { onEvent(LibraryUiEvent.ClearHistory) }
            )
            2 -> OfflineSearchTab(
                indexedCount = state.indexedCount,
                isSyncing = state.isSyncing,
                syncProgress = state.syncProgress,
                syncCount = state.syncCount,
                searchQuery = state.searchQuery,
                searchResults = state.searchResults,
                isSearching = state.isSearching,
                onSearchQueryChange = { onEvent(LibraryUiEvent.UpdateSearchQuery(it)) },
                onStartSync = { onEvent(LibraryUiEvent.StartSync) },
                onClearIndex = { onEvent(LibraryUiEvent.ClearIndex) },
                onPlayResult = { onEvent(LibraryUiEvent.PlayOfflineResult(it)) },
                onSermonSelect = { onEvent(LibraryUiEvent.SelectSermon(it)) }
            )
        }
    }
}

@Composable
private fun OfflineSearchTab(
    indexedCount: Int,
    isSyncing: Boolean,
    syncProgress: Int,
    syncCount: Int,
    searchQuery: String,
    searchResults: List<OfflineSearchResult>,
    isSearching: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onStartSync: () -> Unit,
    onClearIndex: () -> Unit,
    onPlayResult: (OfflineSearchResult) -> Unit,
    onSermonSelect: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sync & Settings Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (indexedCount > 0) Icons.Default.DownloadDone else Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Offline Full Catalog Search (~27 MB)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (indexedCount > 0) "$indexedCount sermons indexed locally" else "Index 70,000+ sermons for offline search",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Downloads and streams uncompressed JSON lines into a local SQLite FTS virtual table for instant search across titles, preachers and summaries without an internet connection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isSyncing) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Indexing catalog ($syncCount indexed)...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$syncProgress%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { syncProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onStartSync,
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (indexedCount > 0) "Re-download & Sync" else "Download Catalog")
                        }

                        if (indexedCount > 0) {
                            OutlinedButton(
                                onClick = onClearIndex,
                                enabled = !isSyncing
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }
        }

        // Search Bar (if indexed)
        if (indexedCount > 0) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SearchTextField(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = "Search across $indexedCount indexed sermons..."
                )
            }

            if (searchQuery.isNotBlank() && searchResults.isEmpty() && !isSearching) {
                item {
                    EmptyStateView(
                        title = "No results found in offline catalog",
                        subtitle = "Try searching for another preacher or sermon title."
                    )
                }
            }

            items(searchResults, key = { it.sermonId }) { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSermonSelect(result.sermonId) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = result.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = result.speaker,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (result.summary.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = result.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        FilledIconButton(
                            onClick = { onPlayResult(result) },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesList(
    favorites: List<FavoriteSermon>,
    onPlay: (FavoriteSermon) -> Unit,
    onSelect: (String) -> Unit,
    onRemove: (FavoriteSermon) -> Unit
) {
    if (favorites.isEmpty()) {
        EmptyStateView(
            title = "No favorites yet",
            subtitle = "Tap the heart icon on any sermon to save it to your personal library."
        )
    } else {
        LazyColumn(
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
                                contentDescription = "Remove",
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
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
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
    onPlay: (ListeningHistory) -> Unit,
    onSelect: (String) -> Unit,
    onClearAll: () -> Unit
) {
    if (history.isEmpty()) {
        EmptyStateView(
            title = "No playback history",
            subtitle = "Sermons you listen to will automatically appear here with saved progress."
        )
    } else {
        LazyColumn(
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
                        Text("Clear History")
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
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
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
