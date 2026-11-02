package com.example.sermonindex.ui.library

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.BibleBookmark
import com.example.sermonindex.ui.bible.components.QuoteCardSheet
import com.example.sermonindex.ui.library.components.BookmarksTabContent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BibleBookmarksScreen(
    onBack: () -> Unit,
    onNavigateToBible: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var sharingBookmark by remember { mutableStateOf<BibleBookmark?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.library_menu_bookmarks),
                        style = MaterialTheme.typography.headlineSmallEmphasized
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        BookmarksTabContent(
            bookmarks = state.bookmarks,
            listState = listState,
            onSelectBookmark = { bookmark ->
                viewModel.onEvent(LibraryUiEvent.OpenBookmarkInBible(bookmark))
                onNavigateToBible()
            },
            onDelete = { id -> viewModel.onEvent(LibraryUiEvent.DeleteBookmark(id)) },
            onShare = { bookmark -> sharingBookmark = bookmark },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )

        sharingBookmark?.let { bookmark ->
            QuoteCardSheet(
                verseText = bookmark.text,
                reference = "${bookmark.bookName} ${bookmark.chapter}:${bookmark.verse}",
                translationName = bookmark.translationId.uppercase(),
                onDismissRequest = { sharingBookmark = null }
            )
        }
    }
}
