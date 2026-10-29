package com.example.sermonindex.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.sermonindex.HomeRoute
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.theme.SermonIndexTheme
import com.example.sermonindex.theme.ThemePreviews
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import com.example.sermonindex.ui.components.PreviewFixtures
import com.example.sermonindex.ui.components.SermonListItem
import com.example.sermonindex.ui.home.components.ContinueListeningShelf
import com.example.sermonindex.ui.home.components.HeroCarousel
import com.example.sermonindex.ui.home.components.RandomSermonsShelf
import com.example.sermonindex.ui.home.components.SectionHeader
import com.example.sermonindex.ui.home.components.SpeakersShelf
import com.example.sermonindex.ui.home.components.TopicsShelf
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

@ThemePreviews
@Composable
private fun HomeContentPreview() {
    SermonIndexTheme {
        Surface {
            HomeContent(
                state = HomeUiState(
                    isLoading = false,
                    popularSermons = listOf(
                        PreviewFixtures.previewAudioSermon,
                        PreviewFixtures.previewTextSermon
                    ),
                    speakers = listOf(
                        PreviewFixtures.previewSpeaker
                    ),
                    topics = listOf(
                        PreviewFixtures.previewTopic
                    ),
                    history = listOf(
                        PreviewFixtures.previewListeningHistory
                    )
                ),
                onEvent = {}
            )
        }
    }
}
