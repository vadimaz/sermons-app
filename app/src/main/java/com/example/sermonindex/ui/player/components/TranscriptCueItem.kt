package com.example.sermonindex.ui.player.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sermonindex.domain.model.TranscriptCue
import com.example.sermonindex.theme.SermonIndexTheme
import com.example.sermonindex.theme.ThemePreviews
import com.example.sermonindex.ui.search.components.TextHighlightUtils

@Composable
fun TranscriptCueItem(
    cue: TranscriptCue,
    isActive: Boolean,
    isMatch: Boolean,
    highlightQuery: String?,
    onSeekToCue: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else if (isMatch) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    }
    val animatedColor by animateColorAsState(targetValue = targetColor, label = "cueTextColor")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSeekToCue(cue.startMs) }
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        if (highlightQuery.isNullOrBlank()) {
            Text(
                text = cue.text,
                fontSize = 22.sp,
                lineHeight = 30.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = animatedColor,
                textAlign = TextAlign.Start
            )
        } else {
            val annotated = remember(cue.text, highlightQuery) {
                TextHighlightUtils.buildHighlightedText(
                    text = cue.text,
                    query = highlightQuery,
                    highlightColor = Color(0x66FFC107)
                )
            }
            Text(
                text = annotated,
                fontSize = 22.sp,
                lineHeight = 30.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = animatedColor,
                textAlign = TextAlign.Start
            )
        }
    }
}

@ThemePreviews
@Composable
private fun TranscriptCueItemActivePreview() {
    SermonIndexTheme {
        Surface {
            TranscriptCueItem(
                cue = TranscriptCue(
                    index = 1,
                    startMs = 12000L,
                    endMs = 18000L,
                    text = "No man is greater than his prayer life."
                ),
                isActive = true,
                isMatch = false,
                highlightQuery = null,
                onSeekToCue = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@ThemePreviews
@Composable
private fun TranscriptCueItemInactivePreview() {
    SermonIndexTheme {
        Surface {
            TranscriptCueItem(
                cue = TranscriptCue(
                    index = 2,
                    startMs = 18000L,
                    endMs = 24000L,
                    text = "The pastor who is not praying is playing; the people who are not praying are straying."
                ),
                isActive = false,
                isMatch = false,
                highlightQuery = null,
                onSeekToCue = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
