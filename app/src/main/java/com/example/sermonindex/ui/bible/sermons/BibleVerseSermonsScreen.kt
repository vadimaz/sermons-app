package com.example.sermonindex.ui.bible.sermons

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.ui.components.EmptyStateView
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import com.example.sermonindex.ui.components.SermonListItem
import com.example.sermonindex.ui.scripture.ScriptureVerseQuoteCard
import com.example.sermonindex.ui.scripture.TranslationPickerBottomSheet
import com.example.sermonindex.ui.scripture.TranslationPickerTarget

/**
 * Stateful entry point for BibleVerseSermonsScreen.
 */
@Composable
fun BibleVerseSermonsScreen(
    bookId: String,
    bookName: String,
    chapter: Int,
    verse: Int,
    onBack: () -> Unit,
    onSermonClick: (Sermon) -> Unit,
    onTextSermonClick: (Sermon) -> Unit = onSermonClick,
    modifier: Modifier = Modifier
) {
    val args = BibleVerseArgs(bookId, bookName, chapter, verse)
    val viewModel: BibleVerseSermonsViewModel = hiltViewModel(
        key = "verse_sermons_${bookId}_${chapter}_${verse}"
    ) { factory: BibleVerseSermonsViewModel.Factory ->
        factory.create(args)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BibleVerseSermonsContent(
        state = state,
        onEvent = { event ->
            when (event) {
                is BibleVerseSermonsUiEvent.ClickSermon -> onSermonClick(event.sermon)
                is BibleVerseSermonsUiEvent.ReadSermon -> onTextSermonClick(event.sermon)
                else -> viewModel.onEvent(event)
            }
        },
        onBack = onBack,
        modifier = modifier
    )
}

/**
 * Stateless BibleVerseSermonsContent.
 */
@Composable
fun BibleVerseSermonsContent(
    state: BibleVerseSermonsUiState,
    onEvent: (BibleVerseSermonsUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Standard 56dp Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    val passageTitle = "${state.bookName} ${state.chapter}:${state.verse}"
                    Text(
                        text = passageTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.sermons_on_verse),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Body Content
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Dual Translation Verse Card
            item(key = "verse_quote_card") {
                ScriptureVerseQuoteCard(
                    bookName = state.bookName,
                    chapter = state.chapter,
                    verse = state.verse,
                    firstTranslation = state.firstTranslation,
                    secondTranslation = state.secondTranslation,
                    firstVerseText = state.firstVerseText,
                    secondVerseText = state.secondVerseText,
                    isLoadingFirstText = state.isLoadingFirstVerseText,
                    isLoadingSecondText = state.isLoadingSecondVerseText,
                    onSelectFirstTranslation = {
                        onEvent(BibleVerseSermonsUiEvent.OpenTranslationPicker(TranslationPickerTarget.FIRST))
                    },
                    onSelectSecondTranslation = {
                        onEvent(BibleVerseSermonsUiEvent.OpenTranslationPicker(TranslationPickerTarget.SECOND))
                    }
                )
            }

            // Sermons section
            if (state.isLoadingSermons && state.sermons.isEmpty()) {
                item(key = "loading_sermons") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingView(
                            text = stringResource(
                                R.string.loading_verse_sermons,
                                state.bookName,
                                state.chapter,
                                state.verse
                            )
                        )
                    }
                }
            } else if (state.errorMessage != null && state.sermons.isEmpty()) {
                item(key = "error_sermons") {
                    ErrorView(
                        message = state.errorMessage,
                        onRetry = { onEvent(BibleVerseSermonsUiEvent.Retry) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            } else if (state.sermons.isEmpty()) {
                item(key = "empty_sermons") {
                    EmptyStateView(
                        title = stringResource(R.string.no_sermons_found_title),
                        subtitle = stringResource(R.string.no_sermons_found_subtitle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            } else {
                item(key = "sermons_header") {
                    Text(
                        text = stringResource(
                            R.string.sermons_referencing_verse,
                            state.sermons.size,
                            state.bookName,
                            state.chapter,
                            state.verse
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(state.sermons, key = { it.id }) { sermon ->
                    SermonListItem(
                        sermon = sermon,
                        onPlayClick = { onEvent(BibleVerseSermonsUiEvent.PlaySermon(sermon)) },
                        onReadClick = { onEvent(BibleVerseSermonsUiEvent.ReadSermon(sermon)) },
                        onItemClick = { onEvent(BibleVerseSermonsUiEvent.ClickSermon(sermon)) },
                        onFavoriteToggle = { onEvent(BibleVerseSermonsUiEvent.ToggleFavorite(sermon)) },
                        isFavorite = state.favoriteIds.contains(sermon.id)
                    )
                }
            }
        }
    }

    // Translation Picker Modal Bottom Sheet
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
                onEvent(BibleVerseSermonsUiEvent.SelectTranslation(translation, state.translationPickerTarget))
            },
            onDismiss = { onEvent(BibleVerseSermonsUiEvent.DismissTranslationPicker) }
        )
    }
}
