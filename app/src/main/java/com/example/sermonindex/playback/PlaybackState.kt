package com.example.sermonindex.playback

import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SleepTimerState

data class PlaybackState(
    val currentSermon: Sermon? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isUsingFailoverCdn: Boolean = false,
    val isLocalAudio: Boolean = false,
    val sleepTimerState: SleepTimerState = SleepTimerState.Off,
    val errorMessage: String? = null
) {
    val hasActiveMedia: Boolean
        get() = currentSermon != null

    val progressFraction: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
