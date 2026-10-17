package com.example.sermonindex.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SermonDetail
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Stateful entry point for FullscreenPlayerScreen.
 * Collects state from [PlayerViewModel] and passes down to stateless [PlayerContent].
 */
@Composable
fun FullscreenPlayerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: PlayerViewModel = hiltViewModel()

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Auto-exit if no sermon is active after waiting for initial service connection
    LaunchedEffect(state.sermon) {
        if (state.sermon == null) {
            delay(500L)
            if (viewModel.uiState.value.sermon == null) {
                onBack()
            }
        }
    }

    PlayerContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

/**
 * Stateless PlayerContent receiving state and emitting events.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerContent(
    state: PlayerUiState,
    onEvent: (PlayerUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sermon = state.sermon

    if (sermon == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top App Bar with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close_player))
                }
                Text(
                    text = stringResource(R.string.player),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Center Empty State Content with Action Button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.no_sermon_playing_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.no_sermon_playing_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.browse_sermons))
                    }
                }
            }
        }
        return
    }

    val currentMs = state.displayPositionMs
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val currentOnEvent by rememberUpdatedState(onEvent)
    val isReadingMode = state.isReadingMode

    // NestedScrollConnection detects any manual user scrolling (touch drag, fling) instantly
    val nestedScrollConnection = remember(isReadingMode) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isReadingMode && source == NestedScrollSource.UserInput) {
                    currentOnEvent(PlayerUiEvent.SetReadingMode(false))
                }
                return Offset.Zero
            }
        }
    }

    // Also monitor InteractionSource to detect drag gestures on any child elements
    LaunchedEffect(state.isReadingMode) {
        if (state.isReadingMode) {
            listState.interactionSource.interactions.collect { interaction ->
                if (interaction is DragInteraction.Start) {
                    currentOnEvent(PlayerUiEvent.SetReadingMode(false))
                }
            }
        }
    }

    // Smooth auto-scroll when Reading Mode is toggled on (anchors active cue with 3 preceding lines)
    LaunchedEffect(state.isReadingMode) {
        if (state.isReadingMode) {
            val target = if (state.activeCueIndex >= 3) state.activeCueIndex - 1 else 1
            listState.animateScrollToItem(target)
        }
    }

    // Smooth auto-scroll while playing in Reading Mode (anchors active cue with 3 preceding lines)
    LaunchedEffect(state.activeCueIndex) {
        if (state.isReadingMode && state.activeCueIndex >= 0) {
            val target = if (state.activeCueIndex >= 3) state.activeCueIndex - 1 else 1
            listState.animateScrollToItem(target)
        }
    }

    if (state.showSleepTimerDialog) {
        val currentTimer = state.sleepTimerState
        AlertDialog(
            onDismissRequest = { onEvent(PlayerUiEvent.SetSleepTimerDialogVisible(false)) },
            title = { Text(stringResource(R.string.sleep_timer)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val options = listOf(
                        0 to stringResource(R.string.sleep_timer_off),
                        15 to stringResource(R.string.sleep_timer_15m),
                        30 to stringResource(R.string.sleep_timer_30m),
                        45 to stringResource(R.string.sleep_timer_45m),
                        60 to stringResource(R.string.sleep_timer_60m),
                        -1 to stringResource(R.string.sleep_timer_end_of_sermon)
                    )

                    options.forEach { (minutes, label) ->
                        val isSelected = when (minutes) {
                            0 -> currentTimer is com.example.sermonindex.domain.model.SleepTimerState.Off
                            -1 -> currentTimer is com.example.sermonindex.domain.model.SleepTimerState.EndOfSermon
                            else -> currentTimer is com.example.sermonindex.domain.model.SleepTimerState.Active &&
                                    (currentTimer.totalMillis / 60000L).toInt() == minutes
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (minutes) {
                                        0 -> onEvent(PlayerUiEvent.CancelSleepTimer)
                                        -1 -> onEvent(PlayerUiEvent.SetSleepTimerEndOfSermon)
                                        else -> onEvent(PlayerUiEvent.SetSleepTimerMinutes(minutes))
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    when (minutes) {
                                        0 -> onEvent(PlayerUiEvent.CancelSleepTimer)
                                        -1 -> onEvent(PlayerUiEvent.SetSleepTimerEndOfSermon)
                                        else -> onEvent(PlayerUiEvent.SetSleepTimerMinutes(minutes))
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { onEvent(PlayerUiEvent.SetSleepTimerDialogVisible(false)) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close_player))
            }

            Text(
                text = stringResource(R.string.now_playing),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sleep Timer button
                IconButton(onClick = { onEvent(PlayerUiEvent.SetSleepTimerDialogVisible(true)) }) {
                    val isTimerActive = state.sleepTimerState !is com.example.sermonindex.domain.model.SleepTimerState.Off
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = stringResource(R.string.sleep_timer),
                        tint = if (isTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Download button
                IconButton(onClick = { onEvent(PlayerUiEvent.ToggleDownload) }) {
                    if (state.downloadProgress != null) {
                        CircularProgressIndicator(
                            progress = { (state.downloadProgress.toFloat() / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (state.isDownloaded) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = stringResource(R.string.downloaded),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = stringResource(R.string.download),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Favorite button
                IconButton(onClick = { onEvent(PlayerUiEvent.ToggleFavorite) }) {
                    Icon(
                        imageVector = if (state.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.favorite),
                        tint = if (state.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Scrollable content with sticky controls row
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Artwork, Title, Badges, Slider, Time
            item(key = "player_header") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Speaker Artwork
                    val imageUrl = sermon.speakerImage.ifBlank { sermon.thumbnailUrl }
                    Surface(
                        modifier = Modifier
                            .size(240.dp)
                            .shadow(16.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = sermon.speaker,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Sermon Title & Speaker
                    Text(
                        text = sermon.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sermon.speaker,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Status badges (Offline Audio, Sleep Timer, CDN Failover)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        if (state.isLocalAudio) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.OfflinePin,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.offline_audio),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        val timerState = state.sleepTimerState
                        if (timerState !is com.example.sermonindex.domain.model.SleepTimerState.Off) {
                            val timerText = when (timerState) {
                                is com.example.sermonindex.domain.model.SleepTimerState.Active -> {
                                    val remainingMinutes = (timerState.remainingMillis / 60000L).coerceAtLeast(1L)
                                    stringResource(R.string.sleep_timer_active, "$remainingMinutes min")
                                }
                                is com.example.sermonindex.domain.model.SleepTimerState.EndOfSermon -> stringResource(R.string.sleep_timer_end_of_sermon)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.clickable { onEvent(PlayerUiEvent.SetSleepTimerDialogVisible(true)) }
                            ) {
                                Text(
                                    text = timerText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (state.isUsingFailoverCdn) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = stringResource(R.string.cdn_failover_active),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Playback Slider
                    Slider(
                        value = if (state.durationMs > 0) {
                            (currentMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f,
                        onValueChange = {
                            onEvent(PlayerUiEvent.SliderChange(it))
                        },
                        onValueChangeFinished = {
                            onEvent(PlayerUiEvent.SliderChangeFinished)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Duration labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(currentMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDuration(state.durationMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Sticky Playback Controls (Speed, Rewind 15s, Play/Pause, Forward 30s, MenuBook)
            stickyHeader(key = "player_controls") {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Speed Selector (Icon only; accent color when not 1.0x)
                        Box {
                            IconButton(onClick = { onEvent(PlayerUiEvent.SetSpeedMenuVisible(true)) }) {
                                val isCustomSpeed = state.playbackSpeed != 1.0f
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = stringResource(R.string.playback_speed),
                                    tint = if (isCustomSpeed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = state.showSpeedMenu,
                                onDismissRequest = { onEvent(PlayerUiEvent.SetSpeedMenuVisible(false)) }
                            ) {
                                listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                    DropdownMenuItem(
                                        text = { Text("${speed}x") },
                                        onClick = {
                                            onEvent(PlayerUiEvent.SetSpeed(speed))
                                        }
                                    )
                                }
                            }
                        }

                        // 2. Rewind 15s
                        IconButton(onClick = { onEvent(PlayerUiEvent.Rewind(15)) }) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = stringResource(R.string.rewind_15_seconds),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // 3. Play / Pause
                        FilledIconButton(
                            onClick = { onEvent(PlayerUiEvent.TogglePlayPause) },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // 4. Forward 30s
                        IconButton(onClick = { onEvent(PlayerUiEvent.FastForward(30)) }) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = stringResource(R.string.fast_forward_30_seconds),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // 5. Reading Mode (MenuBook; disabled if no VTT, accent color when active)
                        val hasVtt = state.transcriptCues.isNotEmpty()
                        IconButton(
                            onClick = {
                                if (state.isReadingMode) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(0)
                                    }
                                    onEvent(PlayerUiEvent.SetReadingMode(false))
                                } else {
                                    onEvent(PlayerUiEvent.SetReadingMode(true))
                                }
                            },
                            enabled = hasVtt
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = stringResource(R.string.reading_mode),
                                tint = when {
                                    !hasVtt -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    state.isReadingMode -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Transcript Section: WebVTT Cues (Karaoke Style) or Static Text Fallback
            if (state.transcriptCues.isNotEmpty()) {
                item(key = "transcript_header") {
                    Text(
                        text = stringResource(R.string.tab_transcript),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                itemsIndexed(
                    items = state.transcriptCues,
                    key = { _, cue -> "cue_${cue.index}" }
                ) { index, cue ->
                    val isActive = index == state.activeCueIndex
                    val targetColor = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    }
                    val animatedColor by animateColorAsState(targetValue = targetColor, label = "cueTextColor")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onEvent(PlayerUiEvent.SeekToCue(cue.startMs)) }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = cue.text,
                            fontSize = 22.sp,
                            lineHeight = 30.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = animatedColor,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            } else if (state.isLoadingCues) {
                item(key = "loading_cues") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                    }
                }
            } else {
                // Fallback to static transcript with standard typography
                val detail = state.sermonDetail
                item(key = "static_transcript") {
                    TranscriptSection(
                        transcriptHtml = detail?.transcriptHtml ?: "",
                        transcriptText = detail?.transcript ?: ""
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.navigationBarsPadding().height(48.dp))
            }
        }
    }
}

@Composable
private fun TranscriptSection(transcriptHtml: String, transcriptText: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = stringResource(R.string.tab_transcript),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(10.dp))

        val cleanText = remember(transcriptHtml, transcriptText) {
            if (transcriptText.isNotBlank()) transcriptText
            else transcriptHtml.replace(Regex("<[^>]*>"), "")
        }

        if (cleanText.isBlank()) {
            Text(
                text = stringResource(R.string.no_transcript_available),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = cleanText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
