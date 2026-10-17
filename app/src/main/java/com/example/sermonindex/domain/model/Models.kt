package com.example.sermonindex.domain.model

/**
 * Pure domain model representing a sermon in the SermonIndex catalog.
 * Decoupled from network serialization and database schemas.
 */
data class Sermon(
    val id: String,
    val title: String,
    val speaker: String,
    val speakerImage: String = "",
    val mp3Url: String = "",
    val cdnMp3Url: String = "",
    val vttUrl: String = "",
    val hasAudio: Boolean = true,
    val duration: String = "",
    val thumbnailUrl: String = "",
    val summary: String = "",
    val topics: List<Topic> = emptyList()
) {
    val isTextOnly: Boolean
        get() = !hasAudio && mp3Url.isBlank() && cdnMp3Url.isBlank()
}

/**
 * Detailed sermon domain model including transcript, outline, quotes, and FAQ.
 */
data class SermonDetail(
    val id: String,
    val title: String,
    val speaker: String,
    val speakerImage: String = "",
    val speakerSlug: String = "",
    val mp3Url: String = "",
    val cdnMp3Url: String = "",
    val vttUrl: String = "",
    val hasAudio: Boolean = true,
    val duration: String = "",
    val thumbnailUrl: String = "",
    val summary: String = "",
    val description: String = "",
    val scriptureCitations: List<String> = emptyList(),
    val outline: List<OutlineItem> = emptyList(),
    val quotes: List<QuoteItem> = emptyList(),
    val applicationPoints: List<String> = emptyList(),
    val faq: List<FaqItem> = emptyList(),
    val transcript: String = "",
    val transcriptHtml: String = ""
) {
    val isTextOnly: Boolean
        get() = !hasAudio && mp3Url.isBlank() && cdnMp3Url.isBlank()

    fun toSermon(): Sermon = Sermon(
        id = id,
        title = title,
        speaker = speaker,
        speakerImage = speakerImage,
        mp3Url = mp3Url,
        cdnMp3Url = cdnMp3Url,
        vttUrl = vttUrl,
        hasAudio = hasAudio,
        duration = duration,
        thumbnailUrl = thumbnailUrl,
        summary = summary
    )
}

/**
 * Represents a single time-coded subtitle/transcript cue from WebVTT.
 */
data class TranscriptCue(
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String
)

data class OutlineItem(
    val timestamp: String,
    val timeSeconds: Long,
    val title: String
)

data class QuoteItem(
    val quote: String,
    val topic: String
)

data class FaqItem(
    val question: String,
    val answer: String
)

data class Speaker(
    val id: String,
    val name: String,
    val slug: String,
    val image: String = "",
    val sermonCount: Int = 0,
    val audioCount: Int = 0,
    val bio: String = "",
    val totalPages: Int = 1,
    val topTopics: List<Topic> = emptyList()
)

data class SpeakerPagedSermons(
    val sermons: List<Sermon>,
    val currentPage: Int,
    val totalPages: Int,
    val totalSermons: Int
)

data class Topic(
    val id: String,
    val name: String,
    val slug: String,
    val count: Int = 0
)

data class TopicScripture(
    val text: String = "",
    val bookId: String = "",
    val chapter: Int = 0,
    val verse: Int = 0,
    val count: Int = 0,
    val bsb: String = ""
)

data class TopicDetail(
    val id: String,
    val name: String,
    val slug: String,
    val description: String,
    val topScriptures: List<TopicScripture> = emptyList(),
    val sermons: List<Sermon> = emptyList()
)

data class BibleBook(
    val id: String,
    val name: String,
    val testament: String,
    val chapterCount: Int
)

data class BibleTranslation(
    val id: String,
    val name: String,
    val englishName: String,
    val shortName: String,
    val language: String,
    val languageName: String = "",
    val languageEnglishName: String = "",
    val numberOfBooks: Int = 66
)

data class ChapterDetails(
    val bookId: String,
    val bookName: String,
    val chapter: Int,
    val verseCount: Int,
    val verseHeatMap: Map<Int, Int> = emptyMap()
)

data class FavoriteSermon(
    val id: String,
    val title: String,
    val speaker: String,
    val speakerImage: String,
    val duration: String,
    val addedAt: Long
) {
    fun toSermon(): Sermon = Sermon(
        id = id,
        title = title,
        speaker = speaker,
        speakerImage = speakerImage,
        duration = duration
    )
}

data class ListeningHistory(
    val sermonId: String,
    val title: String,
    val speaker: String,
    val speakerImage: String,
    val lastPositionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val completed: Boolean
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (lastPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    fun toSermon(): Sermon = Sermon(
        id = sermonId,
        title = title,
        speaker = speaker,
        speakerImage = speakerImage,
        duration = ""
    )
}

/**
 * Pure domain model representing an offline downloaded sermon.
 */
data class DownloadedSermon(
    val id: String,
    val title: String,
    val speaker: String,
    val speakerImage: String = "",
    val duration: String = "",
    val localPath: String,
    val fileSizeBytes: Long,
    val downloadedAt: Long
) {
    fun toSermon(): Sermon = Sermon(
        id = id,
        title = title,
        speaker = speaker,
        speakerImage = speakerImage,
        duration = duration
    )

    val formattedSize: String
        get() {
            val mb = fileSizeBytes.toDouble() / (1024.0 * 1024.0)
            return if (mb >= 1.0) {
                "${(mb * 10).toLong() / 10.0} MB"
            } else {
                val kb = fileSizeBytes / 1024
                "$kb KB"
            }
        }
}

/**
 * Domain representation of sleep timer status.
 */
sealed interface SleepTimerState {
    data object Off : SleepTimerState
    data class Active(val remainingMillis: Long, val totalMillis: Long) : SleepTimerState
    data object EndOfSermon : SleepTimerState
}

