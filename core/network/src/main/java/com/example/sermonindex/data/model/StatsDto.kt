package com.example.sermonindex.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiStatsDto(
    val generated: String = "",
    val sermons: Int = 0,
    val speakers: Int = 0,
    val topics: Int = 0
)
