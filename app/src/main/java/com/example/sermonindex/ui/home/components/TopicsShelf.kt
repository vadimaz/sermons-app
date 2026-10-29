package com.example.sermonindex.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sermonindex.domain.model.Topic
import com.example.sermonindex.theme.SermonIndexTheme
import com.example.sermonindex.theme.ThemePreviews
import com.example.sermonindex.ui.components.PreviewFixtures

@Composable
fun TopicsShelf(
    topics: List<Topic>,
    onTopicClick: (Topic) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = topics,
            key = { it.slug },
            contentType = { "topic_card" }
        ) { topic ->
            TopicCard(
                topic = topic,
                onClick = { onTopicClick(topic) }
            )
        }
    }
}

@ThemePreviews
@Composable
private fun TopicsShelfPreview() {
    SermonIndexTheme {
        Surface {
            TopicsShelf(
                topics = listOf(
                    PreviewFixtures.previewTopic,
                    PreviewFixtures.previewTopic.copy(id = "top_2", name = "Repentance", slug = "repentance", count = 110)
                ),
                onTopicClick = {}
            )
        }
    }
}
