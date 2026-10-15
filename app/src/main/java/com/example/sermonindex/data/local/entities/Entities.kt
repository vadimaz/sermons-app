package com.example.sermonindex.data.local.entities

data class FavoriteSermonEntity(
    val id: String,
    val title: String,
    val speaker: String,
    val speakerImage: String,
    val duration: String,
    val addedAt: Long = System.currentTimeMillis()
)

data class HistoryEntity(
    val sermonId: String,
    val title: String = "",
    val speaker: String = "",
    val speakerImage: String = "",
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    val completed: Boolean = false
)

data class DownloadedSermonEntity(
    val id: String,
    val title: String,
    val speaker: String,
    val speakerImage: String,
    val duration: String,
    val localPath: String,
    val fileSizeBytes: Long,
    val downloadedAt: Long = System.currentTimeMillis()
)
