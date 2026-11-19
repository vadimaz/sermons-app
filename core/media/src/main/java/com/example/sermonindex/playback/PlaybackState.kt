package com.example.sermonindex.playback

import androidx.compose.runtime.Immutable
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SleepTimerState

@Immutable
data class ActiveMediaInfo(
    val currentSermon: Sermon? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isUsingFailoverCdn: Boolean = false,
    val isLocalAudio: Boolean = false
) {
    val hasActiveMedia: Boolean
        get() = currentSermon != null

    val isBibleTrack: Boolean
        get() = currentSermon?.id?.startsWith("bible_") == true
}

@Immutable
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
    val isVideoActive: Boolean = true,
    val sleepTimerState: SleepTimerState = SleepTimerState.Off,
    val errorMessage: String? = null
) {
    val hasActiveMedia: Boolean
        get() = currentSermon != null

    val hasVideo: Boolean
        get() = currentSermon?.hasVideo == true

    val progressFraction: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    fun toActiveMediaInfo(): ActiveMediaInfo = ActiveMediaInfo(
        currentSermon = currentSermon,
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        isUsingFailoverCdn = isUsingFailoverCdn,
        isLocalAudio = isLocalAudio
    )
}

