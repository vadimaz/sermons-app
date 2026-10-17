package com.example.sermonindex

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Serializable
data object ScriptureRoute : NavKey

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
data class SermonDetailRoute(val sermonId: String) : NavKey

@Serializable
data object PlayerRoute : NavKey

@Serializable
data class TextSermonRoute(val sermonId: String) : NavKey
