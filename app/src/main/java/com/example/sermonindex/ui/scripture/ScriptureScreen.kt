package com.example.sermonindex.ui.scripture

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sermonindex.SermonApp
import com.example.sermonindex.data.model.ScriptureBookDto
import com.example.sermonindex.data.model.ScriptureChapterDto
import com.example.sermonindex.data.model.SermonDto
import com.example.sermonindex.data.model.VerseCountDto
import com.example.sermonindex.ui.components.EmptyStateView
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import com.example.sermonindex.ui.components.SermonListItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptureScreen(
    onSermonClick: (SermonDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = SermonApp.instance.repository
    val playbackManager = SermonApp.instance.playbackManager
    val scope = rememberCoroutineScope()

    var books by remember { mutableStateOf<List<ScriptureBookDto>>(emptyList()) }
    var isLoadingBooks by remember { mutableStateOf(true) }
    var booksError by remember { mutableStateOf<String?>(null) }

    // Navigation state within Scripture Explorer:
    // Level 1: selectedBook == null
    // Level 2: selectedBook != null && selectedChapter == null
    // Level 3: selectedBook != null && selectedChapter != null
    var selectedBook by remember { mutableStateOf<ScriptureBookDto?>(null) }
    var selectedChapter by remember { mutableStateOf<Int?>(null) }
    var selectedVerse by remember { mutableStateOf<Int?>(null) }

    var chapterData by remember { mutableStateOf<ScriptureChapterDto?>(null) }
    var isLoadingChapter by remember { mutableStateOf(false) }

    var verseSermons by remember { mutableStateOf<List<SermonDto>>(emptyList()) }
    var isLoadingVerseSermons by remember { mutableStateOf(false) }

    var testamentTabIndex by remember { mutableIntStateOf(0) } // 0: All, 1: OT, 2: NT

    val favorites by repository.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val favoriteIds = remember(favorites) { favorites.map { it.id }.toSet() }

    LaunchedEffect(Unit) {
        isLoadingBooks = true
        repository.getScriptureBooks()
            .onSuccess {
                books = it
                isLoadingBooks = false
            }
            .onFailure {
                booksError = it.localizedMessage ?: "Failed to load scripture books."
                isLoadingBooks = false
            }
    }

    // Load chapter details (verses heat map)
    LaunchedEffect(selectedBook, selectedChapter) {
        val book = selectedBook
        val chapter = selectedChapter
        if (book != null && chapter != null) {
            isLoadingChapter = true
            repository.getScriptureChapter(book.bookId, chapter)
                .onSuccess {
                    chapterData = it
                    isLoadingChapter = false
                }
                .onFailure {
                    chapterData = ScriptureChapterDto(bookId = book.bookId, chapter = chapter)
                    isLoadingChapter = false
                }
        }
    }

    // Load sermons for selected verse
    LaunchedEffect(selectedBook, selectedChapter, selectedVerse) {
        val book = selectedBook
        val chapter = selectedChapter
        val verse = selectedVerse
        if (book != null && chapter != null && verse != null) {
            isLoadingVerseSermons = true
            repository.getScriptureVerse(book.bookId, chapter, verse)
                .onSuccess {
                    verseSermons = it
                    isLoadingVerseSermons = false
                }
                .onFailure {
                    verseSermons = emptyList()
                    isLoadingVerseSermons = false
                }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 96.dp)
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
                if (selectedBook != null) {
                    IconButton(
                        onClick = {
                            if (selectedVerse != null) {
                                selectedVerse = null
                            } else if (selectedChapter != null) {
                                selectedChapter = null
                                chapterData = null
                            } else {
                                selectedBook = null
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    selectedBook != null && selectedChapter != null && selectedVerse != null ->
                        "${selectedBook?.name} $selectedChapter:$selectedVerse"
                    selectedBook != null && selectedChapter != null ->
                        "${selectedBook?.name} Chapter $selectedChapter"
                    selectedBook != null ->
                        "${selectedBook?.name} (${selectedBook?.bookId})"
                    else -> "Scripture Explorer"
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

        // LEVEL 4: Sermons for selected verse
        if (selectedBook != null && selectedChapter != null && selectedVerse != null) {
            if (isLoadingVerseSermons) {
                LoadingView(text = "Loading sermons for ${selectedBook?.name} $selectedChapter:$selectedVerse...")
            } else if (verseSermons.isEmpty()) {
                EmptyStateView(
                    title = "No sermons found",
                    subtitle = "There are no indexed sermons specifically referencing this verse."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "${verseSermons.size} Sermons referencing ${selectedBook?.name} $selectedChapter:$selectedVerse",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(verseSermons) { sermon ->
                        SermonListItem(
                            sermon = sermon,
                            onPlayClick = { playbackManager.playSermon(sermon) },
                            onItemClick = { onSermonClick(sermon) },
                            onFavoriteToggle = { scope.launch { repository.toggleFavorite(sermon) } },
                            isFavorite = favoriteIds.contains(sermon.id)
                        )
                    }
                }
            }
            return@Column
        }

        // LEVEL 3: Verse Heat Map Grid for selected chapter
        if (selectedBook != null && selectedChapter != null) {
            if (isLoadingChapter) {
                LoadingView(text = "Loading verse sermon counts...")
            } else {
                val chapter = chapterData
                val verses = chapter?.verses ?: emptyList()
                val maxCount = verses.maxOfOrNull { it.sermonCount }?.coerceAtLeast(1) ?: 1

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        text = "Select a verse to listen to corresponding sermons:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (verses.isEmpty()) {
                        EmptyStateView(
                            title = "Chapter Sermons",
                            subtitle = "No verse breakdown is available for this chapter."
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 64.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(verses) { verseCount ->
                                VerseHeatMapCell(
                                    verse = verseCount,
                                    maxCount = maxCount,
                                    onClick = { selectedVerse = verseCount.verse }
                                )
                            }
                        }
                    }
                }
            }
            return@Column
        }

        // LEVEL 2: Chapter Grid for selected book
        if (selectedBook != null) {
            val totalChapters = selectedBook?.chapters ?: 1
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = "Select chapter (1..$totalChapters):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 56.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items((1..totalChapters).toList()) { chapterNum ->
                        Surface(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedChapter = chapterNum },
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
        if (isLoadingBooks) {
            LoadingView(text = "Loading Bible books...")
            return@Column
        }

        if (booksError != null && books.isEmpty()) {
            ErrorView(message = booksError ?: "Failed to load", onRetry = {
                scope.launch {
                    isLoadingBooks = true
                    repository.getScriptureBooks().onSuccess { books = it; isLoadingBooks = false }
                }
            })
            return@Column
        }

        PrimaryTabRow(
            selectedTabIndex = testamentTabIndex,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = testamentTabIndex == 0,
                onClick = { testamentTabIndex = 0 },
                text = { Text("All (66)") }
            )
            Tab(
                selected = testamentTabIndex == 1,
                onClick = { testamentTabIndex = 1 },
                text = { Text("Old Testament") }
            )
            Tab(
                selected = testamentTabIndex == 2,
                onClick = { testamentTabIndex = 2 },
                text = { Text("New Testament") }
            )
        }

        val filteredBooks = when (testamentTabIndex) {
            1 -> books.filter { it.testament.equals("OT", ignoreCase = true) }
            2 -> books.filter { it.testament.equals("NT", ignoreCase = true) }
            else -> books
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredBooks) { book ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedBook = book },
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
                                        text = book.bookId,
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
                                    text = "${book.chapters} chapters • ${book.sermonCount} sermons",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerseHeatMapCell(
    verse: VerseCountDto,
    maxCount: Int,
    onClick: () -> Unit
) {
    // Heat map intensity from 0.15 to 1.0
    val density = (verse.sermonCount.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
    val cellColor = if (verse.sermonCount > 0) {
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
                text = "${verse.verse}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (density > 0.5f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            if (verse.sermonCount > 0) {
                Text(
                    text = "${verse.sermonCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (density > 0.5f) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
