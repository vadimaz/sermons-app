package com.example.sermonindex.ui.home.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.theme.SermonIndexTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpeakerCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun speakerCard_rendersSpeakerNameAndHandlesClick() {
        var clicked = false
        val speaker = Speaker(
            id = "spk-1",
            name = "Leonard Ravenhill",
            slug = "leonard-ravenhill",
            sermonCount = 520,
            image = ""
        )

        composeTestRule.setContent {
            SermonIndexTheme {
                SpeakerCard(
                    speaker = speaker,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Leonard Ravenhill").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leonard Ravenhill").performClick()
        assertTrue(clicked)
    }

    @Test
    fun speakersShelf_rendersMultipleSpeakers() {
        var selectedSpeaker: Speaker? = null
        val speakers = listOf(
            Speaker(id = "1", name = "A.W. Tozer", slug = "tozer", sermonCount = 100),
            Speaker(id = "2", name = "David Wilkerson", slug = "wilkerson", sermonCount = 200)
        )

        composeTestRule.setContent {
            SermonIndexTheme {
                SpeakersShelf(
                    speakers = speakers,
                    onSpeakerClick = { selectedSpeaker = it }
                )
            }
        }

        composeTestRule.onNodeWithText("A.W. Tozer").assertIsDisplayed()
        composeTestRule.onNodeWithText("David Wilkerson").assertIsDisplayed()

        composeTestRule.onNodeWithText("A.W. Tozer").performClick()
        assertTrue(selectedSpeaker?.slug == "tozer")
    }
}
