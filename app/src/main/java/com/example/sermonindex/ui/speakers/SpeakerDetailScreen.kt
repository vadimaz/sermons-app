package com.example.sermonindex.ui.speakers

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.sermonindex.SermonApp
import com.example.sermonindex.data.model.SermonDto
import com.example.sermonindex.data.model.SpeakerDto
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import com.example.sermonindex.ui.components.SermonListItem
import kotlinx.coroutines.launch

@Composable
fun SpeakerDetailScreen(
    slug: String,
    onBack: () -> Unit,
    onSermonClick: (SermonDto) -> Unit,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = SermonApp.instance.repository
    val playbackManager = SermonApp.instance.playbackManager
    val scope = rememberCoroutineScope()

    var speaker by remember { mutableStateOf<SpeakerDto?>(null) }
    var sermons by remember { mutableStateOf<List<SermonDto>>(emptyList()) }
    var isLoadingSpeaker by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val favorites by repository.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val favoriteIds = remember(favorites) { favorites.map { it.id }.toSet() }

    val listState = rememberLazyListState()

    // Load initial speaker data & page 1 sermons
    LaunchedEffect(slug) {
        isLoadingSpeaker = true
        errorMessage = null
        repository.getSpeakerDetails(slug)
            .onSuccess { sp ->
                speaker = sp
                totalPages = sp.totalPages.coerceAtLeast(1)
                // Also fetch page 1 sermons
                repository.getSpeakerSermonsPage(slug, 1)
                    .onSuccess { paged ->
                        sermons = paged.sermons
                        totalPages = paged.totalPages
                        currentPage = 1
                        isLoadingSpeaker = false
                    }
                    .onFailure {
                        isLoadingSpeaker = false
                    }
            }
            .onFailure {
                errorMessage = it.localizedMessage ?: "Failed to load preacher details."
                isLoadingSpeaker = false
            }
    }

    // Infinite scroll detection: when user scrolls near the end, load next page
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItemCount = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItemCount > 0 && lastVisibleItemIndex >= totalItemCount - 3 && !isLoadingMore && currentPage < totalPages
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            isLoadingMore = true
            val nextPage = currentPage + 1
            repository.getSpeakerSermonsPage(slug, nextPage)
                .onSuccess { paged ->
                    sermons = sermons + paged.sermons
                    currentPage = nextPage
                    isLoadingMore = false
                }
                .onFailure {
                    isLoadingMore = false
                }
        }
    }

    if (isLoadingSpeaker) {
        LoadingView(modifier = modifier, text = "Loading preacher profile...")
        return
    }

    if (errorMessage != null && speaker == null) {
        ErrorView(
            message = errorMessage ?: "Failed to load speaker",
            onRetry = {
                // Retry loading
            },
            modifier = modifier
        )
        return
    }

    val currentSpeaker = speaker ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 96.dp)
    ) {
        // App bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentSpeaker.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header with portrait & bio
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (currentSpeaker.image.isNotBlank()) {
                        AsyncImage(
                            model = currentSpeaker.image,
                            contentDescription = currentSpeaker.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(96.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentSpeaker.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${currentSpeaker.sermonCount} Sermons • ${currentSpeaker.audioCount} Audio Available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (currentSpeaker.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = currentSpeaker.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Top Topics
            if (currentSpeaker.topTopics.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Frequent Preaching Themes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(
                        contentPadding = PaddingValues(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentSpeaker.topTopics) { topic ->
                            FilterChip(
                                selected = false,
                                onClick = { onTopicClick(topic.slug) },
                                label = { Text("${topic.name} (${topic.count})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }

            // Sermons header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sermons (${sermons.size}${if (currentPage < totalPages) "+" else ""})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Sermon list items
            items(sermons) { sermon ->
                SermonListItem(
                    sermon = sermon,
                    onPlayClick = { playbackManager.playSermon(sermon) },
                    onItemClick = { onSermonClick(sermon) },
                    onFavoriteToggle = { scope.launch { repository.toggleFavorite(sermon) } },
                    isFavorite = favoriteIds.contains(sermon.id)
                )
            }

            // Loading more footer
            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
