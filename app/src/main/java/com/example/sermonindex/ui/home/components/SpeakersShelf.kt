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
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.theme.SermonIndexTheme
import com.example.sermonindex.theme.ThemePreviews
import com.example.sermonindex.ui.components.PreviewFixtures

@Composable
fun SpeakersShelf(
    speakers: List<Speaker>,
    onSpeakerClick: (Speaker) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = speakers,
            key = { it.slug },
            contentType = { "speaker_card" }
        ) { speaker ->
            SpeakerCard(
                speaker = speaker,
                onClick = { onSpeakerClick(speaker) }
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SpeakersShelfPreview() {
    SermonIndexTheme {
        Surface {
            SpeakersShelf(
                speakers = listOf(
                    PreviewFixtures.previewSpeaker,
                    PreviewFixtures.previewSpeaker.copy(id = "spk_2", name = "Leonard Ravenhill", slug = "leonard-ravenhill")
                ),
                onSpeakerClick = {}
            )
        }
    }
}
