package com.example.sermonindex.ui.library

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.RecentMediaItem
import com.example.sermonindex.theme.SermonIndexTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun libraryContent_showsMenuRowsAndEmptyRecentState() {
        var favoritesClicked = false

        val state = LibraryUiState(
            favorites = emptyList(),
            downloads = emptyList(),
            history = emptyList(),
            bookmarks = emptyList(),
            recentItems = emptyList()
        )

        composeTestRule.setContent {
            SermonIndexTheme {
                LibraryContent(
                    state = state,
                    onEvent = {},
                    onSermonSelect = {},
                    onNavigateToFavorites = { favoritesClicked = true },
                    onNavigateToDownloads = {},
                    onNavigateToHistory = {},
                    onNavigateToBookmarks = {},
                    onNavigateToSettings = {}
                )
            }
        }

        // 3 primary menu items are visible (Favorites, Downloads, Bookmarks)
        composeTestRule.onNodeWithText(context.getString(R.string.library_menu_favorites)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.library_menu_downloads)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.library_menu_bookmarks)).assertIsDisplayed()

        // "History" section header & empty placeholder
        composeTestRule.onNodeWithText(context.getString(R.string.tab_history)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.library_history_empty)).assertIsDisplayed()

        // Clicking Favorites triggers callback
        composeTestRule.onNodeWithText(context.getString(R.string.library_menu_favorites)).performClick()
        assertTrue(favoritesClicked)
    }

    @Test
    fun libraryContent_showsRecentSermonsCards() {
        var selectedSermonId: String? = null

        val state = LibraryUiState(
            recentItems = listOf(
                RecentMediaItem(
                    id = "recent-1",
                    title = "Revival Praying",
                    speaker = "Leonard Ravenhill"
                )
            )
        )

        composeTestRule.setContent {
            SermonIndexTheme {
                LibraryContent(
                    state = state,
                    onEvent = {},
                    onSermonSelect = { selectedSermonId = it },
                    onNavigateToFavorites = {},
                    onNavigateToDownloads = {},
                    onNavigateToHistory = {},
                    onNavigateToBookmarks = {},
                    onNavigateToSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Revival Praying").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leonard Ravenhill").assertIsDisplayed()

        composeTestRule.onNodeWithText("Revival Praying").performClick()
        org.junit.Assert.assertEquals("recent-1", selectedSermonId)
    }
}
