package com.example.sermonindex.ui.home.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.sermonindex.domain.model.Topic
import com.example.sermonindex.theme.SermonIndexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TopicCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun topicCard_rendersTopicNameCapsuleAndHandlesClick() {
        var clicked = false
        val topic = Topic(
            id = "top-1",
            name = "Prayer & Intercession",
            slug = "prayer-intercession",
            count = 450
        )

        composeTestRule.setContent {
            SermonIndexTheme {
                TopicCard(
                    topic = topic,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Prayer & Intercession").assertIsDisplayed()
        composeTestRule.onNodeWithText("Prayer & Intercession").performClick()
        assertTrue(clicked)
    }

    @Test
    fun topicsShelf_rendersGridOfTopicsAndHandlesSelection() {
        var selectedTopic: Topic? = null
        val topics = listOf(
            Topic(id = "1", name = "Holy Spirit", slug = "holy-spirit", count = 500),
            Topic(id = "2", name = "Revival", slug = "revival", count = 400),
            Topic(id = "3", name = "Holiness", slug = "holiness", count = 300),
            Topic(id = "4", name = "Faith", slug = "faith", count = 200)
        )

        composeTestRule.setContent {
            SermonIndexTheme {
                TopicsShelf(
                    topics = topics,
                    onTopicClick = { selectedTopic = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Holy Spirit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Revival").assertIsDisplayed()
        composeTestRule.onNodeWithText("Holiness").assertIsDisplayed()
        composeTestRule.onNodeWithText("Faith").assertIsDisplayed()

        composeTestRule.onNodeWithText("Revival").performClick()
        assertEquals("revival", selectedTopic?.slug)
    }
}
