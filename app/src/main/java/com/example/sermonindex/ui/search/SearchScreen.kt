package com.example.sermonindex.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImage
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.CatalogSyncState
import com.example.sermonindex.SearchRoute
import com.example.sermonindex.domain.model.SearchCategory
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.model.Topic
import com.example.sermonindex.ui.components.EmptyStateView
import com.example.sermonindex.ui.components.LoadingView
import com.example.sermonindex.ui.components.SermonListItem
import com.example.sermonindex.ui.search.components.CatalogSyncBanner
import com.example.sermonindex.ui.search.components.RecentSearchItem
import com.example.sermonindex.ui.search.components.SpeakerSearchItem
import com.example.sermonindex.ui.search.components.TopicSearchBadge
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun SearchScreen(
    onSpeakerSelect: (String) -> Unit,
    onTopicSelect: (String) -> Unit,
    onSermonSelect: (String, String?) -> Unit,
    onTextSermonSelect: (String, String?) -> Unit,
    reselectEvents: SharedFlow<NavKey>? = null,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SearchContent(
        state = uiState,
        onEvent = viewModel::onEvent,
        onSpeakerSelect = onSpeakerSelect,
        onTopicSelect = onTopicSelect,
        onSermonSelect = onSermonSelect,
        onTextSermonSelect = onTextSermonSelect,
        reselectEvents = reselectEvents
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchContent(
    state: SearchUiState,
    onEvent: (SearchUiEvent) -> Unit,
    onSpeakerSelect: (String) -> Unit,
    onTopicSelect: (String) -> Unit,
    onSermonSelect: (String, String?) -> Unit,
    onTextSermonSelect: (String, String?) -> Unit,
    reselectEvents: SharedFlow<NavKey>? = null,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    LaunchedEffect(reselectEvents) {
        reselectEvents?.collect { key ->
            if (key == SearchRoute) {
                listState.animateScrollToItem(0)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = state.query,
            onValueChange = { onEvent(SearchUiEvent.UpdateQuery(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_hint),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onEvent(SearchUiEvent.ClearQuery) }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear_search)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    onEvent(SearchUiEvent.ExecuteSearch(state.query))
                }
            )
        )

        // Filter Category Chips (Visible only when search query is present)
        AnimatedVisibility(
            visible = state.query.isNotBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SearchCategory.entries.toTypedArray()) { category ->
                    val selected = state.selectedCategory == category
                    val label = when (category) {
                        SearchCategory.ALL -> stringResource(R.string.search_filter_all)
                        SearchCategory.SERMONS -> stringResource(R.string.search_filter_sermons)
                        SearchCategory.SPEAKERS -> stringResource(R.string.search_filter_speakers)
                        SearchCategory.TOPICS -> stringResource(R.string.search_filter_topics)
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { onEvent(SearchUiEvent.SelectCategory(category)) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Content Area
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isSearching) {
                LoadingView(modifier = Modifier.fillMaxSize())
            } else if (state.query.isBlank()) {
                // Empty query: Show catalog sync banner, recent searches, suggestions
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Catalog Sync / Update Banner (if not dismissed or has update)
                    val showCatalogBanner = !(state.isCatalogReadyBannerDismissed &&
                        state.catalogState is CatalogSyncState.Ready &&
                        !state.catalogState.hasUpdate)

                    if (showCatalogBanner) {
                        item {
                            CatalogSyncBanner(
                                state = state.catalogState,
                                onDownloadClick = { onEvent(SearchUiEvent.DownloadCatalog) },
                                onUpdateClick = { onEvent(SearchUiEvent.DownloadCatalog) },
                                onDismissClick = { onEvent(SearchUiEvent.DismissCatalogReadyBanner) }
                            )
                        }
                    }

                    // 2. Recent Searches (if any)
                    if (state.recentQueries.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.search_recent_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(onClick = { onEvent(SearchUiEvent.ClearRecentQueries) }) {
                                        Text(text = stringResource(R.string.search_clear_recent))
                                    }
                                }

                                state.recentQueries.take(5).forEach { query ->
                                    RecentSearchItem(
                                        query = query,
                                        onClick = {
                                            keyboardController?.hide()
                                            onEvent(SearchUiEvent.ExecuteSearch(query))
                                        },
                                        onDelete = { onEvent(SearchUiEvent.DeleteRecentQuery(query)) }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Popular Speakers Shelf
                    if (state.popularSpeakers.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = stringResource(R.string.home_speakers_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(state.popularSpeakers, key = { it.id }) { speaker ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(76.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onSpeakerSelect(speaker.slug) }
                                        ) {
                                            AsyncImage(
                                                model = speaker.image,
                                                contentDescription = speaker.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(CircleShape)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = speaker.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Popular Topics Shelf
                    if (state.popularTopics.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = stringResource(R.string.home_topics_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    state.popularTopics.forEach { topic ->
                                        TopicSearchBadge(
                                            topic = topic,
                                            onClick = { onTopicSelect(topic.slug) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Active search results
                val result = state.searchResult
                val hasAnyResults = result.speakers.isNotEmpty() ||
                    result.topics.isNotEmpty() ||
                    result.sermons.isNotEmpty()

                if (!hasAnyResults) {
                    EmptyStateView(
                        title = stringResource(R.string.search_empty_title),
                        subtitle = stringResource(R.string.search_empty_desc),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // === ALL CATEGORY (GROUPED) ===
                        if (state.selectedCategory == SearchCategory.ALL) {
                            // Speakers section
                            if (result.speakers.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = stringResource(R.string.search_speakers_section),
                                        displayedCount = result.speakers.size,
                                        totalCount = result.totalSpeakerMatches,
                                        onSeeAll = { onEvent(SearchUiEvent.SelectCategory(SearchCategory.SPEAKERS)) }
                                    )
                                }
                                items(result.speakers, key = { "speaker_${it.id}" }) { speaker ->
                                    SpeakerSearchItem(
                                        speaker = speaker,
                                        onClick = {
                                            onEvent(SearchUiEvent.ExecuteSearch(state.query))
                                            onSpeakerSelect(speaker.slug)
                                        }
                                    )
                                }
                            }

                            // Topics section
                            if (result.topics.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = stringResource(R.string.search_topics_section),
                                        displayedCount = result.topics.size,
                                        totalCount = result.totalTopicMatches,
                                        onSeeAll = { onEvent(SearchUiEvent.SelectCategory(SearchCategory.TOPICS)) }
                                    )
                                }
                                item {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        result.topics.forEach { topic ->
                                            TopicSearchBadge(
                                                topic = topic,
                                                onClick = {
                                                    onEvent(SearchUiEvent.ExecuteSearch(state.query))
                                                    onTopicSelect(topic.slug)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Sermons section
                            if (result.sermons.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = stringResource(R.string.search_sermons_section),
                                        displayedCount = result.sermons.size,
                                        totalCount = result.totalSermonMatches,
                                        onSeeAll = { onEvent(SearchUiEvent.SelectCategory(SearchCategory.SERMONS)) }
                                    )
                                }
                                items(result.sermons, key = { "sermon_${it.id}" }) { sermon ->
                                    SermonListItem(
                                        sermon = sermon,
                                        isFavorite = state.favoriteIds.contains(sermon.id),
                                        onFavoriteToggle = { onEvent(SearchUiEvent.ToggleFavorite(sermon)) },
                                        onPlayClick = {
                                            onEvent(SearchUiEvent.ExecuteSearch(state.query))
                                            onEvent(SearchUiEvent.PlaySermon(sermon))
                                        },
                                        onReadClick = {
                                            onEvent(SearchUiEvent.ExecuteSearch(state.query))
                                            onTextSermonSelect(sermon.id, state.query.ifBlank { null })
                                        },
                                        onItemClick = {
                                            onEvent(SearchUiEvent.ExecuteSearch(state.query))
                                            onSermonSelect(sermon.id, state.query.ifBlank { null })
                                        },
                                        highlightQuery = state.query,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // === SPEAKERS ONLY ===
                        if (state.selectedCategory == SearchCategory.SPEAKERS) {
                            items(result.speakers, key = { "speaker_only_${it.id}" }) { speaker ->
                                SpeakerSearchItem(
                                    speaker = speaker,
                                    onClick = {
                                        onEvent(SearchUiEvent.ExecuteSearch(state.query))
                                        onSpeakerSelect(speaker.slug)
                                    }
                                )
                            }
                        }

                        // === TOPICS ONLY ===
                        if (state.selectedCategory == SearchCategory.TOPICS) {
                            item {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                    ) {
                                    result.topics.forEach { topic ->
                                        TopicSearchBadge(
                                            topic = topic,
                                            onClick = {
                                                onEvent(SearchUiEvent.ExecuteSearch(state.query))
                                                onTopicSelect(topic.slug)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // === SERMONS ONLY ===
                        if (state.selectedCategory == SearchCategory.SERMONS) {
                            items(result.sermons, key = { "sermon_only_${it.id}" }) { sermon ->
                                SermonListItem(
                                    sermon = sermon,
                                    isFavorite = state.favoriteIds.contains(sermon.id),
                                    onFavoriteToggle = { onEvent(SearchUiEvent.ToggleFavorite(sermon)) },
                                    onPlayClick = {
                                        onEvent(SearchUiEvent.ExecuteSearch(state.query))
                                        onEvent(SearchUiEvent.PlaySermon(sermon))
                                    },
                                    onReadClick = {
                                        onEvent(SearchUiEvent.ExecuteSearch(state.query))
                                        onTextSermonSelect(sermon.id, state.query.ifBlank { null })
                                    },
                                    onItemClick = {
                                        onEvent(SearchUiEvent.ExecuteSearch(state.query))
                                        onSermonSelect(sermon.id, state.query.ifBlank { null })
                                    },
                                    highlightQuery = state.query,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    displayedCount: Int,
    totalCount: Int,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (totalCount > displayedCount) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSeeAll)
                    .padding(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.search_see_all_count, totalCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


