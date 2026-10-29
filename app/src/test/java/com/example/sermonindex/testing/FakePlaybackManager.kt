package com.example.sermonindex.testing

import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.playback.ActiveMediaInfo
import com.example.sermonindex.playback.PlaybackManager
import com.example.sermonindex.playback.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Reusable Fake implementation of [PlaybackManager] for ViewModel unit tests.
 *
 * Avoids Android MediaController/Service binding by passing `context = null` and exposes
 * a mutable [playbackStateFlow] along with recorded invocation properties.
 */
class FakePlaybackManager : PlaybackManager(null) {

    private val fakeScope = CoroutineScope(Dispatchers.Unconfined)
    val playbackStateFlow = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = playbackStateFlow.asStateFlow()

    override val activeMediaInfo: StateFlow<ActiveMediaInfo> = playbackStateFlow
        .map { it.toActiveMediaInfo() }
        .distinctUntilChanged()
        .stateIn(
            scope = fakeScope,
            started = SharingStarted.Eagerly,
            initialValue = playbackStateFlow.value.toActiveMediaInfo()
        )

    override val progressFraction: Float
        get() = playbackStateFlow.value.progressFraction

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

    fun setPlaybackState(state: PlaybackState) {
        playbackStateFlow.value = state
    }

    fun reset() {
        playbackStateFlow.value = PlaybackState()
        lastPlayedSermon = null
        lastStartPositionMs = 0L
        togglePlayPauseCalled = false
    }
}
