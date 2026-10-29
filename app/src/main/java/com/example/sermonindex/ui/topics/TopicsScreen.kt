package com.example.sermonindex.ui.topics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.sermonindex.R
import com.example.sermonindex.TopicsRoute
import com.example.sermonindex.theme.SermonIndexTheme
import com.example.sermonindex.theme.ThemePreviews
import com.example.sermonindex.ui.components.EmptyStateView
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import com.example.sermonindex.ui.components.PreviewFixtures
import com.example.sermonindex.ui.components.SearchTextField
import com.example.sermonindex.ui.topics.components.TopicRow
import kotlinx.coroutines.flow.Flow

/**
 * Stateful entry point for TopicsScreen.
 */
@Composable
fun TopicsScreen(
    onTopicSelect: (String) -> Unit,
    onBackClick: (() -> Unit)? = null,
    reselectEvents: Flow<NavKey>? = null,
    modifier: Modifier = Modifier
) {
    val viewModel: TopicsViewModel = hiltViewModel()

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TopicsContent(
        state = state,
        onEvent = { event ->
            when (event) {
                is TopicsUiEvent.SelectTopic -> onTopicSelect(event.slug)
                else -> viewModel.onEvent(event)
            }
        },
        onBackClick = onBackClick,
        reselectEvents = reselectEvents,
        modifier = modifier
    )
}

/**
 * Stateless TopicsContent.
 */
@Composable
fun TopicsContent(
    state: TopicsUiState,
    onEvent: (TopicsUiEvent) -> Unit,
    onBackClick: (() -> Unit)? = null,
    reselectEvents: Flow<NavKey>? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.searchQuery) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(reselectEvents) {
        reselectEvents?.collect { route ->
            if (route == TopicsRoute) {
                listState.animateScrollToItem(0)
            }
        }
    }

    if (state.isLoading) {
        LoadingView(modifier = modifier, text = stringResource(R.string.loading_thematic_catalog))
        return
    }

    if (state.errorMessage != null && state.topics.isEmpty()) {
        ErrorView(
            message = state.errorMessage,
            onRetry = { onEvent(TopicsUiEvent.Retry) },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        if (onBackClick != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.home_topics_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Box(modifier = Modifier.padding(16.dp)) {
            SearchTextField(
                query = state.searchQuery,
                onQueryChange = { onEvent(TopicsUiEvent.UpdateSearchQuery(it)) },
                placeholder = stringResource(R.string.search_topics_placeholder, state.topics.size)
            )
        }

        val filtered = state.filteredTopics
        if (filtered.isEmpty()) {
            EmptyStateView(
                title = stringResource(R.string.no_topics_found_title),
                subtitle = stringResource(R.string.no_topics_found_subtitle)
            )
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filtered,
                    key = { it.slug },
                    contentType = { "topic" }
                ) { topic ->
                    TopicRow(
                        topic = topic,
                        onClick = { onEvent(TopicsUiEvent.SelectTopic(topic.slug)) }
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun TopicsContentPreview() {
    SermonIndexTheme {
        Surface {
            TopicsContent(
                state = TopicsUiState(
                    isLoading = false,
                    topics = listOf(
                        PreviewFixtures.previewTopic,
                        PreviewFixtures.previewTopic.copy(
                            id = "top_2",
                            name = "Holy Spirit Revival",
                            slug = "holy-spirit-revival",
                            count = 145
                        )
                    )
                ),
                onEvent = {}
            )
        }
    }
}
