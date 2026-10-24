package com.example.sermonindex

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Serializable
data object SearchRoute : NavKey

@Serializable
data object BibleRoute : NavKey

@Serializable
data object ScriptureRoute : NavKey

@Serializable
data class BibleVerseSermonsRoute(
    val bookId: String,
    val bookName: String,
    val chapter: Int,
    val verse: Int
) : NavKey

@Serializable
data object SpeakersRoute : NavKey

@Serializable
data object TopicsRoute : NavKey

@Serializable
data object LibraryRoute : NavKey

@Serializable
data class SpeakerDetailRoute(val slug: String) : NavKey

@Serializable
data class TopicDetailRoute(val slug: String) : NavKey

@Serializable
data class SermonDetailRoute(val sermonId: String, val highlightQuery: String? = null) : NavKey

@Serializable
data object PlayerRoute : NavKey

@Serializable
data object FullscreenVideoRoute : NavKey

@Serializable
data class TextSermonRoute(val sermonId: String, val highlightQuery: String? = null) : NavKey
