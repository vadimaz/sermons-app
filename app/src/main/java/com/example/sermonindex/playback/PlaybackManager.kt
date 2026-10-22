package com.example.sermonindex.playback

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.Sermon
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
open class PlaybackManager(private val context: Context? = null) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionPollJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    open val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var activeSermon: Sermon? = null
    private var pendingPlay: Pair<Sermon, Long>? = null

    init {
        if (context != null) {
            connectToService()
        }
    }

    private fun connectToService() {
        val ctx = context ?: return
        val sessionToken = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(ctx, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()?.apply {
                    addListener(PlayerListener())
                    syncStateWithPlayer(this)
                    pendingPlay?.let { (pendingSermon, pos) ->
                        pendingPlay = null
                        playSermon(pendingSermon, pos)
                    }
                }
            } catch (e: Exception) {
                _playbackState.value = _playbackState.value.copy(errorMessage = e.message)
            }
        }, MoreExecutors.directExecutor())
    }

    open fun playSermon(sermon: Sermon, startPositionMs: Long = 0L) {
        if (!sermon.hasAudio && sermon.mp3Url.isBlank() && sermon.cdnMp3Url.isBlank()) {
            _playbackState.value = _playbackState.value.copy(
                errorMessage = context?.getString(R.string.no_audio_stream_available) ?: "No audio stream available"
            )
            return
        }

        activeSermon = sermon
        val controller = mediaController ?: run {
            pendingPlay = sermon to startPositionMs
            connectToService()
            return
        }

        val localFile = context?.filesDir?.let { java.io.File(it, "audio_downloads/${sermon.id}.mp3") }
        val isLocal = localFile != null && localFile.exists() && localFile.length() > 0
        val playUrl = if (isLocal) {
            "file://${localFile.absolutePath}"
        } else {
            sermon.mp3Url.ifBlank { sermon.cdnMp3Url }
        }
        val cdnUrl = sermon.cdnMp3Url

        if (playUrl.isBlank()) {
            _playbackState.value = _playbackState.value.copy(
                errorMessage = context?.getString(R.string.no_audio_stream_available) ?: "No audio stream available"
            )
            return
        }

        val mediaItem = PlaybackService.buildMediaItem(
            url = playUrl,
            title = sermon.title,
            speaker = sermon.speaker,
            speakerImage = sermon.speakerImage,
            sermonId = sermon.id,
            isFailover = false,
            primaryUrl = playUrl,
            cdnUrl = cdnUrl,
            isLocal = isLocal
        )

        controller.setMediaItem(mediaItem)
        if (startPositionMs > 0) {
            controller.seekTo(startPositionMs)
        }
        controller.prepare()
        controller.play()

        _playbackState.value = _playbackState.value.copy(
            currentSermon = sermon,
            isPlaying = true,
            isBuffering = true,
            errorMessage = null,
            currentPositionMs = startPositionMs,
            isLocalAudio = isLocal
        )

        startPositionPolling()
    }

    private var sleepTimerJob: Job? = null

    fun setSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            cancelSleepTimer()
            return
        }

        val totalMs = minutes * 60 * 1000L
        var remainingMs = totalMs
        _playbackState.value = _playbackState.value.copy(
            sleepTimerState = com.example.sermonindex.domain.model.SleepTimerState.Active(remainingMs, totalMs)
        )

        sleepTimerJob = scope.launch {
            while (isActive && remainingMs > 0) {
                delay(1000L)
                remainingMs = (remainingMs - 1000L).coerceAtLeast(0L)
                _playbackState.value = _playbackState.value.copy(
                    sleepTimerState = com.example.sermonindex.domain.model.SleepTimerState.Active(remainingMs, totalMs)
                )
            }
            if (isActive) {
                pause()
                _playbackState.value = _playbackState.value.copy(
                    sleepTimerState = com.example.sermonindex.domain.model.SleepTimerState.Off
                )
            }
        }
    }

    fun setSleepTimerEndOfSermon() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _playbackState.value = _playbackState.value.copy(
            sleepTimerState = com.example.sermonindex.domain.model.SleepTimerState.EndOfSermon
        )
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _playbackState.value = _playbackState.value.copy(
            sleepTimerState = com.example.sermonindex.domain.model.SleepTimerState.Off
        )
    }

    fun resume() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun rewind(seconds: Int = 15) {
        val controller = mediaController ?: return
        val newPos = (controller.currentPosition - seconds * 1000L).coerceAtLeast(0L)
        controller.seekTo(newPos)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = newPos)
    }

    fun fastForward(seconds: Int = 30) {
        val controller = mediaController ?: return
        val duration = controller.duration.coerceAtLeast(0L)
        val newPos = if (duration > 0) {
            (controller.currentPosition + seconds * 1000L).coerceAtMost(duration)
        } else {
            controller.currentPosition + seconds * 1000L
        }
        controller.seekTo(newPos)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = newPos)
    }

    fun setSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
        _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
    }

    fun stop() {
        mediaController?.stop()
        _playbackState.value = PlaybackState()
        stopPositionPolling()
    }

    private fun startPositionPolling() {
        positionPollJob?.cancel()
        positionPollJob = scope.launch {
            while (isActive) {
                delay(500L)
                mediaController?.let { syncStateWithPlayer(it) }
            }
        }
    }

    private fun stopPositionPolling() {
        positionPollJob?.cancel()
        positionPollJob = null
    }

    private fun syncStateWithPlayer(player: Player) {
        val isFailover = player.currentMediaItem?.mediaMetadata?.extras?.getBoolean(PlaybackService.EXTRA_IS_FAILOVER) ?: false
        val isLocal = player.currentMediaItem?.mediaMetadata?.extras?.getBoolean(PlaybackService.EXTRA_IS_LOCAL) ?: false
        val duration = player.duration.coerceAtLeast(0L)
        val position = player.currentPosition.coerceAtLeast(0L)
        val buffered = player.bufferedPosition.coerceAtLeast(0L)
        val isBuffering = player.playbackState == Player.STATE_BUFFERING

        _playbackState.value = _playbackState.value.copy(
            currentSermon = activeSermon,
            isPlaying = player.isPlaying,
            isBuffering = isBuffering,
            currentPositionMs = position,
            durationMs = duration,
            bufferedPositionMs = buffered,
            playbackSpeed = player.playbackParameters.speed,
            isUsingFailoverCdn = isFailover,
            isLocalAudio = isLocal
        )
    }

    private inner class PlayerListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            mediaController?.let { syncStateWithPlayer(it) }
            if (isPlaying) {
                startPositionPolling()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            mediaController?.let { syncStateWithPlayer(it) }
            if (playbackState == Player.STATE_ENDED) {
                stopPositionPolling()
                if (_playbackState.value.sleepTimerState is com.example.sermonindex.domain.model.SleepTimerState.EndOfSermon) {
                    cancelSleepTimer()
                    pause()
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaController?.let { syncStateWithPlayer(it) }
        }
    }
}
