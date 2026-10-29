package com.example.sermonindex.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.sermonindex.R
import com.example.sermonindex.theme.SermonIndexTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CommonViewsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun emptyStateView_displaysTitleAndSubtitle() {
        composeTestRule.setContent {
            SermonIndexTheme {
                EmptyStateView(
                    title = "No Bookmarks Yet",
                    subtitle = "Long press any verse in Bible Reader to bookmark it."
                )
            }
        }

        composeTestRule.onNodeWithText("No Bookmarks Yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Long press any verse in Bible Reader to bookmark it.").assertIsDisplayed()
    }

    @Test
    fun errorView_displaysMessageAndTriggersRetry() {
        var retryClicked = false

        composeTestRule.setContent {
            SermonIndexTheme {
                ErrorView(
                    message = "Connection timeout occurred.",
                    onRetry = { retryClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Connection timeout occurred.").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.try_again)).performClick()
        assertTrue(retryClicked)
    }

    @Test
    fun loadingView_displaysLoadingText() {
        composeTestRule.setContent {
            SermonIndexTheme {
                LoadingView(text = "Loading scriptures...")
            }
        }

        composeTestRule.onNodeWithText("Loading scriptures...").assertIsDisplayed()
    }
}
