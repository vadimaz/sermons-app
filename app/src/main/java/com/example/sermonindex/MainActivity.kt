package com.example.sermonindex

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.sermonindex.data.sync.CatalogUpdateCheckWorker
import com.example.sermonindex.domain.model.AppThemeMode
import com.example.sermonindex.domain.repository.NetworkMonitor
import com.example.sermonindex.domain.repository.UserPreferencesRepository
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
  lateinit var networkMonitor: NetworkMonitor

  @Inject
  lateinit var userPreferencesRepository: UserPreferencesRepository

  private var isPipMode by mutableStateOf(false)
  private var externalRoute by mutableStateOf<NavKey?>(null)

  override fun onPictureInPictureModeChanged(
    isInPictureInPictureMode: Boolean,
    newConfig: Configuration
  ) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    isPipMode = isInPictureInPictureMode
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleIntent(intent)

    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isNavigationBarContrastEnforced = false
    }
    setContent {
      val themeMode by userPreferencesRepository.themeMode.collectAsStateWithLifecycle(initialValue = AppThemeMode.SYSTEM)
      val systemInDark = isSystemInDarkTheme()
      val isDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> systemInDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
      }

      DisposableEffect(isDarkTheme) {
        enableEdgeToEdge(
          statusBarStyle = if (isDarkTheme) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
          } else {
            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
          },
          navigationBarStyle = if (isDarkTheme) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
          } else {
            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
          }
        )
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isDarkTheme
        insetsController.isAppearanceLightNavigationBars = !isDarkTheme
        onDispose {}
      }

      CompositionLocalProvider(LocalPictureInPictureMode provides isPipMode) {
        SermonIndexTheme(darkTheme = isDarkTheme) {
          Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val isFromSearchNotification = intent.getBooleanExtra(CatalogUpdateCheckWorker.EXTRA_NAVIGATE_TO_SEARCH, false)
            val isFromSettingsNotification = intent.getBooleanExtra(CatalogUpdateCheckWorker.EXTRA_NAVIGATE_TO_SETTINGS, false)
            val startRoute = when {
              isFromSettingsNotification -> SettingsRoute
              isFromSearchNotification -> SearchRoute
              else -> HomeRoute
            }
            MainNavigation(
              playbackManager = playbackManager,
              networkMonitor = networkMonitor,
              initialRoute = if (startRoute is HomeRoute || startRoute is SearchRoute) startRoute else HomeRoute,
              navigateToRoute = if (startRoute !is HomeRoute && startRoute !is SearchRoute) startRoute else externalRoute
            )
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    if (intent?.getBooleanExtra(CatalogUpdateCheckWorker.EXTRA_NAVIGATE_TO_SEARCH, false) == true) {
      externalRoute = SearchRoute
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
