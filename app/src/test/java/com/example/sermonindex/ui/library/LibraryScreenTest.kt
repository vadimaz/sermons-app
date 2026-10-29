package com.example.sermonindex.ui.library

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.sermonindex.R
import com.example.sermonindex.theme.SermonIndexTheme
import org.junit.Assert.assertEquals
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
    fun libraryContent_showsTabsAndEmptyState() {
        var selectedTab = 0

        val state = LibraryUiState(
            selectedTab = 0,
            favorites = emptyList()
        )

        composeTestRule.setContent {
            SermonIndexTheme {
                LibraryContent(
                    state = state,
                    onEvent = { event ->
                        if (event is LibraryUiEvent.SelectTab) {
                            selectedTab = event.tab
                        }
                    }
                )
            }
        }

        // All 4 tabs are visible
        composeTestRule.onNodeWithText(context.getString(R.string.tab_favorites)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.tab_downloads)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.tab_history)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.tab_bookmarks)).assertIsDisplayed()

        // Empty state for favorites tab
        composeTestRule.onNodeWithText(context.getString(R.string.no_favorites_title)).assertIsDisplayed()

        // Clicking Downloads tab triggers SelectTab(1)
        composeTestRule.onNodeWithText(context.getString(R.string.tab_downloads)).performClick()
        assertEquals(1, selectedTab)
    }
}
