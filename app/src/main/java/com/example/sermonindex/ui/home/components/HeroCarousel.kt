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
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.theme.SermonIndexTheme
import com.example.sermonindex.theme.ThemePreviews
import com.example.sermonindex.ui.components.PreviewFixtures

@Composable
fun HeroCarousel(
    sermons: List<Sermon>,
    onPlayClick: (Sermon) -> Unit,
    onSermonClick: (Sermon) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = sermons,
            key = { it.id },
            contentType = { "hero_sermon" }
        ) { sermon ->
            HeroCard(
                sermon = sermon,
                onPlayClick = { onPlayClick(sermon) },
                onClick = { onSermonClick(sermon) }
            )
        }
    }
}

@ThemePreviews
@Composable
private fun HeroCarouselPreview() {
    SermonIndexTheme {
        Surface {
            HeroCarousel(
                sermons = listOf(
                    PreviewFixtures.previewAudioSermon,
                    PreviewFixtures.previewTextSermon
                ),
                onPlayClick = {},
                onSermonClick = {}
            )
        }
    }
}
