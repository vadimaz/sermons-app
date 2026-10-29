package com.example.sermonindex.ui.scripture

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
import androidx.compose.runtime.remember
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
import com.example.sermonindex.ui.components.EmptyStateView
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import kotlinx.coroutines.flow.Flow

/**
 * Level 1: Stateful entry point for ScriptureScreen (66 Books Directory).
 */
@Composable
fun ScriptureScreen(
    onBookSelect: (BibleBook) -> Unit,
    reselectEvents: Flow<NavKey>? = null,
    modifier: Modifier = Modifier
) {
    val viewModel: ScriptureViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ScriptureContent(
        state = state,
        onEvent = { event ->
            when (event) {
                is ScriptureUiEvent.SelectBook -> onBookSelect(event.book)
                else -> viewModel.onEvent(event)
            }
        },
        reselectEvents = reselectEvents,
        modifier = modifier
    )
}

/**
 * Level 1: Stateless ScriptureContent displaying 66 books of Old and New Testaments.
 */
@Composable
fun ScriptureContent(
    state: ScriptureUiState,
    onEvent: (ScriptureUiEvent) -> Unit,
    reselectEvents: Flow<NavKey>? = null,
    modifier: Modifier = Modifier
) {
    val booksListState = rememberLazyListState()

    LaunchedEffect(reselectEvents) {
        reselectEvents?.collect { route ->
            if (route == ScriptureRoute) {
                booksListState.animateScrollToItem(0)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        ScriptureTopBar(
            title = stringResource(R.string.scripture_explorer),
            onBack = null
        )

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
            items(
                items = filteredBooks,
                key = { it.id },
                contentType = { "bible_book" }
            ) { book ->
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
}

/**
 * Level 2: Stateless Chapter Picker Grid for a selected Bible book.
 */
@Composable
fun ScriptureChaptersScreen(
    bookId: String,
    bookName: String,
    chapterCount: Int,
    onChapterSelect: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chaptersGridState = rememberLazyGridState()
    val totalChapters = chapterCount.coerceAtLeast(1)

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        ScriptureTopBar(
            title = "$bookName ($bookId)",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
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
                items(
                    count = totalChapters,
                    key = { it + 1 },
                    contentType = { "chapter_cell" }
                ) { index ->
                    val chapterNum = index + 1
                    Surface(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onChapterSelect(chapterNum) },
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
    }
}

/**
 * Level 3: Stateful entry point for ScriptureVerseHeatMapScreen.
 */
@Composable
fun ScriptureVerseHeatMapScreen(
    bookId: String,
    bookName: String,
    chapter: Int,
    onVerseSelect: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val args = ScriptureChapterArgs(bookId, bookName, chapter)
    val viewModel: ScriptureChapterViewModel = hiltViewModel(
        key = "scripture_chapter_${bookId}_$chapter"
    ) { factory: ScriptureChapterViewModel.Factory ->
        factory.create(args)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ScriptureVerseHeatMapContent(
        state = state,
        onEvent = { event ->
            when (event) {
                is ScriptureChapterUiEvent.SelectVerse -> onVerseSelect(event.verse)
                else -> viewModel.onEvent(event)
            }
        },
        onBack = onBack,
        modifier = modifier
    )
}

/**
 * Level 3: Stateless ScriptureVerseHeatMapContent displaying verse sermon density grid.
 */
@Composable
fun ScriptureVerseHeatMapContent(
    state: ScriptureChapterUiState,
    onEvent: (ScriptureChapterUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val versesGridState = rememberLazyGridState()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        ScriptureTopBar(
            title = "${state.bookName} ${stringResource(R.string.chapter_number, state.chapter)}",
            onBack = onBack
        )

        if (state.isLoading) {
            LoadingView(text = stringResource(R.string.loading_verse_sermon_counts))
            return@Column
        }

        if (state.errorMessage != null && state.chapterData == null) {
            ErrorView(
                message = state.errorMessage,
                onRetry = { onEvent(ScriptureChapterUiEvent.Retry) }
            )
            return@Column
        }

        val chapterData = state.chapterData
        val (versesList, maxCount) = remember(chapterData) {
            val heatMap = chapterData?.verseHeatMap ?: emptyMap()
            val verseCount = (chapterData?.verseCount ?: heatMap.size).coerceAtLeast(1)
            val list = (1..verseCount).map { v -> v to (heatMap[v] ?: 0) }
            val max = list.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
            list to max
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
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
                    items(
                        items = versesList,
                        key = { it.first },
                        contentType = { "verse_heat_cell" }
                    ) { (verseNum, count) ->
                        VerseHeatMapCell(
                            verse = verseNum,
                            count = count,
                            maxCount = maxCount,
                            onClick = { onEvent(ScriptureChapterUiEvent.SelectVerse(verseNum)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScriptureTopBar(
    title: String,
    onBack: (() -> Unit)?
) {
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
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
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

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
