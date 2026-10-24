package com.example.sermonindex

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.example.sermonindex.playback.PlaybackManager
import com.example.sermonindex.theme.SermonIndexTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

val LocalPictureInPictureMode = staticCompositionLocalOf { false }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject
  lateinit var playbackManager: PlaybackManager

  @Inject
  lateinit var networkMonitor: com.example.sermonindex.domain.repository.NetworkMonitor

  private var isPipMode by mutableStateOf(false)

  override fun onPictureInPictureModeChanged(
    isInPictureInPictureMode: Boolean,
    newConfig: Configuration
  ) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    isPipMode = isInPictureInPictureMode
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
    )
    @Suppress("DEPRECATION")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isStatusBarContrastEnforced = false
      window.isNavigationBarContrastEnforced = false
    }
    setContent {
      CompositionLocalProvider(LocalPictureInPictureMode provides isPipMode) {
        SermonIndexTheme {
          Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            MainNavigation(playbackManager = playbackManager, networkMonitor = networkMonitor)
          }
        }
      }
    }
  }

  override fun onStop() {
    super.onStop()
    // When the user explicitly closes/dismisses the PiP window, stop playback immediately
    if (isPipMode) {
      playbackManager.stop()
    }
  }

  override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val state = playbackManager.playbackState.value
      if (state.hasVideo && state.isPlaying) {
        runCatching {
          val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true)
          }
          enterPictureInPictureMode(builder.build())
        }
      }
    }
  }
}
