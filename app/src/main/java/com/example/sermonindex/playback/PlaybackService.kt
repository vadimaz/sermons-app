package com.example.sermonindex.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.sermonindex.MainActivity
import com.example.sermonindex.domain.repository.SermonRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var sermonRepository: SermonRepository

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentSermonId: String? = null
    private var currentTitle: String = ""
    private var currentSpeaker: String = ""
    private var currentSpeakerImage: String = ""
    private var primaryMp3Url: String = ""
    private var cdnMp3Url: String = ""
    private var hasTriedFailover: Boolean = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(MediaSessionCallback())
            .build()

        setupPlayerListener()
        startPeriodicHistoryTracking()
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer error code: ${error.errorCode}, message: ${error.message}")

                // Step 2.2 Failover Logic: If primary Archive.org URL failed, switch to BunnyCDN
                if (!hasTriedFailover && cdnMp3Url.isNotBlank() && cdnMp3Url != primaryMp3Url) {
                    Log.i(TAG, "Primary audio failed. Initiating failover to secondary CDN URL: $cdnMp3Url")
                    hasTriedFailover = true
                    val currentPosition = player.currentPosition

                    val currentExtras = player.currentMediaItem?.mediaMetadata?.extras
                    val hasVideo = currentExtras?.getBoolean(EXTRA_HAS_VIDEO, false) ?: false
                    val videoUrl = currentExtras?.getString(EXTRA_VIDEO_URL) ?: ""
                    val mp4Url = currentExtras?.getString(EXTRA_MP4_URL) ?: ""
                    val cdnMp4Url = currentExtras?.getString(EXTRA_CDN_MP4_URL) ?: ""

                    val failoverMediaItem = buildMediaItem(
                        url = cdnMp3Url,
                        title = currentTitle,
                        speaker = currentSpeaker,
                        speakerImage = currentSpeakerImage,
                        sermonId = currentSermonId ?: "",
                        isFailover = true,
                        hasVideo = hasVideo,
                        videoUrl = videoUrl,
                        mp4Url = mp4Url,
                        cdnMp4Url = cdnMp4Url
                    )

                    player.setMediaItem(failoverMediaItem)
                    player.seekTo(currentPosition)
                    player.prepare()
                    player.play()
                } else {
                    Log.e(TAG, "Failover already attempted or no secondary CDN URL available. Playback aborted.")
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                mediaItem?.mediaMetadata?.extras?.let { extras ->
                    currentSermonId = extras.getString(EXTRA_SERMON_ID)
                    currentTitle = mediaItem.mediaMetadata.title?.toString() ?: ""
                    currentSpeaker = mediaItem.mediaMetadata.artist?.toString() ?: ""
                    currentSpeakerImage = mediaItem.mediaMetadata.artworkUri?.toString() ?: ""
                    primaryMp3Url = extras.getString(EXTRA_PRIMARY_URL) ?: ""
                    cdnMp3Url = extras.getString(EXTRA_CDN_URL) ?: ""
                    hasTriedFailover = extras.getBoolean(EXTRA_IS_FAILOVER, false)
                }
            }
        })
    }

    private fun startPeriodicHistoryTracking() {
        serviceScope.launch {
            while (isActive) {
                delay(2000L)
                if (player.isPlaying) {
                    val sermonId = currentSermonId
                    if (!sermonId.isNullOrBlank() && !sermonId.startsWith("bible_")) {
                        val pos = player.currentPosition
                        val duration = player.duration.coerceAtLeast(0L)
                        runCatching {
                            sermonRepository.updateHistory(
                                sermonId = sermonId,
                                title = currentTitle,
                                speaker = currentSpeaker,
                                speakerImage = currentSpeakerImage,
                                positionMs = pos,
                                durationMs = duration
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(com.example.sermonindex.R.string.playback_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(com.example.sermonindex.R.string.playback_channel_description)
            setShowBadge(false)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private inner class MediaSessionCallback : MediaSession.Callback

    companion object {
        private const val TAG = "PlaybackService"
        const val CHANNEL_ID = "sermon_playback_channel"

        const val EXTRA_SERMON_ID = "extra_sermon_id"
        const val EXTRA_PRIMARY_URL = "extra_primary_url"
        const val EXTRA_CDN_URL = "extra_cdn_url"
        const val EXTRA_IS_FAILOVER = "extra_is_failover"
        const val EXTRA_IS_LOCAL = "extra_is_local"
        const val EXTRA_HAS_VIDEO = "extra_has_video"
        const val EXTRA_VIDEO_URL = "extra_video_url"
        const val EXTRA_MP4_URL = "extra_mp4_url"
        const val EXTRA_CDN_MP4_URL = "extra_cdn_mp4_url"

        fun buildMediaItem(
            url: String,
            title: String,
            speaker: String,
            speakerImage: String,
            sermonId: String,
            isFailover: Boolean = false,
            primaryUrl: String = url,
            cdnUrl: String = "",
            isLocal: Boolean = false,
            hasVideo: Boolean = false,
            videoUrl: String = "",
            mp4Url: String = "",
            cdnMp4Url: String = ""
        ): MediaItem {
            val extras = Bundle().apply {
                putString(EXTRA_SERMON_ID, sermonId)
                putString(EXTRA_PRIMARY_URL, primaryUrl)
                putString(EXTRA_CDN_URL, cdnUrl)
                putBoolean(EXTRA_IS_FAILOVER, isFailover)
                putBoolean(EXTRA_IS_LOCAL, isLocal)
                putBoolean(EXTRA_HAS_VIDEO, hasVideo)
                putString(EXTRA_VIDEO_URL, videoUrl)
                putString(EXTRA_MP4_URL, mp4Url)
                putString(EXTRA_CDN_MP4_URL, cdnMp4Url)
            }

            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(speaker)
                .setArtworkUri(if (speakerImage.isNotBlank()) speakerImage.toUri() else null)
                .setExtras(extras)
                .build()

            return MediaItem.Builder()
                .setMediaId(sermonId)
                .setUri(url.toUri())
                .setMediaMetadata(metadata)
                .build()
        }
    }
}
