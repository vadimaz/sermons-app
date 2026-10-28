package com.example.sermonindex.ui.scripture

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.BibleTranslation

enum class TranslationPickerTarget {
    FIRST,
    SECOND
}

private val POPULAR_TRANSLATION_IDS = listOf(
    "BSB",
    "rus_syn",
    "ukr_1996",
    "eng_kjv",
    "spa_r09",
    "deu_elbbk"
)

fun getLanguageFlag(language: String?, translationId: String? = null): String {
    val lang = language?.lowercase() ?: ""
    val id = translationId?.lowercase() ?: ""
    return when {
        lang == "rus" || id.contains("rus") -> "🇷🇺"
        lang == "ukr" || id.contains("ukr") -> "🇺🇦"
        lang == "eng" && (id.contains("kjv") || id.contains("cpb")) -> "🇬🇧"
        lang == "eng" -> "🇺🇸"
        lang == "spa" || id.contains("spa") -> "🇪🇸"
        lang == "deu" || id.contains("deu") -> "🇩🇪"
        lang == "fra" -> "🇫🇷"
        lang == "por" -> "🇧🇷"
        lang == "ita" -> "🇮🇹"
        lang == "heb" -> "🇮🇱"
        lang == "grc" || lang == "ell" -> "🇬🇷"
        lang == "lat" -> "🇻🇦"
        lang == "ron" -> "🇷🇴"
        lang == "pol" -> "🇵🇱"
        lang == "nld" -> "🇳🇱"
        lang == "kor" -> "🇰🇷"
        lang == "zho" || lang == "cmn" -> "🇨🇳"
        lang == "jpn" -> "🇯🇵"
        else -> "📖"
    }
}

/**
 * Scripture Verse Quote Card displaying dual translations by default.
 * Translation 1 defaults to BSB, Translation 2 defaults to the user's system language (or KJV fallback).
 */
@Composable
fun ScriptureVerseQuoteCard(
    bookName: String,
    chapter: Int,
    verse: Int,
    firstTranslation: BibleTranslation?,
    secondTranslation: BibleTranslation?,
    firstVerseText: String?,
    secondVerseText: String?,
    isLoadingFirstText: Boolean,
    isLoadingSecondText: Boolean,
    onSelectFirstTranslation: () -> Unit,
    onSelectSecondTranslation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Reference badge + Dual Translation chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Scripture Reference Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$bookName $chapter:$verse",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Translation selector chips [ 🇺🇸 BSB ▾ ] [ 🇷🇺 SYN ▾ ]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TranslationChip(
                        flag = getLanguageFlag(firstTranslation?.language, firstTranslation?.id),
                        label = firstTranslation?.shortName ?: firstTranslation?.id ?: "BSB",
                        onClick = onSelectFirstTranslation
                    )

                    TranslationChip(
                        flag = getLanguageFlag(secondTranslation?.language, secondTranslation?.id),
                        label = secondTranslation?.shortName ?: secondTranslation?.id ?: "SYN",
                        onClick = onSelectSecondTranslation
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body: Dual Translation comparative display
            Column(modifier = Modifier.fillMaxWidth()) {
                // Section 1: First Translation (BSB by default)
                ParallelVerseSection(
                    flag = getLanguageFlag(firstTranslation?.language, firstTranslation?.id),
                    translationName = firstTranslation?.name ?: firstTranslation?.englishName ?: "Berean Standard Bible",
                    verseText = firstVerseText,
                    isLoading = isLoadingFirstText
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Section 2: Second Translation (System Language / KJV by default)
                ParallelVerseSection(
                    flag = getLanguageFlag(secondTranslation?.language, secondTranslation?.id),
                    translationName = secondTranslation?.name ?: secondTranslation?.englishName ?: "Translation",
                    verseText = secondVerseText,
                    isLoading = isLoadingSecondText
                )
            }
        }
    }
}

@Composable
private fun TranslationChip(
    flag: String,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(text = flag, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ParallelVerseSection(
    flag: String,
    translationName: String,
    verseText: String?,
    isLoading: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = flag, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = translationName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (isLoading) {
            VerseLoadingIndicator()
        } else if (!verseText.isNullOrBlank()) {
            Text(
                text = "«$verseText»",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = stringResource(R.string.verse_text_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun VerseLoadingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.loading_verse_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * BottomSheet allowing the user to select from popular translations or search across all 1,250+ Bible versions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationPickerBottomSheet(
    target: TranslationPickerTarget,
    currentTranslationId: String,
    availableTranslations: List<BibleTranslation>,
    onSelectTranslation: (BibleTranslation) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredTranslations = remember(searchQuery, availableTranslations) {
        if (searchQuery.isBlank()) {
            availableTranslations
        } else {
            val q = searchQuery.trim().lowercase()
            availableTranslations.filter {
                it.name.lowercase().contains(q) ||
                    it.englishName.lowercase().contains(q) ||
                    it.shortName.lowercase().contains(q) ||
                    it.id.lowercase().contains(q) ||
                    it.language.lowercase().contains(q) ||
                    it.languageName.lowercase().contains(q) ||
                    it.languageEnglishName.lowercase().contains(q)
            }
        }
    }

    val popularTranslations = remember(availableTranslations) {
        POPULAR_TRANSLATION_IDS.mapNotNull { pid ->
            availableTranslations.find { it.id.equals(pid, ignoreCase = true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(max = 600.dp)
        ) {
            // Header with title and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (target == TranslationPickerTarget.FIRST) {
                        stringResource(R.string.first_translation)
                    } else {
                        stringResource(R.string.second_translation)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(
                            R.string.search_translations_placeholder,
                            availableTranslations.size.coerceAtLeast(1)
                        )
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Translations List
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Show popular section only when not searching
                if (searchQuery.isBlank() && popularTranslations.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.popular_translations),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }

                    items(popularTranslations, key = { "pop_${it.id}" }) { translation ->
                        TranslationItem(
                            translation = translation,
                            isSelected = translation.id.equals(currentTranslationId, ignoreCase = true),
                            onClick = { onSelectTranslation(translation) }
                        )
                    }

                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = stringResource(R.string.all_translations, filteredTranslations.size),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                items(filteredTranslations, key = { it.id }) { translation ->
                    TranslationItem(
                        translation = translation,
                        isSelected = translation.id.equals(currentTranslationId, ignoreCase = true),
                        onClick = { onSelectTranslation(translation) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslationItem(
    translation: BibleTranslation,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = getLanguageFlag(translation.language, translation.id),
                fontSize = 20.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = translation.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = translation.shortName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (translation.englishName.isNotBlank() && !translation.englishName.equals(translation.name, ignoreCase = true)) {
                    Text(
                        text = translation.englishName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
