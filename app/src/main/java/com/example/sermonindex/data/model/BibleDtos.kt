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
data class BibleChapterResponse(
    val numberOfVerses: Int = 0,
    val chapter: BibleChapterContentDto? = null
)

@Serializable
data class BibleChapterContentDto(
    val number: Int = 0,
    val content: List<JsonElement> = emptyList()
)
