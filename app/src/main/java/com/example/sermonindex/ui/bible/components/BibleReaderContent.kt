package com.example.sermonindex.ui.bible.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.BibleBookmark
import com.example.sermonindex.domain.model.BibleChapterItem
import com.example.sermonindex.domain.model.BibleFootnote
import com.example.sermonindex.domain.model.HighlightColor
import com.example.sermonindex.ui.bible.BibleUiState

@Composable
fun BibleReaderContent(
    state: BibleUiState,
    listState: LazyListState,
    onOpenTableOfContents: () -> Unit,
    onNextChapter: () -> Unit,
    onPreviousChapter: () -> Unit,
    onFootnoteClick: (BibleFootnote) -> Unit,
    onOpenSermonsForVerse: (bookId: String, bookName: String, chapter: Int, verse: Int) -> Unit,
    onStartVerseSelection: (verseNumber: Int) -> Unit,
    onClearTargetScrollVerse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chapter = state.currentChapter ?: return

    val preferences = state.preferences
    val fontFamily = if (preferences.isSerif) FontFamily.Serif else FontFamily.Default
    val fontSize = preferences.fontSizeSp.sp
    val lineHeight = (preferences.fontSizeSp * preferences.lineSpacingMultiplier).sp

    val bookmarksByVerse = remember(state.chapterBookmarks) {
        state.chapterBookmarks.associateBy { it.verse }
    }
    val footnotesById = remember(chapter.footnotes) {
        chapter.footnotes.associateBy { it.noteId }
    }
    val footnotesByVerse = remember(chapter.footnotes) {
        chapter.footnotes.groupBy { it.verse }
    }

    // Scroll to target verse if requested (e.g. from Library bookmarks)
    LaunchedEffect(state.targetScrollVerse, chapter) {
        val targetVerse = state.targetScrollVerse ?: return@LaunchedEffect
        val isTargetChapterLoaded = chapter.bookId.equals(preferences.bookId, ignoreCase = true) &&
            chapter.chapterNumber == preferences.chapter
        if (!isTargetChapterLoaded) return@LaunchedEffect

        val itemIndex = chapter.items.indexOfFirst {
            it is BibleChapterItem.Verse && it.number == targetVerse
        }
        if (itemIndex != -1) {
            listState.animateScrollToItem(itemIndex + 1)
        }
        onClearTargetScrollVerse()
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 0.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Top Chapter Title with Table of Contents invocation
        item(key = "chapter_header") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenTableOfContents() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = chapter.bookName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.table_of_contents),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.chapter_number, chapter.chapterNumber),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Chapter Content Items
        itemsIndexed(
            items = chapter.items,
            key = { index, item ->
                when (item) {
                    is BibleChapterItem.Verse -> "verse_${item.number}"
                    is BibleChapterItem.Heading -> "heading_${item.text.hashCode()}_$index"
                    is BibleChapterItem.HebrewSubtitle -> "sub_${item.text.hashCode()}_$index"
                    is BibleChapterItem.LineBreak -> "br_$index"
                }
            },
            contentType = { _, item -> item::class }
        ) { _, item ->
            when (item) {
                is BibleChapterItem.Heading -> {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = fontFamily,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp, bottom = 8.dp)
                    )
                }
                is BibleChapterItem.HebrewSubtitle -> {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = fontFamily,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 8.dp)
                    )
                }
                is BibleChapterItem.LineBreak -> {
                    Spacer(modifier = Modifier.height(10.dp))
                }
                is BibleChapterItem.Verse -> {
                    val isSelected = item.number == state.selectedVerse
                    val matchingBookmark = bookmarksByVerse[item.number]

                    VerseItemRow(
                        verse = item,
                        bookId = chapter.bookId,
                        bookName = chapter.bookName,
                        chapterNumber = chapter.chapterNumber,
                        fontFamily = fontFamily,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        footnotesById = footnotesById,
                        footnotesByVerse = footnotesByVerse,
                        isSelectionMode = state.isSelectionMode,
                        isSelected = isSelected,
                        matchingBookmark = matchingBookmark,
                        onFootnoteClick = onFootnoteClick,
                        onOpenSermonsForVerse = onOpenSermonsForVerse,
                        onStartSelection = { onStartVerseSelection(item.number) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    )
                }
            }
        }

        // Bottom Chapter Navigation
        item(key = "bottom_nav_footer") {
            Spacer(modifier = Modifier.height(24.dp))
            BottomNavigationFooter(
                onPreviousChapter = onPreviousChapter,
                onNextChapter = onNextChapter
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerseItemRow(
    verse: BibleChapterItem.Verse,
    bookId: String,
    bookName: String,
    chapterNumber: Int,
    fontFamily: FontFamily,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    footnotesById: Map<Int, BibleFootnote>,
    footnotesByVerse: Map<Int, List<BibleFootnote>>,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    matchingBookmark: BibleBookmark?,
    onFootnoteClick: (BibleFootnote) -> Unit,
    onOpenSermonsForVerse: (bookId: String, bookName: String, chapter: Int, verse: Int) -> Unit,
    onStartSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val matchingFootnotes = remember(verse, footnotesById, footnotesByVerse) {
        if (verse.footnoteIds.isNotEmpty()) {
            verse.footnoteIds.mapNotNull { footnotesById[it] }
        } else {
            footnotesByVerse[verse.number] ?: footnotesByVerse[0] ?: emptyList()
        }
    }
    val matchingFootnote = remember(matchingFootnotes) {
        if (matchingFootnotes.isEmpty()) null
        else if (matchingFootnotes.size == 1) matchingFootnotes.first()
        else {
            val first = matchingFootnotes.first()
            val combinedText = matchingFootnotes.joinToString("\n\n") { fn ->
                val prefix = if (fn.caller.isNotBlank() && fn.caller != "+") "${fn.caller} " else ""
                "$prefix${fn.text}"
            }
            first.copy(text = combinedText)
        }
    }

    val highlightColor = matchingBookmark?.color
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        highlightColor != null -> when (highlightColor) {
            HighlightColor.YELLOW -> Color(0xFFFBC02D).copy(alpha = 0.28f)
            HighlightColor.GREEN -> Color(0xFF388E3C).copy(alpha = 0.25f)
            HighlightColor.BLUE -> Color(0xFF1976D2).copy(alpha = 0.25f)
            HighlightColor.CORAL -> Color(0xFFD32F2F).copy(alpha = 0.25f)
        }
        else -> Color.Transparent
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val annotatedText = remember(verse.number, verse.text, fontSize, matchingFootnote, primaryColor, tertiaryColor) {
        buildAnnotatedString {
            // Verse Number
            withStyle(
                SpanStyle(
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = (fontSize.value * 0.75f).sp,
                    baselineShift = BaselineShift.Superscript
                )
            ) {
                append("${verse.number} ")
            }

            // Verse Text
            append(verse.text)

            // Footnote Indicators
            if (verse.footnoteIds.isNotEmpty() || matchingFootnote != null) {
                val link = if (matchingFootnote != null) {
                    LinkAnnotation.Clickable(
                        tag = "footnote_${verse.number}",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = tertiaryColor,
                                textDecoration = TextDecoration.None
                            )
                        ),
                        linkInteractionListener = {
                            onFootnoteClick(matchingFootnote)
                        }
                    )
                } else null

                withStyle(
                    SpanStyle(
                        color = tertiaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = (fontSize.value * 0.7f).sp,
                        baselineShift = BaselineShift.Superscript
                    )
                ) {
                    if (link != null) {
                        withLink(link) {
                            append(" *")
                        }
                    } else {
                        append(" *")
                    }
                }
            }
        }
    }

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .then(
                    if (isSelected) {
                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    } else Modifier
                )
                .combinedClickable(
                    onClick = {
                        onOpenSermonsForVerse(bookId, bookName, chapterNumber, verse.number)
                    },
                    onLongClick = {
                        onStartSelection()
                    }
                )
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(
                text = annotatedText,
                style = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BottomNavigationFooter(
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onPreviousChapter,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.previous_chapter))
        }

        Button(
            onClick = onNextChapter,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(stringResource(R.string.next_chapter))
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
