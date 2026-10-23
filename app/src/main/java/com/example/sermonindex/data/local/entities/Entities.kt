package com.example.sermonindex.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteSermonEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val speaker: String,
    @ColumnInfo(name = "speaker_image")
    val speakerImage: String,
    val duration: String,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "sermon_id")
    val sermonId: String,
    val title: String = "",
    val speaker: String = "",
    @ColumnInfo(name = "speaker_image")
    val speakerImage: String = "",
    @ColumnInfo(name = "last_position_ms")
    val lastPositionMs: Long = 0L,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0L,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    val completed: Boolean = false
)

@Entity(tableName = "downloads")
data class DownloadedSermonEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val speaker: String,
    @ColumnInfo(name = "speaker_image")
    val speakerImage: String,
    val duration: String,
    @ColumnInfo(name = "local_path")
    val localPath: String,
    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long,
    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "catalog_metadata")
data class CatalogMetadataEntity(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo(name = "sermon_count")
    val sermonCount: Int,
    @ColumnInfo(name = "generated_at")
    val generatedAt: String,
    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sermon_fts")
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
data class SermonFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int = 0,
    @ColumnInfo(name = "sermon_id")
    val sermonId: String,
    val title: String,
    val speaker: String,
    val summary: String,
    val duration: String,
    @ColumnInfo(name = "speaker_image")
    val speakerImage: String,
    @ColumnInfo(name = "has_audio")
    val hasAudio: Int,
    @ColumnInfo(name = "mp3_url")
    val mp3Url: String,
    @ColumnInfo(name = "cdn_mp3_url")
    val cdnMp3Url: String
)

data class FtsSermonRecord(
    val id: String,
    val title: String,
    val speaker: String,
    val summary: String,
    val duration: String,
    val speakerImage: String,
    val hasAudio: Boolean,
    val mp3Url: String = "",
    val cdnMp3Url: String = ""
)
