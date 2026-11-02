package com.example.sermonindex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.sermonindex.R
import com.example.sermonindex.playback.ActiveMediaInfo
import com.example.sermonindex.playback.PlaybackState
import com.example.sermonindex.theme.SermonIndexTheme
import com.example.sermonindex.theme.ThemePreviews

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiniPlayer(
    mediaInfo: ActiveMediaInfo,
    progressProvider: () -> Float,
    onTogglePlayPause: () -> Unit,
    onExpandClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sermon = mediaInfo.currentSermon ?: return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(28.dp))
            .clickable(onClick = onExpandClick),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 8.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speaker portrait
                val imageUrl = sermon.speakerImage.ifBlank { sermon.thumbnailUrl }
                AsyncImage(
                    model = imageUrl,
                    contentDescription = sermon.speaker,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Speaker
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = sermon.title,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sermon.speaker,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (mediaInfo.isUsingFailoverCdn) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.cdn),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play / Pause / Buffering button with Expressive shape morphing
                if (mediaInfo.isBuffering) {
                    LoadingIndicator(
                        modifier = Modifier.size(38.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        shapes = IconButtonDefaults.shapes(
                            shape = if (mediaInfo.isPlaying) IconButtonDefaults.smallSquareShape else IconButtonDefaults.smallRoundShape,
                            pressedShape = IconButtonDefaults.smallPressedShape
                        ),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = if (mediaInfo.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(
                                if (mediaInfo.isPlaying) R.string.pause else R.string.play
                            ),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                IconButton(
                    onClick = onCloseClick,
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_player),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expressive Wavy Progress Indicator along bottom of capsule
            LinearWavyProgressIndicator(
                progress = progressProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 6.dp)
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                amplitude = { progress ->
                    if (mediaInfo.isPlaying && progress > 0.01f && progress < 0.99f) 1f else 0f
                }
            )
        }
    }
}

@Composable
fun MiniPlayer(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onExpandClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MiniPlayer(
        mediaInfo = playbackState.toActiveMediaInfo(),
        progressProvider = { playbackState.progressFraction },
        onTogglePlayPause = onTogglePlayPause,
        onExpandClick = onExpandClick,
        onCloseClick = onCloseClick,
        modifier = modifier
    )
}

@ThemePreviews
@Composable
private fun MiniPlayerPlayingPreview() {
    SermonIndexTheme {
        Surface {
            MiniPlayer(
                mediaInfo = ActiveMediaInfo(
                    currentSermon = PreviewFixtures.previewAudioSermon,
                    isPlaying = true
                ),
                progressProvider = { 0.45f },
                onTogglePlayPause = {},
                onExpandClick = {},
                onCloseClick = {}
            )
        }
    }
}

@ThemePreviews
@Composable
private fun MiniPlayerPausedPreview() {
    SermonIndexTheme {
        Surface {
            MiniPlayer(
                mediaInfo = ActiveMediaInfo(
                    currentSermon = PreviewFixtures.previewAudioSermon,
                    isPlaying = false
                ),
                progressProvider = { 0.15f },
                onTogglePlayPause = {},
                onExpandClick = {},
                onCloseClick = {}
            )
        }
    }
}
