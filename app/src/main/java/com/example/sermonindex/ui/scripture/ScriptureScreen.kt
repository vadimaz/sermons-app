package com.example.sermonindex.ui.scripture

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.sermonindex.R
import com.example.sermonindex.ScriptureRoute
import com.example.sermonindex.domain.model.BibleBook
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.ui.components.EmptyStateView
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import com.example.sermonindex.ui.components.SermonListItem
import kotlinx.coroutines.flow.Flow

/**
 * Stateful entry point for ScriptureScreen.
 */
@Composable
fun ScriptureScreen(
    onSermonClick: (Sermon) -> Unit,
    onTextSermonClick: (Sermon) -> Unit = onSermonClick,
    reselectEvents: Flow<NavKey>? = null,
    onIsAtRootChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val viewModel: ScriptureViewModel = hiltViewModel()

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val isAtRoot = state.selectedBook == null
    LaunchedEffect(isAtRoot) {
        onIsAtRootChanged?.invoke(isAtRoot)

    }

    ScriptureContent(
        state = state,
        onEvent = { event ->
            when (event) {
                is ScriptureUiEvent.ClickSermon -> onSermonClick(event.sermon)
                is ScriptureUiEvent.ReadSermon -> onTextSermonClick(event.sermon)
                else -> viewModel.onEvent(event)
            }
        },
        reselectEvents = reselectEvents,
        modifier = modifier
    )
}

/**
 * Stateless ScriptureContent.
 */
@Composable
fun ScriptureContent(
    state: ScriptureUiState,
    onEvent: (ScriptureUiEvent) -> Unit,
    reselectEvents: Flow<NavKey>? = null,
    modifier: Modifier = Modifier
) {
    val booksListState = rememberLazyListState()
    val chaptersGridState = rememberLazyGridState()
    val versesGridState = rememberLazyGridState()
    val verseSermonsListState = rememberLazyListState()

    BackHandler(enabled = state.selectedBook != null) {
        if (state.selectedVerse != null) {
            onEvent(ScriptureUiEvent.ClearVerse)
        } else if (state.selectedChapter != null) {
            onEvent(ScriptureUiEvent.ClearChapter)
        } else {
            onEvent(ScriptureUiEvent.ClearBook)
        }
    }

    LaunchedEffect(reselectEvents) {
        reselectEvents?.collect { route ->
            if (route == ScriptureRoute) {
                if (state.selectedBook != null && state.selectedChapter != null && state.selectedVerse != null) {
                    verseSermonsListState.animateScrollToItem(0)
                } else if (state.selectedBook != null && state.selectedChapter != null) {
                    versesGridState.animateScrollToItem(0)
                } else if (state.selectedBook != null) {
                    chaptersGridState.animateScrollToItem(0)
                } else {
                    booksListState.animateScrollToItem(0)
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Scripture Breadcrumb Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.selectedBook != null) {
                    IconButton(
                        onClick = {
                            if (state.selectedVerse != null) {
                                onEvent(ScriptureUiEvent.ClearVerse)
                            } else if (state.selectedChapter != null) {
                                onEvent(ScriptureUiEvent.ClearChapter)
                            } else {
                                onEvent(ScriptureUiEvent.ClearBook)
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                val titleText = when {
                    state.selectedBook != null && state.selectedChapter != null && state.selectedVerse != null ->
                        "${state.selectedBook.name} ${state.selectedChapter}:${state.selectedVerse}"
                    state.selectedBook != null && state.selectedChapter != null ->
                        "${state.selectedBook.name} ${stringResource(R.string.chapter_number, state.selectedChapter)}"
                    state.selectedBook != null ->
                        "${state.selectedBook.name} (${state.selectedBook.id})"
                    else -> stringResource(R.string.scripture_explorer)
                }

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // LEVEL 4: Sermons & Scripture Quote for selected verse
        if (state.selectedBook != null && state.selectedChapter != null && state.selectedVerse != null) {
            LazyColumn(
                state = verseSermonsListState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Scripture Quote Card (Always visible at top of verse explorer)
                item(key = "verse_quote_card") {
                    ScriptureVerseQuoteCard(
                        bookName = state.selectedBook.name,
                        chapter = state.selectedChapter,
                        verse = state.selectedVerse,
                        firstTranslation = state.firstTranslation,
                        secondTranslation = state.secondTranslation,
                        firstVerseText = state.firstVerseText,
                        secondVerseText = state.secondVerseText,
                        isLoadingFirstText = state.isLoadingFirstVerseText,
                        isLoadingSecondText = state.isLoadingSecondVerseText,
                        onSelectFirstTranslation = { onEvent(ScriptureUiEvent.OpenTranslationPicker(TranslationPickerTarget.FIRST)) },
                        onSelectSecondTranslation = { onEvent(ScriptureUiEvent.OpenTranslationPicker(TranslationPickerTarget.SECOND)) }
                    )
                }

                if (state.isLoadingVerseSermons) {
                    item(key = "loading_sermons") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingView(text = stringResource(R.string.loading_verse_sermons, state.selectedBook.name, state.selectedChapter, state.selectedVerse))
                        }
                    }
                } else if (state.verseSermons.isEmpty()) {
                    item(key = "empty_sermons") {
                        EmptyStateView(
                            title = stringResource(R.string.no_sermons_found_title),
                            subtitle = stringResource(R.string.no_sermons_found_subtitle)
                        )
                    }
                } else {
                    item(key = "sermons_header") {
                        Text(
                            text = stringResource(R.string.sermons_referencing_verse, state.verseSermons.size, state.selectedBook.name, state.selectedChapter, state.selectedVerse),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(state.verseSermons, key = { it.id }) { sermon ->
                        SermonListItem(
                            sermon = sermon,
                            onPlayClick = { onEvent(ScriptureUiEvent.PlaySermon(sermon)) },
                            onReadClick = { onEvent(ScriptureUiEvent.ReadSermon(sermon)) },
                            onItemClick = { onEvent(ScriptureUiEvent.ClickSermon(sermon)) },
                            onFavoriteToggle = { onEvent(ScriptureUiEvent.ToggleFavorite(sermon)) },
                            isFavorite = state.favoriteIds.contains(sermon.id)
                        )
                    }
                }
            }
            return@Column
        }

        // LEVEL 3: Verse Heat Map Grid for selected chapter
        if (state.selectedBook != null && state.selectedChapter != null) {
            if (state.isLoadingChapter) {
                LoadingView(text = stringResource(R.string.loading_verse_sermon_counts))
            } else {
                val chapter = state.chapterData
                val heatMap = chapter?.verseHeatMap ?: emptyMap()
                val verseCount = chapter?.verseCount ?: heatMap.size
                val versesList = (1..verseCount.coerceAtLeast(1)).map { v -> v to (heatMap[v] ?: 0) }
                val maxCount = versesList.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.select_verse_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (versesList.isEmpty() || (versesList.size == 1 && versesList.first().second == 0)) {
                        EmptyStateView(
                            title = stringResource(R.string.chapter_sermons_title),
                            subtitle = stringResource(R.string.no_verse_breakdown_subtitle)
                        )
                    } else {
                        LazyVerticalGrid(
                            state = versesGridState,
                            columns = GridCells.Adaptive(minSize = 64.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(versesList, key = { it.first }) { (verseNum, count) ->
                                VerseHeatMapCell(
                                    verse = verseNum,
                                    count = count,
                                    maxCount = maxCount,
                                    onClick = { onEvent(ScriptureUiEvent.SelectVerse(verseNum)) }
                                )
                            }
                        }
                    }
                }
            }
            return@Column
        }

        // LEVEL 2: Chapter Grid for selected book
        if (state.selectedBook != null) {
            val totalChapters = state.selectedBook.chapterCount.coerceAtLeast(1)
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = stringResource(R.string.select_chapter_prompt, totalChapters),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    state = chaptersGridState,
                    columns = GridCells.Adaptive(minSize = 56.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items((1..totalChapters).toList()) { chapterNum ->
                        Surface(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onEvent(ScriptureUiEvent.SelectChapter(chapterNum)) },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$chapterNum",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            return@Column
        }

        // LEVEL 1: 66 Books Directory
        if (state.isLoadingBooks) {
            LoadingView(text = stringResource(R.string.loading_bible_books))
            return@Column
        }

        if (state.errorMessage != null && state.books.isEmpty()) {
            ErrorView(
                message = state.errorMessage,
                onRetry = { onEvent(ScriptureUiEvent.RetryBooks) }
            )
            return@Column
        }

        PrimaryTabRow(
            selectedTabIndex = state.testamentTabIndex,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = state.testamentTabIndex == 0,
                onClick = { onEvent(ScriptureUiEvent.SelectTestamentTab(0)) },
                text = { Text(stringResource(R.string.tab_all_books)) }
            )
            Tab(
                selected = state.testamentTabIndex == 1,
                onClick = { onEvent(ScriptureUiEvent.SelectTestamentTab(1)) },
                text = { Text(stringResource(R.string.tab_old_testament)) }
            )
            Tab(
                selected = state.testamentTabIndex == 2,
                onClick = { onEvent(ScriptureUiEvent.SelectTestamentTab(2)) },
                text = { Text(stringResource(R.string.tab_new_testament)) }
            )
        }

        val filteredBooks = state.filteredBooks

        LazyColumn(
            state = booksListState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredBooks, key = { it.id }) { book ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEvent(ScriptureUiEvent.SelectBook(book)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = book.id,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = book.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.chapters_count, book.chapterCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (state.isTranslationPickerOpen) {
        TranslationPickerBottomSheet(
            target = state.translationPickerTarget,
            currentTranslationId = if (state.translationPickerTarget == TranslationPickerTarget.FIRST) {
                state.firstTranslationId
            } else {
                state.secondTranslationId
            },
            availableTranslations = state.availableTranslations,
            onSelectTranslation = { translation ->
                onEvent(ScriptureUiEvent.SelectTranslation(translation, state.translationPickerTarget))
            },
            onDismiss = { onEvent(ScriptureUiEvent.DismissTranslationPicker) }
        )
    }
}

@Composable
private fun VerseHeatMapCell(
    verse: Int,
    count: Int,
    maxCount: Int,
    onClick: () -> Unit
) {
    val density = (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
    val cellColor = if (count > 0) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f + 0.75f * density)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = cellColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$verse",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (density > 0.5f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            if (count > 0) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (density > 0.5f) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
