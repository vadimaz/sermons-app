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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    private val _activeMediaInfo = MutableStateFlow(ActiveMediaInfo())
    open val activeMediaInfo: StateFlow<ActiveMediaInfo> = _activeMediaInfo.asStateFlow()

    open val progressFraction: Float
        get() = _playbackState.value.progressFraction

    private fun setPlaybackState(newState: PlaybackState) {
        _playbackState.value = newState
        val newInfo = newState.toActiveMediaInfo()
        if (_activeMediaInfo.value != newInfo) {
            _activeMediaInfo.value = newInfo
        }
    }

    open val player: Player?
        get() = mediaController

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
                setPlaybackState(_playbackState.value.copy(errorMessage = e.message))
            }
        }, MoreExecutors.directExecutor())
    }

    open fun playSermon(sermon: Sermon, startPositionMs: Long = 0L) {
        val hasStream = (sermon.hasAudio && (sermon.mp3Url.isNotBlank() || sermon.cdnMp3Url.isNotBlank())) ||
                sermon.hasVideo ||
                sermon.mp3Url.isNotBlank() ||
                sermon.cdnMp3Url.isNotBlank()

        if (!hasStream) {
            setPlaybackState(
                _playbackState.value.copy(
                    errorMessage = context?.getString(R.string.no_audio_stream_available) ?: "No audio or video stream available"
                )
            )
            return
        }

        activeSermon = sermon
        val controller = mediaController ?: run {
            pendingPlay = sermon to startPositionMs
            connectToService()
            return
        }

        val mp4File = context?.filesDir?.let { java.io.File(it, "audio_downloads/${sermon.id}.mp4") }
        val mp3File = context?.filesDir?.let { java.io.File(it, "audio_downloads/${sermon.id}.mp3") }
        val localFile = when {
            mp4File != null && mp4File.exists() && mp4File.length() > 0 -> mp4File
            mp3File != null && mp3File.exists() && mp3File.length() > 0 -> mp3File
            else -> null
        }
        val isLocal = localFile != null
        val isLocalVideo = localFile?.name?.endsWith(".mp4", ignoreCase = true) == true
        val hasVideo = sermon.hasVideo || isLocalVideo
        val localUri = localFile?.let { "file://${it.absolutePath}" }

        val playUrl = if (isLocal) {
            localUri!!
        } else if (hasVideo) {
            sermon.mp4Url.ifBlank { sermon.cdnMp4Url.ifBlank { sermon.mp3Url.ifBlank { sermon.cdnMp3Url } } }
        } else {
            sermon.mp3Url.ifBlank { sermon.cdnMp3Url }
        }
        val cdnUrl = if (hasVideo) {
            sermon.cdnMp4Url.ifBlank { sermon.cdnMp3Url }
        } else {
            sermon.cdnMp3Url
        }

        if (playUrl.isBlank()) {
            setPlaybackState(
                _playbackState.value.copy(
                    errorMessage = context?.getString(R.string.no_audio_stream_available) ?: "No audio stream available"
                )
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
            isLocal = isLocal,
            hasVideo = hasVideo,
            videoUrl = sermon.videoUrl,
            mp4Url = if (isLocalVideo) localUri!! else sermon.mp4Url,
            cdnMp4Url = if (isLocalVideo) localUri!! else sermon.cdnMp4Url
        )

        controller.setMediaItem(mediaItem)
        if (startPositionMs > 0) {
            controller.seekTo(startPositionMs)
        }
        controller.prepare()
        controller.play()

        setPlaybackState(
            _playbackState.value.copy(
                currentSermon = sermon,
                isPlaying = true,
                isBuffering = true,
                errorMessage = null,
                currentPositionMs = startPositionMs,
                isLocalAudio = isLocal,
                isVideoActive = hasVideo
            )
        )

        startPositionPolling()
    }

    fun setVideoActive(active: Boolean) {
        setPlaybackState(_playbackState.value.copy(isVideoActive = active))
    }

    private var sleepTimerJob: Job? = null

    open fun setSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            cancelSleepTimer()
            return
        }

        val totalMs = minutes * 60 * 1000L
        var remainingMs = totalMs
        setPlaybackState(
            _playbackState.value.copy(
                sleepTimerState = com.example.sermonindex.domain.model.SleepTimerState.Active(remainingMs, totalMs)
            )
        )

        sleepTimerJob = scope.launch {
            while (isActive && remainingMs > 0) {
                delay(1000L)
                remainingMs = (remainingMs - 1000L).coerceAtLeast(0L)
                setPlaybackState(
                    _playbackState.value.copy(
                        sleepTimerState = com.example.sermonindex.domain.model.SleepTimerState.Active(remainingMs, totalMs)
                    )
                )
            }
            if (isActive) {
                pause()
                setPlaybackState(
                    _playbackState.value.copy(
                        sleepTimerState = com.example.sermonindex.domain.model.SleepTimerState.Off
                    )
                )
            }
        }
    }

    open fun setSleepTimerEndOfSermon() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        setPlaybackState(
            _playbackState.value.copy(
                sleepTimerState = com.example.sermonindex.domain.model.SleepTimerState.EndOfSermon
            )
        )
    }

    open fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        setPlaybackState(
            _playbackState.value.copy(
                sleepTimerState = com.example.sermonindex.domain.model.SleepTimerState.Off
            )
        )
    }

    open fun resume() {
        mediaController?.play()
    }

    open fun pause() {
        mediaController?.pause()
    }

    open fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    open fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        setPlaybackState(_playbackState.value.copy(currentPositionMs = positionMs))
    }

    open fun rewind(seconds: Int = 15) {
        val controller = mediaController ?: return
        val newPos = (controller.currentPosition - seconds * 1000L).coerceAtLeast(0L)
        controller.seekTo(newPos)
        setPlaybackState(_playbackState.value.copy(currentPositionMs = newPos))
    }

    open fun fastForward(seconds: Int = 30) {
        val controller = mediaController ?: return
        val duration = controller.duration.coerceAtLeast(0L)
        val newPos = if (duration > 0) {
            (controller.currentPosition + seconds * 1000L).coerceAtMost(duration)
        } else {
            controller.currentPosition + seconds * 1000L
        }
        controller.seekTo(newPos)
        setPlaybackState(_playbackState.value.copy(currentPositionMs = newPos))
    }

    open fun setSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
        setPlaybackState(_playbackState.value.copy(playbackSpeed = speed))
    }

    fun stop() {
        mediaController?.stop()
        setPlaybackState(PlaybackState())
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

        setPlaybackState(
            _playbackState.value.copy(
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
