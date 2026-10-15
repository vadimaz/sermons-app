package com.example.sermonindex.playback

import com.example.sermonindex.domain.model.Sermon

data class PlaybackState(
    val currentSermon: Sermon? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isUsingFailoverCdn: Boolean = false,
    val errorMessage: String? = null
) {
    val hasActiveMedia: Boolean
        get() = currentSermon != null

    val progressFraction: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
