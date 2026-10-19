package com.example.sermonindex.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AvailableTranslationsResponse(
    val translations: List<BibleTranslationDto> = emptyList()
)

@Serializable
data class BibleTranslationDto(
    val id: String,
    val name: String = "",
    val englishName: String = "",
    val shortName: String = "",
    val language: String = "",
    val languageName: String = "",
    val languageEnglishName: String = "",
    val numberOfBooks: Int = 0
)

@Serializable
data class BibleTranslationBooksResponse(
    val books: List<BibleTranslationBookDto> = emptyList()
)

@Serializable
data class BibleTranslationBookDto(
    val id: String,
    val translationId: String = "",
    val name: String = "",
    val commonName: String = "",
    val title: String = "",
    val order: Int = 0,
    val numberOfChapters: Int = 0,
    val firstChapterNumber: Int = 1,
    val lastChapterNumber: Int = 1,
    val totalNumberOfVerses: Int = 0
)

@Serializable
data class BibleChapterResponse(
    val numberOfVerses: Int = 0,
    val book: BibleBookHeaderDto? = null,
    val chapter: BibleChapterContentDto? = null,
    val footnotes: List<BibleFootnoteDto> = emptyList(),
    val thisChapterAudioLinks: Map<String, String> = emptyMap(),
    val nextChapterApiLink: String? = null,
    val previousChapterApiLink: String? = null
)

@Serializable
data class BibleBookHeaderDto(
    val id: String = "",
    val translationId: String = "",
    val name: String = "",
    val order: Int = 0,
    val numberOfChapters: Int = 0
)

@Serializable
data class BibleChapterContentDto(
    val number: Int = 0,
    val content: List<JsonElement> = emptyList()
)

@Serializable
data class BibleFootnoteDto(
    val noteId: Int = 0,
    val caller: String = "",
    val text: String = "",
    val reference: BibleFootnoteRefDto? = null
)

@Serializable
data class BibleFootnoteRefDto(
    val chapter: Int = 0,
    val verse: Int = 0
)
