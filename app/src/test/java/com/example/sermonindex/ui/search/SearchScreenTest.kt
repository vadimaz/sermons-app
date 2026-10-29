package com.example.sermonindex.ui.search

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.SearchCategory
import com.example.sermonindex.theme.SermonIndexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun searchContent_typingTriggersUpdateQuery() {
        var queryReceived = ""

        val state = SearchUiState(
            query = "",
            recentQueries = listOf("Tozer", "Ravenhill")
        )

        composeTestRule.setContent {
            SermonIndexTheme {
                SearchContent(
                    state = state,
                    onEvent = { event ->
                        if (event is SearchUiEvent.UpdateQuery) {
                            queryReceived = event.query
                        }
                    },
                    onSpeakerSelect = {},
                    onTopicSelect = {},
                    onSermonSelect = { _, _ -> },
                    onTextSermonSelect = { _, _ -> }
                )
            }
        }

        // Recent searches are displayed
        composeTestRule.onNodeWithText("Tozer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ravenhill").assertIsDisplayed()

        // Type query into search field
        composeTestRule.onNodeWithText(context.getString(R.string.search_hint)).performTextInput("Spurgeon")
        assertEquals("Spurgeon", queryReceived)
    }

    @Test
    fun searchContent_showsCategoryChipsWhenQueryPresent() {
        var selectedCategory: SearchCategory? = null

        val state = SearchUiState(
            query = "prayer",
            selectedCategory = SearchCategory.ALL
        )

        composeTestRule.setContent {
            SermonIndexTheme {
                SearchContent(
                    state = state,
                    onEvent = { event ->
                        if (event is SearchUiEvent.SelectCategory) {
                            selectedCategory = event.category
                        }
                    },
                    onSpeakerSelect = {},
                    onTopicSelect = {},
                    onSermonSelect = { _, _ -> },
                    onTextSermonSelect = { _, _ -> }
                )
            }
        }

        // Category filter chips are displayed
        composeTestRule.onNodeWithText(context.getString(R.string.search_filter_all)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.search_filter_sermons)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.search_filter_speakers)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.search_filter_topics)).assertIsDisplayed()

        // Click Speakers chip
        composeTestRule.onNodeWithText(context.getString(R.string.search_filter_speakers)).performClick()
        assertEquals(SearchCategory.SPEAKERS, selectedCategory)
    }
}
