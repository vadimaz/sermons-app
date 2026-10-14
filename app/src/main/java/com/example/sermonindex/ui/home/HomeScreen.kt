package com.example.sermonindex.ui.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.sermonindex.SermonApp
import com.example.sermonindex.data.local.entities.HistoryEntity
import com.example.sermonindex.data.model.SermonDto
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import com.example.sermonindex.ui.components.SermonListItem
import kotlinx.coroutines.launch

private val QUICK_TOPICS = listOf(
    "Prayer" to "prayer",
    "Revival" to "revival",
    "Holy Spirit" to "holy-spirit",
    "Faith" to "faith",
    "Grace" to "grace",
    "Repentance" to "repentance",
    "Evangelism" to "evangelism"
)

@Composable
fun HomeScreen(
    onSermonSelect: (SermonDto) -> Unit,
    onTopicSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = SermonApp.instance.repository
    val playbackManager = SermonApp.instance.playbackManager
    val scope = rememberCoroutineScope()

    var popularSermons by remember { mutableStateOf<List<SermonDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val historyItems by repository.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val favorites by repository.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val favoriteIds = remember(favorites) { favorites.map { it.id }.toSet() }

    fun loadData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            repository.getPopularSermons()
                .onSuccess {
                    popularSermons = it
                    isLoading = false
                }
                .onFailure {
                    errorMessage = it.localizedMessage ?: "Failed to load popular sermons."
                    isLoading = false
                }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    if (isLoading) {
        LoadingView(modifier = modifier, text = "Loading classic sermons...")
        return
    }

    if (errorMessage != null && popularSermons.isEmpty()) {
        ErrorView(
            message = errorMessage ?: "Unknown error",
            onRetry = { loadData() },
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Hero Carousel
        if (popularSermons.isNotEmpty()) {
            item {
                Text(
                    text = "Featured Sermons",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                )
                HeroCarousel(
                    sermons = popularSermons.take(6),
                    onPlayClick = { playbackManager.playSermon(it) },
                    onSermonClick = onSermonSelect
                )
            }
        }

        // Quick Topics
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Explore Themes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(QUICK_TOPICS) { (name, slug) ->
                    FilterChip(
                        selected = false,
                        onClick = { onTopicSelect(slug) },
                        label = { Text(name) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        // Continue Listening
        if (historyItems.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Continue Listening",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                ContinueListeningShelf(
                    history = historyItems.take(5),
                    onPlayHistory = { history ->
                        val matching = popularSermons.find { it.id == history.sermonId }
                        if (matching != null) {
                            playbackManager.playSermon(matching, startPositionMs = history.lastPositionMs)
                        } else {
                            scope.launch {
                                repository.getSermonDetails(history.sermonId).onSuccess { detail ->
                                    val sermonDto = SermonDto(
                                        id = detail.id,
                                        title = detail.title,
                                        speaker = detail.speaker,
                                        speakerImage = detail.speakerImage,
                                        mp3Url = detail.mp3Url,
                                        cdnMp3Url = detail.cdnMp3Url,
                                        hasAudio = detail.hasAudio,
                                        duration = detail.duration
                                    )
                                    playbackManager.playSermon(sermonDto, startPositionMs = history.lastPositionMs)
                                }
                            }
                        }
                    }
                )
            }
        }

        // Popular Sermons List
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Most Listened Sermons",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        items(popularSermons.drop(6)) { sermon ->
            SermonListItem(
                sermon = sermon,
                onPlayClick = { playbackManager.playSermon(sermon) },
                onItemClick = { onSermonSelect(sermon) },
                onFavoriteToggle = { scope.launch { repository.toggleFavorite(sermon) } },
                isFavorite = favoriteIds.contains(sermon.id),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun HeroCarousel(
    sermons: List<SermonDto>,
    onPlayClick: (SermonDto) -> Unit,
    onSermonClick: (SermonDto) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sermons) { sermon ->
            HeroCard(
                sermon = sermon,
                onPlayClick = { onPlayClick(sermon) },
                onClick = { onSermonClick(sermon) }
            )
        }
    }
}

@Composable
private fun HeroCard(
    sermon: SermonDto,
    onPlayClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(280.dp)
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageUrl = sermon.speakerImage.ifBlank { sermon.thumbnailUrl }
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = sermon.speaker,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Scrim overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = sermon.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sermon.speaker,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (sermon.duration.isNotBlank()) {
                            Text(
                                text = sermon.duration,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray
                            )
                        }
                    }

                    FilledIconButton(
                        onClick = onPlayClick,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueListeningShelf(
    history: List<HistoryEntity>,
    onPlayHistory: (HistoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(history) { item ->
            Card(
                modifier = Modifier
                    .width(220.dp)
                    .clickable { onPlayHistory(item) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.speaker,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
