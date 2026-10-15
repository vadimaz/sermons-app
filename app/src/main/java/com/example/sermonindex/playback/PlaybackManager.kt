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
class PlaybackManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionPollJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var activeSermon: Sermon? = null

    init {
        connectToService()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()?.apply {
                    addListener(PlayerListener())
                    syncStateWithPlayer(this)
                }
            } catch (e: Exception) {
                _playbackState.value = _playbackState.value.copy(errorMessage = e.message)
            }
        }, MoreExecutors.directExecutor())
    }

    fun playSermon(sermon: Sermon, startPositionMs: Long = 0L) {
        if (!sermon.hasAudio && sermon.mp3Url.isBlank() && sermon.cdnMp3Url.isBlank()) {
            _playbackState.value = _playbackState.value.copy(
                errorMessage = context.getString(R.string.no_audio_stream_available)
            )
            return
        }

        activeSermon = sermon
        val controller = mediaController ?: run {
            connectToService()
            return
        }

        val primaryUrl = sermon.mp3Url.ifBlank { sermon.cdnMp3Url }
        val cdnUrl = sermon.cdnMp3Url

        val mediaItem = PlaybackService.buildMediaItem(
            url = primaryUrl,
            title = sermon.title,
            speaker = sermon.speaker,
            speakerImage = sermon.speakerImage,
            sermonId = sermon.id,
            isFailover = false,
            primaryUrl = primaryUrl,
            cdnUrl = cdnUrl
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
            currentPositionMs = startPositionMs
        )

        startPositionPolling()
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
            isUsingFailoverCdn = isFailover
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
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaController?.let { syncStateWithPlayer(it) }
        }
    }
}
