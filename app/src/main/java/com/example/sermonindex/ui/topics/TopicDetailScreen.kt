package com.example.sermonindex.ui.topics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.TopicScripture
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import com.example.sermonindex.ui.components.SermonListItem

private val HTML_TAG_REGEX = Regex("<[^>]*>")



/**
 * Stateful entry point for TopicDetailScreen.
 */
@Composable
fun TopicDetailScreen(
    slug: String,
    onBack: () -> Unit,
    onSermonClick: (Sermon) -> Unit,
    onTextSermonClick: (Sermon) -> Unit = onSermonClick,
    modifier: Modifier = Modifier
) {
    val viewModel: TopicDetailViewModel = hiltViewModel(key = "topic_$slug") { factory: TopicDetailViewModel.Factory ->
        factory.create(slug)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TopicDetailContent(
        state = state,
        onEvent = { event ->
            when (event) {
                is TopicDetailUiEvent.ClickSermon -> onSermonClick(event.sermon)
                is TopicDetailUiEvent.ReadSermon -> onTextSermonClick(event.sermon)
                else -> viewModel.onEvent(event)
            }
        },
        onBack = onBack,
        modifier = modifier
    )
}



/**
 * Stateless TopicDetailContent.
 */
@Composable


fun TopicDetailContent(
    state: TopicDetailUiState,
    onEvent: (TopicDetailUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        LoadingView(modifier = modifier, text = stringResource(R.string.loading_theme_insights))
        return
    }

    if (state.errorMessage != null && state.topicDetail == null) {
        ErrorView(
            message = state.errorMessage,
            onRetry = { onEvent(TopicDetailUiEvent.Retry) },
            modifier = modifier
        )
        return
    }

    val detail = state.topicDetail ?: return

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // App bar
        Surface(
            color = Color.Transparent,
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
                    text = detail.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Theme Description
            if (detail.description.isNotBlank()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.about_biblical_theme),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val cleanDescription = remember(detail.description) {
                                detail.description.replace(HTML_TAG_REGEX, "")
                            }
                            Text(
                                text = cleanDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Top Scriptures Cards (with Berean Standard Bible)
            if (detail.topScriptures.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.key_scripture_references_bsb),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(
                    items = detail.topScriptures,
                    key = { it.text },
                    contentType = { "scripture" }
                ) { scripture ->
                    ScriptureVerseCard(scripture = scripture)
                }
            }

            // Associated Sermons
            if (detail.sermons.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.preachings_on_topic_count, detail.name, detail.sermons.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(
                    items = detail.sermons,
                    key = { it.id },
                    contentType = { "sermon" }
                ) { sermon ->
                    SermonListItem(
                        sermon = sermon,
                        onPlayClick = { onEvent(TopicDetailUiEvent.PlaySermon(sermon)) },
                        onReadClick = { onEvent(TopicDetailUiEvent.ReadSermon(sermon)) },
                        onItemClick = { onEvent(TopicDetailUiEvent.ClickSermon(sermon)) },
                        onFavoriteToggle = { onEvent(TopicDetailUiEvent.ToggleFavorite(sermon)) },
                        isFavorite = state.favoriteIds.contains(sermon.id)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScriptureVerseCard(
    scripture: TopicScripture,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.FormatQuote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = scripture.text,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (scripture.count > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = stringResource(R.string.sermons_count, scripture.count),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (scripture.bsb.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = scripture.bsb,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
