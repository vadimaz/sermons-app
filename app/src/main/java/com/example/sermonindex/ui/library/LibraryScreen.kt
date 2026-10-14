package com.example.sermonindex.ui.library

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil3.compose.AsyncImage
import com.example.sermonindex.SermonApp
import com.example.sermonindex.data.local.entities.FavoriteSermonEntity
import com.example.sermonindex.data.local.entities.FtsSermonResult
import com.example.sermonindex.data.local.entities.HistoryEntity
import com.example.sermonindex.data.model.SermonDto
import com.example.sermonindex.data.sync.CatalogSyncWorker
import com.example.sermonindex.ui.components.EmptyStateView
import com.example.sermonindex.ui.components.SearchTextField
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onSermonSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = SermonApp.instance.repository
    val playbackManager = SermonApp.instance.playbackManager
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Favorites, 1: History, 2: Offline Catalog

    val favorites by repository.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val history by repository.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val ftsCount by repository.ftsIndexedCount.collectAsStateWithLifecycle(initialValue = 0)

    val workInfos by WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow(CatalogSyncWorker.WORK_NAME)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val activeWorkInfo = workInfos.firstOrNull()
    val isSyncing = activeWorkInfo?.state == WorkInfo.State.RUNNING
    val syncProgress = activeWorkInfo?.progress?.getInt(CatalogSyncWorker.KEY_PROGRESS, 0) ?: 0
    val syncCount = activeWorkInfo?.progress?.getInt(CatalogSyncWorker.KEY_COUNT, 0) ?: 0

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                text = { Text("Favorites (${favorites.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                icon = { Icon(Icons.Default.History, contentDescription = null) },
                text = { Text("History (${history.size})") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                text = { Text("Offline Search") }
            )
        }

        when (selectedTab) {
            0 -> FavoritesList(
                favorites = favorites,
                onPlay = { fav ->
                    scope.launch {
                        repository.getSermonDetails(fav.id).onSuccess { detail ->
                            playbackManager.playSermon(
                                SermonDto(
                                    id = detail.id,
                                    title = detail.title,
                                    speaker = detail.speaker,
                                    speakerImage = detail.speakerImage,
                                    mp3Url = detail.mp3Url,
                                    cdnMp3Url = detail.cdnMp3Url,
                                    hasAudio = detail.hasAudio,
                                    duration = detail.duration
                                )
                            )
                        }
                    }
                },
                onSelect = onSermonSelect,
                onRemove = { fav ->
                    scope.launch {
                        repository.toggleFavorite(
                            SermonDto(
                                id = fav.id,
                                title = fav.title,
                                speaker = fav.speaker,
                                speakerImage = fav.speakerImage,
                                duration = fav.duration
                            )
                        )
                    }
                }
            )
            1 -> HistoryList(
                history = history,
                onPlay = { item ->
                    scope.launch {
                        repository.getSermonDetails(item.sermonId).onSuccess { detail ->
                            playbackManager.playSermon(
                                SermonDto(
                                    id = detail.id,
                                    title = detail.title,
                                    speaker = detail.speaker,
                                    speakerImage = detail.speakerImage,
                                    mp3Url = detail.mp3Url,
                                    cdnMp3Url = detail.cdnMp3Url,
                                    hasAudio = detail.hasAudio,
                                    duration = detail.duration
                                ),
                                startPositionMs = item.lastPositionMs
                            )
                        }
                    }
                },
                onSelect = onSermonSelect,
                onClearAll = {
                    scope.launch {
                        SermonApp.instance.database.historyDao.clearHistory()
                    }
                }
            )
            2 -> OfflineSearchTab(
                indexedCount = ftsCount,
                isSyncing = isSyncing,
                syncProgress = syncProgress,
                syncCount = syncCount,
                onStartSync = {
                    val workRequest = OneTimeWorkRequestBuilder<CatalogSyncWorker>()
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()
                    WorkManager.getInstance(context).enqueueUniqueWork(
                        CatalogSyncWorker.WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
                },
                onClearIndex = {
                    scope.launch {
                        repository.clearOfflineIndex()
                    }
                },
                onSermonSelect = onSermonSelect
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
    onStartSync: () -> Unit,
    onClearIndex: () -> Unit,
    onSermonSelect: (String) -> Unit
) {
    val repository = SermonApp.instance.repository
    val playbackManager = SermonApp.instance.playbackManager
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<FtsSermonResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && indexedCount > 0) {
            isSearching = true
            searchResults = repository.searchOffline(searchQuery)
            isSearching = false
        } else {
            searchResults = emptyList()
        }
    }

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
                    onQueryChange = { searchQuery = it },
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

            items(searchResults) { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSermonSelect(result.id) },
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
                            onClick = {
                                scope.launch {
                                    repository.getSermonDetails(result.id).onSuccess { detail ->
                                        playbackManager.playSermon(
                                            SermonDto(
                                                id = detail.id,
                                                title = detail.title,
                                                speaker = detail.speaker,
                                                speakerImage = detail.speakerImage,
                                                mp3Url = detail.mp3Url,
                                                cdnMp3Url = detail.cdnMp3Url,
                                                hasAudio = detail.hasAudio,
                                                duration = detail.duration
                                            )
                                        )
                                    }
                                }
                            },
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
    favorites: List<FavoriteSermonEntity>,
    onPlay: (FavoriteSermonEntity) -> Unit,
    onSelect: (String) -> Unit,
    onRemove: (FavoriteSermonEntity) -> Unit
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
            items(favorites) { item ->
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
    history: List<HistoryEntity>,
    onPlay: (HistoryEntity) -> Unit,
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

            items(history) { item ->
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
                        val progress = if (item.durationMs > 0) {
                            (item.lastPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        LinearProgressIndicator(
                            progress = { progress },
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
