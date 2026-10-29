package com.example.sermonindex.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.sermonindex.R
import com.example.sermonindex.testing.createTestSermon
import com.example.sermonindex.testing.createTestTextSermon
import com.example.sermonindex.theme.SermonIndexTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SermonListItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun audioSermon_rendersPlayActionAndDuration() {
        var playClicked = false
        var favoriteClicked = false
        var itemClicked = false

        val sermon = createTestSermon(
            title = "The Crucified Life",
            speaker = "A.W. Tozer",
            duration = "45:00"
        )

        composeTestRule.setContent {
            SermonIndexTheme {
                SermonListItem(
                    sermon = sermon,
                    isFavorite = false,
                    onPlayClick = { playClicked = true },
                    onItemClick = { itemClicked = true },
                    onFavoriteToggle = { favoriteClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("The Crucified Life").assertIsDisplayed()
        composeTestRule.onNodeWithText("A.W. Tozer").assertIsDisplayed()
        composeTestRule.onNodeWithText("45:00").assertIsDisplayed()

        // Click play button
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.play_sermon)).performClick()
        assertTrue(playClicked)

        // Click favorite toggle
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.add_to_favorites)).performClick()
        assertTrue(favoriteClicked)

        // Click whole item
        composeTestRule.onNodeWithText("The Crucified Life").performClick()
        assertTrue(itemClicked)
    }

    @Test
    fun textSermon_rendersBadgeAndReaderAction() {
        var readClicked = false

        val textSermon = createTestTextSermon(
            title = "Morning Devotion",
            speaker = "C.H. Spurgeon"
        )

        composeTestRule.setContent {
            SermonIndexTheme {
                SermonListItem(
                    sermon = textSermon,
                    isFavorite = true,
                    onPlayClick = {},
                    onReadClick = { readClicked = true },
                    onItemClick = {},
                    onFavoriteToggle = {}
                )
            }
        }

        // Must display Text badge
        composeTestRule.onNodeWithText(context.getString(R.string.badge_text_sermon)).assertIsDisplayed()

        // Favorite icon has remove description
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.remove_from_favorites)).assertIsDisplayed()

        // Action button is Read Sermon
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.read_sermon)).performClick()
        assertTrue(readClicked)
    }

    @Test
    fun highlightQuery_displaysMatchingSnippet() {
        val sermon = createTestSermon(
            title = "Power Through Prayer",
            summary = "True revival begins in private intercession and repentance."
        )

        composeTestRule.setContent {
            SermonIndexTheme {
                SermonListItem(
                    sermon = sermon,
                    isFavorite = false,
                    onPlayClick = {},
                    onItemClick = {},
                    onFavoriteToggle = {},
                    highlightQuery = "intercession"
                )
            }
        }

        // Snippet text should be displayed containing the query
        composeTestRule.onNodeWithText("intercession", substring = true).assertIsDisplayed()
    }
}
