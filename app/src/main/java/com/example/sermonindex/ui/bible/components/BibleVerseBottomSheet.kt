package com.example.sermonindex.ui.bible.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.HighlightColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleVerseBottomSheet(
    visible: Boolean,
    verseNumber: Int?,
    currentColor: HighlightColor?,
    onDismissRequest: () -> Unit,
    onColorSelected: (HighlightColor) -> Unit,
    onRemove: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (!visible || verseNumber == null) return

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header: Verse number label + utility actions (Copy, Share)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.verse_title_format, verseNumber),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCopy) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.action_copy),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.action_share),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Palette row: Color dots + Remove TextButton (visible only if currentColor != null)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HighlightColorDot(
                    color = Color(0xFFFFD54F),
                    isSelected = currentColor == HighlightColor.YELLOW,
                    contentDescription = stringResource(R.string.action_highlight_yellow),
                    onClick = { onColorSelected(HighlightColor.YELLOW) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                HighlightColorDot(
                    color = Color(0xFF81C784),
                    isSelected = currentColor == HighlightColor.GREEN,
                    contentDescription = stringResource(R.string.action_highlight_green),
                    onClick = { onColorSelected(HighlightColor.GREEN) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                HighlightColorDot(
                    color = Color(0xFF64B5F6),
                    isSelected = currentColor == HighlightColor.BLUE,
                    contentDescription = stringResource(R.string.action_highlight_blue),
                    onClick = { onColorSelected(HighlightColor.BLUE) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                HighlightColorDot(
                    color = Color(0xFFE57373),
                    isSelected = currentColor == HighlightColor.CORAL,
                    contentDescription = stringResource(R.string.action_highlight_coral),
                    onClick = { onColorSelected(HighlightColor.CORAL) }
                )

                Spacer(modifier = Modifier.weight(1f))

                if (currentColor != null) {
                    TextButton(onClick = onRemove) {
                        Text(
                            text = stringResource(R.string.action_remove),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightColorDot(
    color: Color,
    isSelected: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier.border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
                }
            )
            .clickable(onClick = onClick)
    )
}
