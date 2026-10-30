package com.example.sermonindex.ui.sermon

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.sermonindex.R
import com.example.sermonindex.theme.SermonIndexTheme
import com.example.sermonindex.theme.ThemePreviews
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import com.example.sermonindex.ui.components.PreviewFixtures
import com.example.sermonindex.ui.sermon.components.SermonNotesSection
import com.example.sermonindex.ui.sermon.components.SermonOutlineSection
import com.example.sermonindex.ui.sermon.components.SermonOverviewSection

/**
 * Stateful entry point for SermonDetailScreen.
 * Injects [SermonDetailViewModel] and collects [SermonDetailUiState].
 */
@Composable
fun SermonDetailScreen(
    sermonId: String,
    onBack: () -> Unit,
    onSpeakerClick: (String) -> Unit,
    onReadClick: (String) -> Unit = {},
    onPlayClick: () -> Unit = {},
    highlightQuery: String? = null,
    modifier: Modifier = Modifier
) {
    val viewModel: SermonDetailViewModel = hiltViewModel(key = "sermon_$sermonId") { factory: SermonDetailViewModel.Factory ->
        factory.create(sermonId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SermonDetailContent(
        state = state,
        highlightQuery = highlightQuery,
        onEvent = { event ->
            when (event) {
                is SermonDetailUiEvent.ClickSpeaker -> onSpeakerClick(event.slug)
                else -> viewModel.onEvent(event)
            }
        },
        onBack = onBack,
        onReadClick = onReadClick,
        onPlayClick = onPlayClick,
        modifier = modifier
    )
}

/**
 * Stateless SermonDetailContent adhering to Clean Architecture.
 */
@Composable
fun SermonDetailContent(
    state: SermonDetailUiState,
    onEvent: (SermonDetailUiEvent) -> Unit,
    onBack: () -> Unit,
    onReadClick: (String) -> Unit = {},
    onPlayClick: () -> Unit = {},
    highlightQuery: String? = null,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        LoadingView(modifier = modifier, text = stringResource(R.string.loading_sermon))
        return
    }

    if (state.errorMessage != null && state.detail == null) {
        ErrorView(
            message = state.errorMessage,
            onRetry = { onEvent(SermonDetailUiEvent.Retry) },
            modifier = modifier
        )
        return
    }

    val detail = state.detail ?: return
    val isTextOnly = detail.isTextOnly

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        Surface(
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }

                Text(
                    text = stringResource(R.string.sermon_details),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sermon Header: Artwork & Titles
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val imageUrl = detail.speakerImage.ifBlank { detail.thumbnailUrl }
                    if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = detail.speaker,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    } else {
                        Surface(
                            color = if (isTextOnly) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isTextOnly) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isTextOnly) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = detail.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = detail.speaker,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (isTextOnly) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isTextOnly) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.text_only_sermon_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (detail.duration.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.duration_label, detail.duration),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Action Buttons: Favorite (Heart), Play/Read (Pill Capsule), Download (audio only)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Favorite (Heart)
                    FilledTonalIconButton(
                        onClick = { onEvent(SermonDetailUiEvent.ToggleFavorite) },
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (state.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(
                                if (state.isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites
                            ),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // 2. Play or Read Button (Pill / Capsule)
                    if (isTextOnly) {
                        Button(
                            onClick = { onReadClick(detail.id) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = stringResource(R.string.read_sermon),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.read_full_sermon),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                val shouldNavigate = !state.isActiveInPlayer
                                onEvent(SermonDetailUiEvent.Play)
                                if (shouldNavigate) {
                                    onPlayClick()
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Default.PlayArrow,
                                contentDescription = stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // 3. Download (Only for audio sermons)
                    if (!isTextOnly) {
                        FilledTonalIconButton(
                            onClick = { onEvent(SermonDetailUiEvent.ToggleDownload) },
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            if (state.downloadProgress != null) {
                                CircularProgressIndicator(
                                    progress = { (state.downloadProgress.toFloat() / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else if (state.isDownloaded) {
                                Icon(
                                    imageVector = Icons.Default.DownloadDone,
                                    contentDescription = stringResource(R.string.downloaded),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = stringResource(R.string.download),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Tabs: Overview, Outline, Notes
            item {
                PrimaryTabRow(
                    selectedTabIndex = state.selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = state.selectedTab == 0,
                        onClick = { onEvent(SermonDetailUiEvent.SelectTab(0)) },
                        text = { Text(stringResource(R.string.tab_overview)) }
                    )
                    Tab(
                        selected = state.selectedTab == 1,
                        onClick = { onEvent(SermonDetailUiEvent.SelectTab(1)) },
                        text = { Text(stringResource(R.string.tab_outline)) }
                    )
                    Tab(
                        selected = state.selectedTab == 2,
                        onClick = { onEvent(SermonDetailUiEvent.SelectTab(2)) },
                        text = { Text(stringResource(R.string.tab_notes)) }
                    )
                }
            }

            // Tab Content
            item {
                when (state.selectedTab) {
                    0 -> SermonOverviewSection(detail = detail, highlightQuery = highlightQuery)
                    1 -> SermonOutlineSection(outline = detail.outline)
                    2 -> SermonNotesSection(
                        quotes = detail.quotes,
                        faq = detail.faq,
                        points = detail.applicationPoints
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SermonDetailContentPreview() {
    SermonIndexTheme {
        Surface {
            SermonDetailContent(
                state = SermonDetailUiState(
                    isLoading = false,
                    detail = PreviewFixtures.previewSermonDetail,
                    isFavorite = true,
                    isDownloaded = false,
                    isPlaying = false
                ),
                onEvent = {},
                onBack = {}
            )
        }
    }
}
