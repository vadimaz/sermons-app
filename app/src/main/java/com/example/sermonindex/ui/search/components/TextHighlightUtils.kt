package com.example.sermonindex.ui.search.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

object TextHighlightUtils {

    private val WHITESPACE_REGEX = Regex("\\s+")
    private val TOKEN_DELIMITER_REGEX = Regex("[^\\p{L}\\p{Nd}]+")

    fun extractSnippet(text: String, query: String, maxChars: Int = 130): String {
        if (text.isBlank() || query.isBlank()) return text.take(maxChars)
        val cleanText = text.replace(WHITESPACE_REGEX, " ").trim()
        val lowerText = cleanText.lowercase()
        val tokens = query.trim().lowercase().split(TOKEN_DELIMITER_REGEX).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return cleanText.take(maxChars)

        // Find earliest match index among tokens
        var firstMatchIndex = -1
        for (token in tokens) {
            val idx = lowerText.indexOf(token)
            if (idx >= 0 && (firstMatchIndex == -1 || idx < firstMatchIndex)) {
                firstMatchIndex = idx
            }
        }
        if (firstMatchIndex == -1) return cleanText.take(maxChars)

        val half = maxChars / 2
        val start = (firstMatchIndex - half).coerceAtLeast(0)
        val end = (start + maxChars).coerceAtMost(cleanText.length)

        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < cleanText.length) "…" else ""

        return prefix + cleanText.substring(start, end).trim() + suffix
    }

    fun buildHighlightedText(
        text: String,
        query: String,
        highlightColor: Color = Color(0x55FFB300),
        activeHighlightColor: Color = Color(0xFFFFB300),
        activeMatchRange: IntRange? = null
    ): AnnotatedString {
        if (query.isBlank() || text.isBlank()) return AnnotatedString(text)
        val tokens = query.trim().lowercase().split(TOKEN_DELIMITER_REGEX).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return AnnotatedString(text)

        val lowerText = text.lowercase()
        val matchRanges = mutableListOf<IntRange>()

        for (token in tokens) {
            var searchStart = 0
            while (searchStart < lowerText.length) {
                val idx = lowerText.indexOf(token, searchStart)
                if (idx == -1) break
                matchRanges.add(idx until (idx + token.length))
                searchStart = idx + token.length
            }
        }

        return buildAnnotatedString {
            append(text)
            for (range in matchRanges) {
                val isActive = activeMatchRange != null &&
                    range.first >= activeMatchRange.first &&
                    range.last <= activeMatchRange.last

                addStyle(
                    style = SpanStyle(
                        background = if (isActive) activeHighlightColor else highlightColor,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                    ),
                    start = range.first,
                    end = range.last + 1
                )
            }
        }
    }
}
