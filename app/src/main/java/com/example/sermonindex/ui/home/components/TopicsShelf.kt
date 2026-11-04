package com.example.sermonindex.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
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
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    PreviewFixtures.previewTopic.copy(id = "top_2", name = "Repentance", slug = "repentance", count = 110),
                    PreviewFixtures.previewTopic.copy(id = "top_3", name = "Holy Spirit", slug = "holy-spirit", count = 520),
                    PreviewFixtures.previewTopic.copy(id = "top_4", name = "Revival", slug = "revival", count = 480),
                    PreviewFixtures.previewTopic.copy(id = "top_5", name = "Holiness", slug = "holiness", count = 390),
                    PreviewFixtures.previewTopic.copy(id = "top_6", name = "Grace & Faith", slug = "grace-faith", count = 210)
                ),
                onTopicClick = {}
            )
        }
    }
}
