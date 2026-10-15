package com.example.sermonindex.ui.speakers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.ui.components.EmptyStateView
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import com.example.sermonindex.ui.components.SearchTextField

/**
 * Stateful entry point for SpeakersScreen.
 */
@Composable
fun SpeakersScreen(
    onSpeakerSelect: (String) -> Unit,
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
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        LoadingView(modifier = modifier, text = "Loading preachers directory...")
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
        // Search bar
        Box(modifier = Modifier.padding(16.dp)) {
            SearchTextField(
                query = state.searchQuery,
                onQueryChange = { onEvent(SpeakersUiEvent.UpdateSearchQuery(it)) },
                placeholder = "Search among ${state.speakers.size} preachers..."
            )
        }

        val filtered = state.filteredSpeakers
        if (filtered.isEmpty()) {
            EmptyStateView(
                title = "No preachers found",
                subtitle = "Try adjusting your search criteria."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.slug }) { speaker ->
                    SpeakerRow(
                        speaker = speaker,
                        onClick = { onEvent(SpeakersUiEvent.SelectSpeaker(speaker.slug)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeakerRow(
    speaker: Speaker,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            if (speaker.image.isNotBlank()) {
                AsyncImage(
                    model = speaker.image,
                    contentDescription = speaker.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = speaker.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${speaker.sermonCount} sermons",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
