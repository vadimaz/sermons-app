package com.example.sermonindex.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.theme.SermonIndexTheme
import com.example.sermonindex.theme.ThemePreviews
import com.example.sermonindex.ui.components.PreviewFixtures

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HeroCard(
    sermon: Sermon,
    onPlayClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(284.dp)
            .height(196.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageUrl = sermon.speakerImage.ifBlank { sermon.thumbnailUrl }
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = sermon.speaker,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // High-contrast Expressive gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = sermon.title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isTextOnly = sermon.isTextOnly
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sermon.speaker,
                            style = MaterialTheme.typography.labelLargeEmphasized,
                            color = if (isTextOnly) MaterialTheme.colorScheme.tertiaryContainer else Color(0xFFFFB951),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isTextOnly) {
                            Text(
                                text = stringResource(R.string.badge_text_sermon),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (sermon.duration.isNotBlank()) {
                            Text(
                                text = sermon.duration,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    FilledIconButton(
                        onClick = onPlayClick,
                        shapes = IconButtonDefaults.shapes(
                            shape = if (isTextOnly) IconButtonDefaults.smallSquareShape else IconButtonDefaults.smallRoundShape,
                            pressedShape = IconButtonDefaults.smallPressedShape
                        ),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isTextOnly) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = if (isTextOnly) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.PlayArrow,
                            contentDescription = stringResource(if (isTextOnly) R.string.read_sermon else R.string.play),
                            tint = if (isTextOnly) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun HeroCardAudioPreview() {
    SermonIndexTheme {
        Surface {
            HeroCard(
                sermon = PreviewFixtures.previewAudioSermon,
                onPlayClick = {},
                onClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@ThemePreviews
@Composable
private fun HeroCardTextPreview() {
    SermonIndexTheme {
        Surface {
            HeroCard(
                sermon = PreviewFixtures.previewTextSermon,
                onPlayClick = {},
                onClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
