package com.example.sermonindex.ui.home.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.theme.SermonIndexTheme
import com.example.sermonindex.theme.ThemePreviews
import com.example.sermonindex.ui.components.PreviewFixtures

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroCarousel(
    sermons: List<Sermon>,
    onPlayClick: (Sermon) -> Unit,
    onSermonClick: (Sermon) -> Unit,
    modifier: Modifier = Modifier
) {
    if (sermons.isEmpty()) return

    val carouselState = rememberCarouselState { sermons.size }

    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = 284.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(196.dp),
        itemSpacing = 12.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) { index ->
        val sermon = sermons[index]
        HeroCard(
            sermon = sermon,
            onPlayClick = { onPlayClick(sermon) },
            onClick = { onSermonClick(sermon) },
            modifier = Modifier
                .height(196.dp)
                .maskClip(MaterialTheme.shapes.extraLarge)
        )
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
