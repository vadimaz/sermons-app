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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImage
import com.example.sermonindex.HomeRoute
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.ListeningHistory
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.model.Topic
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import com.example.sermonindex.ui.components.SermonListItem
import kotlinx.coroutines.flow.Flow

/**
 * Stateful entry point for HomeScreen.
 * Collects state from [HomeViewModel] and routes user interactions via [HomeUiEvent].
 */
@Composable
fun HomeScreen(
    onSermonSelect: (Sermon) -> Unit,
    onSpeakerSelect: (Speaker) -> Unit,
    onSeeAllSpeakers: () -> Unit,
    onTopicSelect: (String) -> Unit,
    onSeeAllTopics: () -> Unit,
    reselectEvents: Flow<NavKey>? = null,
    onTextSermonSelect: (Sermon) -> Unit = onSermonSelect,
    modifier: Modifier = Modifier
) {
    val viewModel: HomeViewModel = hiltViewModel()

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        state = state,
        onEvent = { event ->
            when (event) {
                is HomeUiEvent.SelectSermon -> onSermonSelect(event.sermon)
                is HomeUiEvent.ReadSermon -> onTextSermonSelect(event.sermon)
                is HomeUiEvent.SelectTopic -> onTopicSelect(event.slug)
                else -> viewModel.onEvent(event)
            }
        },
        onSpeakerSelect = onSpeakerSelect,
        onSeeAllSpeakers = onSeeAllSpeakers,
        onTopicSelect = onTopicSelect,
        onSeeAllTopics = onSeeAllTopics,
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
    onSpeakerSelect: (Speaker) -> Unit = {},
    onSeeAllSpeakers: () -> Unit = {},
    onTopicSelect: (String) -> Unit = {},
    onSeeAllTopics: () -> Unit = {},
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
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Hero Carousel (Featured Sermons at the very top)
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

        // 2. Continue Listening Shelf (History)
        if (state.history.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
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

        // 3. Speakers Shelf (Apple Music style header + circular cards)
        if (state.speakers.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    eyebrow = stringResource(R.string.home_speakers_eyebrow),
                    title = stringResource(R.string.home_speakers_title),
                    onClick = onSeeAllSpeakers
                )
                SpeakersShelf(
                    speakers = state.speakers.take(12),
                    onSpeakerClick = onSpeakerSelect
                )
            }
        }

        // 4. Topics Shelf (Apple Music style header + rich cards)
        if (state.topics.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    eyebrow = stringResource(R.string.home_topics_eyebrow),
                    title = stringResource(R.string.home_topics_title),
                    onClick = onSeeAllTopics
                )
                TopicsShelf(
                    topics = state.topics.take(12),
                    onTopicClick = { onTopicSelect(it.slug) }
                )
            }
        }

        // 5. Vintage Treasures Shelf (Random Shards)
        if (state.randomSermons.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                RandomSermonsShelf(
                    sermons = state.randomSermons,
                    isLoading = state.isRandomLoading,
                    onShuffleClick = { onEvent(HomeUiEvent.ShuffleRandomSermons) },
                    onPlayClick = { onEvent(HomeUiEvent.PlaySermon(it)) },
                    onReadClick = { onEvent(HomeUiEvent.ReadSermon(it)) },
                    onSermonClick = { onEvent(HomeUiEvent.SelectSermon(it)) }
                )
            }
        }

        // 6. Most Listened Sermons List
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.most_listened_sermons),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        items(
            items = state.popularSermons.drop(6),
            key = { it.id },
            contentType = { "sermon" }
        ) { sermon ->
            SermonListItem(
                sermon = sermon,
                onPlayClick = { onEvent(HomeUiEvent.PlaySermon(sermon)) },
                onReadClick = { onEvent(HomeUiEvent.ReadSermon(sermon)) },
                onItemClick = { onEvent(HomeUiEvent.SelectSermon(sermon)) },
                onFavoriteToggle = { onEvent(HomeUiEvent.ToggleFavorite(sermon)) },
                isFavorite = state.favoriteIds.contains(sermon.id),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    eyebrow: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = eyebrow.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SpeakersShelf(
    speakers: List<Speaker>,
    onSpeakerClick: (Speaker) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = speakers,
            key = { it.slug },
            contentType = { "speaker_card" }
        ) { speaker ->
            SpeakerCard(
                speaker = speaker,
                onClick = { onSpeakerClick(speaker) }
            )
        }
    }
}

@Composable
private fun SpeakerCard(
    speaker: Speaker,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(108.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (speaker.image.isNotBlank()) {
                AsyncImage(
                    model = speaker.image,
                    contentDescription = speaker.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = speaker.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (speaker.sermonCount > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.sermons_count, speaker.sermonCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TopicsShelf(
    topics: List<Topic>,
    onTopicClick: (Topic) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = topics,
            key = { it.slug },
            contentType = { "topic_card" }
        ) { topic ->
            TopicCard(
                topic = topic,
                onClick = { onTopicClick(topic) }
            )
        }
    }
}

@Composable
private fun TopicCard(
    topic: Topic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .width(160.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Label,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = topic.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (topic.count > 0) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = stringResource(R.string.sermons_count, topic.count),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
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
        items(
            items = sermons,
            key = { it.id },
            contentType = { "hero_sermon" }
        ) { sermon ->
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
                    val isTextOnly = sermon.isTextOnly
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sermon.speaker,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isTextOnly) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isTextOnly) {
                            Text(
                                text = stringResource(R.string.badge_text_sermon),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (sermon.duration.isNotBlank()) {
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
                            containerColor = if (isTextOnly) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isTextOnly) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.PlayArrow,
                            contentDescription = stringResource(if (isTextOnly) R.string.read_sermon else R.string.play),
                            tint = if (isTextOnly) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RandomSermonsShelf(
    sermons: List<Sermon>,
    isLoading: Boolean,
    onShuffleClick: () -> Unit,
    onPlayClick: (Sermon) -> Unit,
    onReadClick: (Sermon) -> Unit,
    onSermonClick: (Sermon) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_random_eyebrow).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_random_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            FilledIconButton(
                onClick = onShuffleClick,
                enabled = !isLoading,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.size(36.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = stringResource(R.string.shuffle_random_sermons),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = sermons,
                key = { it.id },
                contentType = { "random_sermon" }
            ) { sermon ->
                RandomSermonCard(
                    sermon = sermon,
                    onPlayClick = { onPlayClick(sermon) },
                    onReadClick = { onReadClick(sermon) },
                    onClick = { onSermonClick(sermon) }
                )
            }
        }
    }
}

@Composable
private fun RandomSermonCard(
    sermon: Sermon,
    onPlayClick: () -> Unit,
    onReadClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTextOnly = sermon.isTextOnly
    Card(
        modifier = modifier
            .width(260.dp)
            .height(130.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = sermon.speakerImage.ifBlank { sermon.thumbnailUrl }
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = sermon.speaker,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (isTextOnly) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = sermon.speaker,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isTextOnly) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sermon.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isTextOnly) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.badge_text_sermon),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (sermon.duration.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = sermon.duration,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    FilledIconButton(
                        onClick = if (isTextOnly) onReadClick else onPlayClick,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isTextOnly) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isTextOnly) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.PlayArrow,
                            contentDescription = stringResource(if (isTextOnly) R.string.read_sermon else R.string.play),
                            tint = if (isTextOnly) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
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
        items(
            items = history,
            key = { it.sermonId },
            contentType = { "history_card" }
        ) { item ->
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
