package com.example.sermonindex.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TopicItemDto(
    val name: String = "",
    val slug: String = ""
)

@Serializable
data class TopicCountDto(
    val name: String = "",
    val slug: String = "",
    val count: Int = 0
)

@Serializable
data class TopicScriptureDto(
    val text: String = "",
    val bookId: String = "",
    val chapter: Int = 0,
    val verse: Int = 0,
    val count: Int = 0,
    val bsb: String = ""
)

@Serializable
data class ScriptureRefDto(
    val text: String = "",
    val bookId: String = "",
    val book: String = "",
    val chapter: Int = 0,
    val verse: Int = 0
)

@Serializable
data class OutlineItemDto(
    val heading: String = ""
)

@Serializable
data class FaqItemDto(
    val q: String = "",
    val a: String = ""
)

@Serializable
data class SermonDto(
    val id: String = "",
    val slug: String = "",
    val title: String = "",
    val url: String = "",
    val apiUrl: String = "",
    val speaker: String = "",
    val speakerSlug: String = "",
    val speakerUrl: String = "",
    val speakerImage: String = "",
    val summary: String = "",
    val description: String = "",
    val sermonType: String = "",
    val mediaType: String = "",
    val duration: String = "",
    val hasAudio: Boolean = false,
    val hasTranscript: Boolean = false,
    val videoUrl: String = "",
    val mp3Url: String = "",
    val cdnMp3Url: String = "",
    val mp4Url: String = "",
    val cdnMp4Url: String = "",
    val vttUrl: String = "",
    val pdfUrl: String = "",
    val txtUrl: String = "",
    val srtUrl: String = "",
    val thumbnailUrl: String = "",
    val image: String = "",
    val topics: List<TopicItemDto> = emptyList(),
    val scripture: List<ScriptureRefDto> = emptyList()
)

@Serializable
data class SermonDetailDto(
    val id: String = "",
    val slug: String = "",
    val title: String = "",
    val url: String = "",
    val apiUrl: String = "",
    val speaker: String = "",
    val speakerSlug: String = "",
    val speakerUrl: String = "",
    val speakerImage: String = "",
    val speakerBio: String = "",
    val summary: String = "",
    val description: String = "",
    val sermonType: String = "",
    val mediaType: String = "",
    val duration: String = "",
    val hasAudio: Boolean = false,
    val hasTranscript: Boolean = false,
    val videoUrl: String = "",
    val mp3Url: String = "",
    val cdnMp3Url: String = "",
    val mp4Url: String = "",
    val cdnMp4Url: String = "",
    val vttUrl: String = "",
    val pdfUrl: String = "",
    val txtUrl: String = "",
    val srtUrl: String = "",
    val thumbnailUrl: String = "",
    val image: String = "",
    val topics: List<TopicItemDto> = emptyList(),
    val scripture: List<ScriptureRefDto> = emptyList(),
    val outline: List<OutlineItemDto> = emptyList(),
    val quotes: List<String> = emptyList(),
    val applicationPoints: List<String> = emptyList(),
    val faq: List<FaqItemDto> = emptyList(),
    val transcriptHtml: String = "",
    val transcript: String = ""
)

@Serializable
data class PopularSermonsResponse(
    val count: Int = 0,
    val sermons: List<SermonDto> = emptyList()
)

@Serializable
data class SpeakerDto(
    val slug: String = "",
    val name: String = "",
    val bio: String = "",
    val image: String = "",
    val sermonCount: Int = 0,
    val audioCount: Int = 0,
    val topTopics: List<TopicCountDto> = emptyList(),
    val totalPages: Int = 0,
    val url: String = "",
    val apiUrl: String = ""
)

@Serializable
data class SpeakersResponse(
    val count: Int = 0,
    val speakers: List<SpeakerDto> = emptyList()
)

@Serializable
data class SpeakerPagedDto(
    val page: Int = 1,
    val pageSize: Int = 50,
    val totalPages: Int = 0,
    val total: Int = 0,
    val next: String? = null,
    val prev: String? = null,
    val sermons: List<SermonDto> = emptyList()
)

@Serializable
data class TopicDto(
    val slug: String = "",
    val name: String = "",
    val description: String = "",
    val sermonCount: Int = 0,
    val topScriptures: List<TopicScriptureDto> = emptyList(),
    val url: String = "",
    val apiUrl: String = ""
)

@Serializable
data class TopicsResponse(
    val count: Int = 0,
    val topics: List<TopicDto> = emptyList()
)

@Serializable
data class TopicDetailDto(
    val slug: String = "",
    val name: String = "",
    val description: String = "",
    val sermonCount: Int = 0,
    val topScriptures: List<TopicScriptureDto> = emptyList(),
    val sermons: List<SermonDto> = emptyList()
)

@Serializable
data class ScriptureBookDto(
    val bookId: String = "",
    val name: String = "",
    val testament: String = "",
    val order: Int = 0,
    val chapters: Int = 0,
    val sermonCount: Int = 0,
    val apiUrl: String = ""
)

@Serializable
data class ScriptureBooksResponse(
    val count: Int = 0,
    val totalReferences: Int = 0,
    val books: List<ScriptureBookDto> = emptyList()
)

@Serializable
data class VerseCountDto(
    val verse: Int = 0,
    val sermonCount: Int = 0,
    val apiUrl: String = ""
)

@Serializable
data class ScriptureChapterDto(
    val bookId: String = "",
    val book: String = "",
    val chapter: Int = 0,
    val reference: String = "",
    val sermonCount: Int = 0,
    val verses: List<VerseCountDto> = emptyList()
)

@Serializable
data class ScriptureVerseResponse(
    val bookId: String = "",
    val book: String = "",
    val chapter: Int = 0,
    val verse: Int = 0,
    val reference: String = "",
    val sermonCount: Int = 0,
    val sermons: List<SermonDto> = emptyList()
)
