package com.example.sermonindex.ui.bible

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.sermonindex.BibleRoute
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.BibleChapterItem
import com.example.sermonindex.ui.bible.components.BibleFootnoteDialog
import com.example.sermonindex.ui.bible.components.BibleReaderContent
import com.example.sermonindex.ui.bible.components.BibleVerseBottomSheet
import com.example.sermonindex.ui.bible.components.BibleSettingsSheet
import com.example.sermonindex.ui.bible.components.BibleTableOfContentsSheet
import com.example.sermonindex.ui.bible.components.BibleTopBar
import com.example.sermonindex.ui.bible.components.BibleTranslationPickerSheet
import com.example.sermonindex.ui.bible.components.QuoteCardSheet
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import kotlinx.coroutines.flow.Flow

/**
 * Stateful entry point for BibleScreen.
 */
@Composable
fun BibleScreen(
    onNavigateToVerseSermons: (bookId: String, bookName: String, chapter: Int, verse: Int) -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
    reselectEvents: Flow<NavKey>? = null,
    modifier: Modifier = Modifier,
    viewModel: BibleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onEvent(BibleUiEvent.StopAudio)
            onBottomBarVisibilityChange(true)
        }
    }

    BibleContent(
        state = state,
        onEvent = { event ->
            when (event) {
                is BibleUiEvent.OpenSermonsForVerse -> {
                    onNavigateToVerseSermons(event.bookId, event.bookName, event.chapter, event.verse)
                }
                else -> viewModel.onEvent(event)
            }
        },
        onBottomBarVisibilityChange = onBottomBarVisibilityChange,
        reselectEvents = reselectEvents,
        modifier = modifier
    )
}

/**
 * Stateless BibleContent.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BibleContent(
    state: BibleUiState,
    onEvent: (BibleUiEvent) -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
    reselectEvents: Flow<NavKey>? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()
    var isTopBarVisible by rememberSaveable { mutableStateOf(true) }

    val topBarHeight = 56.dp
    val animatedTopBarHeight by animateDpAsState(
        targetValue = if (isTopBarVisible) topBarHeight else 0.dp,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "topBarHeight"
    )
    val animatedTopBarTranslationY by animateDpAsState(
        targetValue = if (isTopBarVisible) 0.dp else -topBarHeight,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "topBarTranslationY"
    )

    // Reset scroll to top and restore bars when chapter changes
    val chapterKey = "${state.currentChapter?.translationId}_${state.currentChapter?.bookId}_${state.currentChapter?.chapterNumber}"
    var lastChapterKey by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(chapterKey) {
        if (state.currentChapter != null) {
            if (lastChapterKey != null && lastChapterKey != chapterKey && state.targetScrollVerse == null) {
                listState.scrollToItem(0)
            }
            lastChapterKey = chapterKey
            isTopBarVisible = true
            onBottomBarVisibilityChange(true)
        }
    }

    // Scroll to top on tab reselect
    LaunchedEffect(reselectEvents) {
        reselectEvents?.collect { route ->
            if (route == BibleRoute) {
                listState.animateScrollToItem(0)
                isTopBarVisible = true
                onBottomBarVisibilityChange(true)
            }
        }
    }

    LaunchedEffect(state.isSelectionMode) {
        if (state.isSelectionMode) {
            onBottomBarVisibilityChange(false)
        } else {
            onBottomBarVisibilityChange(true)
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -8f) {
                    onBottomBarVisibilityChange(false)
                    isTopBarVisible = false
                } else if (available.y > 8f) {
                    onBottomBarVisibilityChange(true)
                    isTopBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedTopBarHeight)
                .clipToBounds()
        ) {
            BibleTopBar(
                state = state,
                onToggleAudio = { onEvent(BibleUiEvent.ToggleAudio()) },
                onSettingsClick = { onEvent(BibleUiEvent.SetSettingsOpen(true)) },
                onTranslationClick = { onEvent(BibleUiEvent.SetTranslationPickerOpen(true)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topBarHeight)
                    .graphicsLayer {
                        translationY = animatedTopBarTranslationY.toPx()
                    }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                state.isLoading && state.currentChapter == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val bookName = state.currentBook?.name ?: state.preferences.bookId
                        LoadingView(
                            text = stringResource(
                                R.string.loading_bible_chapter,
                                bookName,
                                state.preferences.chapter
                            )
                        )
                    }
                }
                state.errorMessage != null && state.currentChapter == null -> {
                    ErrorView(
                        message = state.errorMessage,
                        onRetry = { onEvent(BibleUiEvent.Retry) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                state.currentChapter != null -> {
                    BibleReaderContent(
                        state = state,
                        listState = listState,
                        onOpenTableOfContents = { onEvent(BibleUiEvent.SetTableOfContentsOpen(true)) },
                        onNextChapter = { onEvent(BibleUiEvent.NextChapter) },
                        onPreviousChapter = { onEvent(BibleUiEvent.PreviousChapter) },
                        onFootnoteClick = { onEvent(BibleUiEvent.ShowFootnote(it)) },
                        onOpenSermonsForVerse = { bId, bName, c, v ->
                            onEvent(BibleUiEvent.OpenSermonsForVerse(bId, bName, c, v))
                        },
                        onStartVerseSelection = { onEvent(BibleUiEvent.StartVerseSelection(it)) },
                        onClearTargetScrollVerse = { onEvent(BibleUiEvent.ClearTargetScrollVerse) }
                    )
                }
            }

            // Verse Actions Bottom Sheet
            val selectedVerseItem = remember(state.selectedVerse, state.currentChapter) {
                state.currentChapter?.items?.filterIsInstance<BibleChapterItem.Verse>()
                    ?.firstOrNull { it.number == state.selectedVerse }
            }
            val matchingBookmark = remember(state.selectedVerse, state.chapterBookmarks) {
                state.chapterBookmarks.firstOrNull { it.verse == state.selectedVerse }
            }
            val selectionHighlightColor = matchingBookmark?.color

            BibleVerseBottomSheet(
                visible = state.isSelectionMode,
                verseNumber = state.selectedVerse,
                currentColor = selectionHighlightColor,
                onDismissRequest = { onEvent(BibleUiEvent.ClearSelection) },
                onColorSelected = { color ->
                    onEvent(BibleUiEvent.ApplyHighlight(color))
                    Toast.makeText(context, R.string.toast_saved, Toast.LENGTH_SHORT).show()
                },
                onRemove = {
                    onEvent(BibleUiEvent.DeleteBookmarkForCurrentVerse)
                    Toast.makeText(context, R.string.toast_removed, Toast.LENGTH_SHORT).show()
                },
                onCopy = {
                    val verseText = selectedVerseItem?.text ?: ""
                    val bookName = state.currentChapter?.bookName ?: state.preferences.bookId
                    val chapter = state.currentChapter?.chapterNumber ?: state.preferences.chapter
                    val verse = state.selectedVerse ?: 1
                    val translationName = state.currentTranslation?.shortName ?: state.preferences.translationId
                    val copyContent = "«$verseText»\n— $bookName $chapter:$verse ($translationName)"
                    clipboardManager.setText(AnnotatedString(copyContent))
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                        Toast.makeText(context, R.string.toast_copied, Toast.LENGTH_SHORT).show()
                    }
                    onEvent(BibleUiEvent.ClearSelection)
                },
                onShare = {
                    onEvent(BibleUiEvent.SetQuoteCardSheetOpen(true))
                }
            )
        }
    }

    if (state.showQuoteCardSheet && state.selectedVerse != null) {
        val selectedVerseItem = remember(state.selectedVerse, state.currentChapter) {
            state.currentChapter?.items?.filterIsInstance<BibleChapterItem.Verse>()
                ?.firstOrNull { it.number == state.selectedVerse }
        }
        val verseText = selectedVerseItem?.text ?: ""
        val bookName = state.currentChapter?.bookName ?: state.preferences.bookId
        val chapter = state.currentChapter?.chapterNumber ?: state.preferences.chapter
        val verse = state.selectedVerse
        val reference = "$bookName $chapter:$verse"
        val translationName = state.currentTranslation?.shortName ?: state.preferences.translationId

        QuoteCardSheet(
            verseText = verseText,
            reference = reference,
            translationName = translationName,
            onDismissRequest = { onEvent(BibleUiEvent.SetQuoteCardSheetOpen(false)) }
        )
    }

    // Modal Sheets & Dialogs
    if (state.isTableOfContentsOpen) {
        BibleTableOfContentsSheet(
            books = state.books,
            currentBookId = state.currentChapter?.bookId ?: state.preferences.bookId,
            currentChapter = state.currentChapter?.chapterNumber ?: state.preferences.chapter,
            onSelectBookAndChapter = { bookId, chapter ->
                onEvent(BibleUiEvent.SelectBookAndChapter(bookId, chapter))
            },
            onDismiss = { onEvent(BibleUiEvent.SetTableOfContentsOpen(false)) }
        )
    }

    if (state.isSettingsOpen) {
        val narrators = state.currentChapter?.audioLinks?.keys?.toList() ?: emptyList()
        BibleSettingsSheet(
            preferences = state.preferences,
            availableNarrators = narrators,
            onFontSizeChange = { onEvent(BibleUiEvent.UpdateFontSize(it)) },
            onFontSerifToggle = { onEvent(BibleUiEvent.ToggleFontSerif(it)) },
            onLineSpacingChange = { onEvent(BibleUiEvent.UpdateLineSpacing(it)) },
            onNarratorSelect = { onEvent(BibleUiEvent.SelectNarrator(it)) },
            onDismiss = { onEvent(BibleUiEvent.SetSettingsOpen(false)) }
        )
    }

    if (state.isTranslationPickerOpen) {
        BibleTranslationPickerSheet(
            translations = state.availableTranslations,
            selectedTranslationId = state.preferences.translationId,
            onSelectTranslation = { onEvent(BibleUiEvent.SelectTranslation(it)) },
            onDismiss = { onEvent(BibleUiEvent.SetTranslationPickerOpen(false)) }
        )
    }

    state.activeFootnote?.let { footnote ->
        BibleFootnoteDialog(
            footnote = footnote,
            onDismiss = { onEvent(BibleUiEvent.DismissFootnote) }
        )
    }
}
