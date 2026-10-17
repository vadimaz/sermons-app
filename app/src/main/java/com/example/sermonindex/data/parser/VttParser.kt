package com.example.sermonindex.data.parser

import com.example.sermonindex.domain.model.TranscriptCue

/**
 * Pure Kotlin parser for WebVTT subtitle/transcript files.
 * Transforms timestamped WebVTT cues into [TranscriptCue] domain models.
 */
object VttParser {

    private val HTML_TAG_REGEX = Regex("<[^>]*>")
    private val CUE_TIMING_REGEX = Regex("""(\d{1,2}:(?:\d{2}:)?\d{2}[.,]\d{3})\s*-->\s*(\d{1,2}:(?:\d{2}:)?\d{2}[.,]\d{3})""")

    fun parse(vttContent: String): List<TranscriptCue> {
        if (vttContent.isBlank()) return emptyList()

        val lines = vttContent.lineSequence().map { it.trimEnd() }.iterator()
        val cues = mutableListOf<TranscriptCue>()

        var currentStartMs = -1L
        var currentEndMs = -1L
        val currentTextLines = mutableListOf<String>()
        var cueIndex = 0

        fun flushCurrentCue() {
            if (currentStartMs >= 0L && currentEndMs > currentStartMs) {
                val fullText = currentTextLines
                    .joinToString(" ")
                    .replace(HTML_TAG_REGEX, "")
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .trim()

                if (fullText.isNotBlank()) {
                    cues.add(
                        TranscriptCue(
                            index = cueIndex++,
                            startMs = currentStartMs,
                            endMs = currentEndMs,
                            text = fullText
                        )
                    )
                }
            }
            currentStartMs = -1L
            currentEndMs = -1L
            currentTextLines.clear()
        }

        while (lines.hasNext()) {
            val line = lines.next().trim()

            if (line.isEmpty()) {
                flushCurrentCue()
                continue
            }

            // Skip WEBVTT header or NOTE blocks
            if (line.startsWith("WEBVTT") || line.startsWith("NOTE") || line.startsWith("STYLE")) {
                continue
            }

            val timingMatch = CUE_TIMING_REGEX.find(line)
            if (timingMatch != null) {
                flushCurrentCue()
                val (startStr, endStr) = timingMatch.destructured
                currentStartMs = parseTimestamp(startStr)
                currentEndMs = parseTimestamp(endStr)
            } else if (currentStartMs >= 0L) {
                // Text line belonging to the active timing cue
                currentTextLines.add(line)
            }
        }

        // Flush last cue if file didn't end with newline
        flushCurrentCue()

        return cues
    }

    private fun parseTimestamp(ts: String): Long {
        val parts = ts.trim().split(":")
        return when (parts.size) {
            3 -> {
                val hours = parts[0].toLongOrNull() ?: 0L
                val minutes = parts[1].toLongOrNull() ?: 0L
                val secMillis = parts[2].split(".", ",")
                val seconds = secMillis[0].toLongOrNull() ?: 0L
                val millis = if (secMillis.size > 1) {
                    secMillis[1].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                } else 0L
                hours * 3_600_000L + minutes * 60_000L + seconds * 1_000L + millis
            }
            2 -> {
                val minutes = parts[0].toLongOrNull() ?: 0L
                val secMillis = parts[1].split(".", ",")
                val seconds = secMillis[0].toLongOrNull() ?: 0L
                val millis = if (secMillis.size > 1) {
                    secMillis[1].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                } else 0L
                minutes * 60_000L + seconds * 1_000L + millis
            }
            else -> 0L
        }
    }
}
