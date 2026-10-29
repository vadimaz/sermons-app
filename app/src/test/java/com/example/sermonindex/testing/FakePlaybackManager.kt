package com.example.sermonindex.testing

import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.playback.PlaybackManager
import com.example.sermonindex.playback.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reusable Fake implementation of [PlaybackManager] for ViewModel unit tests.
 *
 * Avoids Android MediaController/Service binding by passing `context = null` and exposes
 * a mutable [playbackStateFlow] along with recorded invocation properties.
 */
class FakePlaybackManager : PlaybackManager(null) {

    val playbackStateFlow = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = playbackStateFlow.asStateFlow()

    var lastPlayedSermon: Sermon? = null
    var lastStartPositionMs: Long = 0L
    var togglePlayPauseCalled: Boolean = false

    override fun playSermon(sermon: Sermon, startPositionMs: Long) {
        lastPlayedSermon = sermon
        lastStartPositionMs = startPositionMs
    }

    override fun togglePlayPause() {
        togglePlayPauseCalled = true
    }

    fun reset() {
        playbackStateFlow.value = PlaybackState()
        lastPlayedSermon = null
        lastStartPositionMs = 0L
        togglePlayPauseCalled = false
    }
}
