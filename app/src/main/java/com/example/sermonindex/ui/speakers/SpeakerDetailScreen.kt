package com.example.sermonindex.ui.speakers

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import com.example.sermonindex.ui.components.SermonListItem

/**
 * Stateful entry point for SpeakerDetailScreen.
 */
@Composable
fun SpeakerDetailScreen(
    slug: String,
    onBack: () -> Unit,
    onSermonClick: (Sermon) -> Unit,
    onTextSermonClick: (Sermon) -> Unit = onSermonClick,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SpeakerDetailViewModel = hiltViewModel(key = "speaker_$slug") { factory: SpeakerDetailViewModel.Factory ->
        factory.create(slug)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pagedSermons = viewModel.sermonsPagingFlow.collectAsLazyPagingItems()

    SpeakerDetailContent(
        state = state,
        sermons = pagedSermons,
        onEvent = { event ->
            when (event) {
                is SpeakerDetailUiEvent.ClickSermon -> onSermonClick(event.sermon)
                is SpeakerDetailUiEvent.ReadSermon -> onTextSermonClick(event.sermon)
                is SpeakerDetailUiEvent.ClickTopic -> onTopicClick(event.slug)
                else -> viewModel.onEvent(event)
            }
        },
        onBack = onBack,
        modifier = modifier
    )
}

/**
 * Stateless SpeakerDetailContent adhering to Clean Architecture.
 */
@Composable
fun SpeakerDetailContent(
    state: SpeakerDetailUiState,
    sermons: LazyPagingItems<Sermon>,
    onEvent: (SpeakerDetailUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    if (state.isLoadingSpeaker) {
        LoadingView(modifier = modifier, text = stringResource(R.string.loading_preacher_profile))
        return
    }

    if (state.errorMessage != null && state.speaker == null) {
        ErrorView(
            message = state.errorMessage,
            onRetry = {
                onEvent(SpeakerDetailUiEvent.Retry)
                sermons.retry()
            },
            modifier = modifier
        )
        return
    }

    val currentSpeaker = state.speaker ?: return

    Column(
        modifier = modifier.fillMaxSize()
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                        text = stringResource(R.string.speaker_sermon_stats, currentSpeaker.sermonCount, currentSpeaker.audioCount),
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
                        text = stringResource(R.string.frequent_preaching_themes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(
                        contentPadding = PaddingValues(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = currentSpeaker.topTopics,
                            key = { it.slug }
                        ) { topic ->
                            FilterChip(
                                selected = false,
                                onClick = { onEvent(SpeakerDetailUiEvent.ClickTopic(topic.slug)) },
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
                val totalSermonCount = currentSpeaker.sermonCount
                val countText = if (totalSermonCount > 0) {
                    stringResource(
                        R.string.sermons_header_count,
                        sermons.itemCount,
                        if (sermons.itemCount < totalSermonCount) "+" else ""
                    )
                } else {
                    stringResource(R.string.sermons_header_count, sermons.itemCount, "")
                }
                Text(
                    text = countText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Initial refresh loading for sermons
            if (sermons.itemCount == 0 && sermons.loadState.refresh is LoadState.Loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Initial refresh error for sermons
            if (sermons.itemCount == 0 && sermons.loadState.refresh is LoadState.Error) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.failed_to_load_more_sermons),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { sermons.retry() }) {
                            Text(stringResource(R.string.try_again))
                        }
                    }
                }
            }

            // Sermon list items
            items(
                count = sermons.itemCount,
                key = sermons.itemKey { it.id },
                contentType = sermons.itemContentType { "sermon" }
            ) { index ->
                val sermon = sermons[index]
                if (sermon != null) {
                    SermonListItem(
                        sermon = sermon,
                        onPlayClick = { onEvent(SpeakerDetailUiEvent.PlaySermon(sermon)) },
                        onReadClick = { onEvent(SpeakerDetailUiEvent.ReadSermon(sermon)) },
                        onItemClick = { onEvent(SpeakerDetailUiEvent.ClickSermon(sermon)) },
                        onFavoriteToggle = { onEvent(SpeakerDetailUiEvent.ToggleFavorite(sermon)) },
                        isFavorite = state.favoriteIds.contains(sermon.id)
                    )
                }
            }

            // Loading more or error footer during pagination
            when (sermons.loadState.append) {
                is LoadState.Loading -> {
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
                is LoadState.Error -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.failed_to_load_more_sermons),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { sermons.retry() }) {
                                Text(stringResource(R.string.try_again))
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}
