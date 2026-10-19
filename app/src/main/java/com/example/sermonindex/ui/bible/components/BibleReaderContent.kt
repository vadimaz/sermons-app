package com.example.sermonindex.ui.bible.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.BibleChapter
import com.example.sermonindex.domain.model.BibleChapterItem
import com.example.sermonindex.domain.model.BibleFootnote
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
    modifier: Modifier = Modifier
) {
    val chapter = state.currentChapter ?: return

    val preferences = state.preferences
    val fontFamily = if (preferences.isSerif) FontFamily.Serif else FontFamily.Default
    val fontSize = preferences.fontSizeSp.sp
    val lineHeight = (preferences.fontSizeSp * preferences.lineSpacingMultiplier).sp

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
        itemsIndexed(chapter.items, key = { index, _ -> "item_$index" }) { _, item ->
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
                    VerseItemRow(
                        verse = item,
                        bookId = chapter.bookId,
                        bookName = chapter.bookName,
                        chapterNumber = chapter.chapterNumber,
                        fontFamily = fontFamily,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        footnotes = chapter.footnotes,
                        onFootnoteClick = onFootnoteClick,
                        onOpenSermonsForVerse = onOpenSermonsForVerse,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }

        // Bottom Chapter Navigation (No extra card underneath)
        item(key = "bottom_nav_footer") {
            Spacer(modifier = Modifier.height(24.dp))
            BottomNavigationFooter(
                onPreviousChapter = onPreviousChapter,
                onNextChapter = onNextChapter
            )
        }
    }
}

@Composable
private fun VerseItemRow(
    verse: BibleChapterItem.Verse,
    bookId: String,
    bookName: String,
    chapterNumber: Int,
    fontFamily: FontFamily,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    footnotes: List<BibleFootnote>,
    onFootnoteClick: (BibleFootnote) -> Unit,
    onOpenSermonsForVerse: (bookId: String, bookName: String, chapter: Int, verse: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTooltip by remember { mutableStateOf(false) }

    val matchingFootnotes = remember(verse, footnotes) {
        val byId = footnotes.filter { it.noteId in verse.footnoteIds }
        if (byId.isNotEmpty()) {
            byId
        } else {
            footnotes.filter { (it.chapter == chapterNumber || it.chapter == 0) && it.verse == verse.number }
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

    Box(modifier = modifier) {
        val annotatedText = buildAnnotatedString {
            // Verse Number
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
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
                                color = MaterialTheme.colorScheme.tertiary,
                                textDecoration = TextDecoration.None
                            )
                        ),
                        linkInteractionListener = {
                            showTooltip = false
                            onFootnoteClick(matchingFootnote)
                        }
                    )
                } else null

                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.tertiary,
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

        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable {
                    showTooltip = !showTooltip
                }
                .padding(vertical = 2.dp)
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

        // Direct tooltip on regular tap to navigate to sermons for this verse or view footnote
        if (showTooltip) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, -50),
                onDismissRequest = { showTooltip = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shadowElevation = 6.dp,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    showTooltip = false
                                    onOpenSermonsForVerse(bookId, bookName, chapterNumber, verse.number)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.verse_sermons_pill, verse.number),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.inverseOnSurface
                            )
                        }

                        if (matchingFootnote != null) {
                            Spacer(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(18.dp)
                                    .background(MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.3f))
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        showTooltip = false
                                        onFootnoteClick(matchingFootnote)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.footnote_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.inverseOnSurface
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
