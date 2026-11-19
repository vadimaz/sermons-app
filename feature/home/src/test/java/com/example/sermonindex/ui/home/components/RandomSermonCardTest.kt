package com.example.sermonindex.ui.home.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.sermonindex.testing.createTestSermon
import com.example.sermonindex.theme.SermonIndexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RandomSermonCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun extractSpeakerInitials_extractsCorrectLetters() {
        assertEquals("AT", extractSpeakerInitials("A.W. Tozer"))
        assertEquals("CS", extractSpeakerInitials("Charles Spurgeon"))
        assertEquals("CS", extractSpeakerInitials("Charles H. Spurgeon"))
        assertEquals("LR", extractSpeakerInitials("Leonard Ravenhill"))
        assertEquals("PA", extractSpeakerInitials("Paul"))
        assertEquals("SI", extractSpeakerInitials(""))
        assertEquals("SI", extractSpeakerInitials("   "))
    }

    @Test
    fun randomSermonCard_rendersSpeakerTitleAndHandlesClick() {
        var clicked = false

        val sermon = createTestSermon(
            title = "Why Revival Tarries",
            speaker = "Leonard Ravenhill"
        )

        composeTestRule.setContent {
            SermonIndexTheme {
                RandomSermonCard(
                    sermon = sermon,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Why Revival Tarries").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leonard Ravenhill").assertIsDisplayed()

        composeTestRule.onNodeWithText("Why Revival Tarries").performClick()
        assertTrue(clicked)
    }

    @Test
    fun randomSermonCard_whenNoImage_rendersInitialsMonogram() {
        val sermon = createTestSermon(
            title = "The Knowledge of the Holy",
            speaker = "A.W. Tozer",
            speakerImage = ""
        )

        composeTestRule.setContent {
            SermonIndexTheme {
                RandomSermonCard(
                    sermon = sermon,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("AT").assertIsDisplayed()
        composeTestRule.onNodeWithText("The Knowledge of the Holy").assertIsDisplayed()
        composeTestRule.onNodeWithText("A.W. Tozer").assertIsDisplayed()
    }
}
