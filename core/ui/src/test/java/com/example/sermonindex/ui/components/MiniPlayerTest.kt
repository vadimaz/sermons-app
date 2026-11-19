package com.example.sermonindex.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.sermonindex.playback.ActiveMediaInfo
import com.example.sermonindex.testing.createTestSermon
import com.example.sermonindex.theme.SermonIndexTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MiniPlayerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun miniPlayer_rendersTitleAndSpeaker_andRespondsToClicks() {
        var playPauseClicked = false
        var expandClicked = false
        var closeClicked = false

        val sermon = createTestSermon(
            id = "mini-sermon",
            title = "Revival Praying",
            speaker = "Leonard Ravenhill"
        )
        val mediaInfo = ActiveMediaInfo(
            currentSermon = sermon,
            isPlaying = false,
            isBuffering = false
        )

        composeTestRule.setContent {
            SermonIndexTheme {
                MiniPlayer(
                    mediaInfo = mediaInfo,
                    progressProvider = { 0.4f },
                    onTogglePlayPause = { playPauseClicked = true },
                    onExpandClick = { expandClicked = true },
                    onCloseClick = { closeClicked = true }
                )
            }
        }

        // Verify title & speaker are visible in semantics
        composeTestRule.onNodeWithText("Revival Praying").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leonard Ravenhill").assertIsDisplayed()

        // Verify play button click
        composeTestRule.onNodeWithContentDescription("Play").performClick()
        assertTrue(playPauseClicked)

        // Verify close button click
        composeTestRule.onNodeWithContentDescription("Close player").performClick()
        assertTrue(closeClicked)

        // Verify clicking the body triggers expand
        composeTestRule.onNodeWithText("Revival Praying").performClick()
        assertTrue(expandClicked)
    }

    @Test
    fun miniPlayer_showsPauseButton_whenPlaying() {
        val sermon = createTestSermon(title = "True Repentance")
        val mediaInfo = ActiveMediaInfo(
            currentSermon = sermon,
            isPlaying = true,
            isBuffering = false
        )

        composeTestRule.setContent {
            SermonIndexTheme {
                MiniPlayer(
                    mediaInfo = mediaInfo,
                    progressProvider = { 0.8f },
                    onTogglePlayPause = {},
                    onExpandClick = {},
                    onCloseClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }
}
