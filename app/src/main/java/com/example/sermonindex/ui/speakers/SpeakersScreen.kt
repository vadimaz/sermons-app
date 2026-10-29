package com.example.sermonindex.ui.speakers

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
import com.example.sermonindex.SpeakersRoute
import com.example.sermonindex.theme.SermonIndexTheme
import com.example.sermonindex.theme.ThemePreviews
import com.example.sermonindex.ui.components.EmptyStateView
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import com.example.sermonindex.ui.components.PreviewFixtures
import com.example.sermonindex.ui.components.SearchTextField
import com.example.sermonindex.ui.speakers.components.SpeakerRow
import kotlinx.coroutines.flow.Flow

/**
 * Stateful entry point for SpeakersScreen.
 */
@Composable
fun SpeakersScreen(
    onSpeakerSelect: (String) -> Unit,
    onBackClick: (() -> Unit)? = null,
    reselectEvents: Flow<NavKey>? = null,
    modifier: Modifier = Modifier
) {
    val viewModel: SpeakersViewModel = hiltViewModel()

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SpeakersContent(
        state = state,
        onEvent = { event ->
            when (event) {
                is SpeakersUiEvent.SelectSpeaker -> onSpeakerSelect(event.slug)
                else -> viewModel.onEvent(event)
            }
        },
        onBackClick = onBackClick,
        reselectEvents = reselectEvents,
        modifier = modifier
    )
}

/**
 * Stateless SpeakersContent.
 */
@Composable
fun SpeakersContent(
    state: SpeakersUiState,
    onEvent: (SpeakersUiEvent) -> Unit,
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
            if (route == SpeakersRoute) {
                listState.animateScrollToItem(0)
            }
        }
    }

    if (state.isLoading) {
        LoadingView(modifier = modifier, text = stringResource(R.string.loading_preachers_directory))
        return
    }

    if (state.errorMessage != null && state.speakers.isEmpty()) {
        ErrorView(
            message = state.errorMessage,
            onRetry = { onEvent(SpeakersUiEvent.Retry) },
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
                    text = stringResource(R.string.home_speakers_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Search bar
        Box(modifier = Modifier.padding(16.dp)) {
            SearchTextField(
                query = state.searchQuery,
                onQueryChange = { onEvent(SpeakersUiEvent.UpdateSearchQuery(it)) },
                placeholder = stringResource(R.string.search_preachers_placeholder, state.speakers.size)
            )
        }

        val filtered = state.filteredSpeakers
        if (filtered.isEmpty()) {
            EmptyStateView(
                title = stringResource(R.string.no_preachers_found_title),
                subtitle = stringResource(R.string.no_preachers_found_subtitle)
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
                    contentType = { "speaker" }
                ) { speaker ->
                    SpeakerRow(
                        speaker = speaker,
                        onClick = { onEvent(SpeakersUiEvent.SelectSpeaker(speaker.slug)) }
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SpeakersContentPreview() {
    SermonIndexTheme {
        Surface {
            SpeakersContent(
                state = SpeakersUiState(
                    isLoading = false,
                    speakers = listOf(
                        PreviewFixtures.previewSpeaker,
                        PreviewFixtures.previewSpeaker.copy(
                            id = "spk_2",
                            name = "Leonard Ravenhill",
                            slug = "leonard-ravenhill",
                            sermonCount = 280
                        )
                    )
                ),
                onEvent = {}
            )
        }
    }
}
