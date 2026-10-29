package com.example.sermonindex.ui.sermon.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.theme.SermonIndexTheme
import com.example.sermonindex.theme.ThemePreviews
import com.example.sermonindex.ui.components.PreviewFixtures
import com.example.sermonindex.ui.search.components.TextHighlightUtils

@Composable
fun SermonOverviewSection(
    detail: SermonDetail,
    highlightQuery: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (detail.summary.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.summary),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (highlightQuery.isNullOrBlank()) {
                        Text(text = detail.summary, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        val highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        val activeHighlightColor = MaterialTheme.colorScheme.primaryContainer
                        val annotated = remember(detail.summary, highlightQuery, highlightColor, activeHighlightColor) {
                            TextHighlightUtils.buildHighlightedText(
                                text = detail.summary,
                                query = highlightQuery,
                                highlightColor = highlightColor,
                                activeHighlightColor = activeHighlightColor
                            )
                        }
                        Text(text = annotated, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (detail.scriptureCitations.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.scripture_citations),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    detail.scriptureCitations.forEach { citation ->
                        Text(
                            text = "• $citation",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (detail.description.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.detailed_description),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = detail.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SermonOverviewSectionPreview() {
    SermonIndexTheme {
        Surface {
            SermonOverviewSection(
                detail = PreviewFixtures.previewSermonDetail,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
