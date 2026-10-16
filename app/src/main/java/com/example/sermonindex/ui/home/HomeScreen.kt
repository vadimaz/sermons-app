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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImage
import com.example.sermonindex.HomeRoute
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.ListeningHistory
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import com.example.sermonindex.ui.components.SermonListItem
import kotlinx.coroutines.flow.Flow

private data class QuickTopicItem(val nameResId: Int, val slug: String)

private val QUICK_TOPICS = listOf(
    QuickTopicItem(R.string.quick_topic_prayer, "prayer"),
    QuickTopicItem(R.string.quick_topic_revival, "revival"),
    QuickTopicItem(R.string.quick_topic_holy_spirit, "holy-spirit"),
    QuickTopicItem(R.string.quick_topic_faith, "faith"),
    QuickTopicItem(R.string.quick_topic_grace, "grace"),
    QuickTopicItem(R.string.quick_topic_repentance, "repentance"),
    QuickTopicItem(R.string.quick_topic_evangelism, "evangelism")
)

/**
 * Stateful entry point for HomeScreen.
 * Collects state from [HomeViewModel] and routes user interactions via [HomeUiEvent].
 */
@Composable
fun HomeScreen(
    onSermonSelect: (Sermon) -> Unit,
    onTopicSelect: (String) -> Unit,
    reselectEvents: Flow<NavKey>? = null,
    modifier: Modifier = Modifier
) {
    val viewModel: HomeViewModel = hiltViewModel()

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        state = state,
        onEvent = { event ->
            when (event) {
                is HomeUiEvent.SelectSermon -> onSermonSelect(event.sermon)
                is HomeUiEvent.SelectTopic -> onTopicSelect(event.slug)
                else -> viewModel.onEvent(event)
            }
        },
        reselectEvents = reselectEvents,
        modifier = modifier
    )
}

/**
 * Stateless UI content component adhering to Google MAD and Clean Architecture.
 * Allows pure Compose previews and easy instrumented UI testing.
 */
@Composable
fun HomeContent(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    reselectEvents: Flow<NavKey>? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(reselectEvents) {
        reselectEvents?.collect { route ->
            if (route == HomeRoute) {
                listState.animateScrollToItem(0)
            }
        }
    }

    if (state.isLoading) {
        LoadingView(modifier = modifier, text = stringResource(R.string.loading_classic_sermons))
        return
    }

    if (state.errorMessage != null && state.popularSermons.isEmpty()) {
        ErrorView(
            message = state.errorMessage,
            onRetry = { onEvent(HomeUiEvent.Refresh) },
            modifier = modifier
        )
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Hero Carousel
        if (state.popularSermons.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.featured_sermons),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                )
                HeroCarousel(
                    sermons = state.popularSermons.take(6),
                    onPlayClick = { onEvent(HomeUiEvent.PlaySermon(it)) },
                    onSermonClick = { onEvent(HomeUiEvent.SelectSermon(it)) }
                )
            }
        }

        // Quick Topics
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.explore_themes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(QUICK_TOPICS) { topic ->
                    FilterChip(
                        selected = false,
                        onClick = { onEvent(HomeUiEvent.SelectTopic(topic.slug)) },
                        label = { Text(stringResource(topic.nameResId)) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        // Continue Listening Shelf
        if (state.history.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.continue_listening),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                ContinueListeningShelf(
                    history = state.history.take(5),
                    onPlayHistory = { onEvent(HomeUiEvent.ResumeHistory(it)) }
                )
            }
        }

        // Most Listened Sermons List
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.most_listened_sermons),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        items(state.popularSermons.drop(6)) { sermon ->
            SermonListItem(
                sermon = sermon,
                onPlayClick = { onEvent(HomeUiEvent.PlaySermon(sermon)) },
                onItemClick = { onEvent(HomeUiEvent.SelectSermon(sermon)) },
                onFavoriteToggle = { onEvent(HomeUiEvent.ToggleFavorite(sermon)) },
                isFavorite = state.favoriteIds.contains(sermon.id),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun HeroCarousel(
    sermons: List<Sermon>,
    onPlayClick: (Sermon) -> Unit,
    onSermonClick: (Sermon) -> Unit,
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
    sermon: Sermon,
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
                            contentDescription = stringResource(R.string.play),
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
    history: List<ListeningHistory>,
    onPlayHistory: (ListeningHistory) -> Unit,
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
                        maxLines = 2,
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
