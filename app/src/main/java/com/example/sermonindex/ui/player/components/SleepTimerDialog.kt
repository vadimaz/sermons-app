package com.example.sermonindex.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sermonindex.R
import com.example.sermonindex.domain.model.SleepTimerState
import com.example.sermonindex.theme.SermonIndexTheme
import com.example.sermonindex.theme.ThemePreviews

@Composable
fun SleepTimerDialog(
    sleepTimerState: SleepTimerState,
    onDismissRequest: () -> Unit,
    onSetMinutes: (Int) -> Unit,
    onSetEndOfSermon: () -> Unit,
    onCancelTimer: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
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
                        0 -> sleepTimerState is SleepTimerState.Off
                        -1 -> sleepTimerState is SleepTimerState.EndOfSermon
                        else -> sleepTimerState is SleepTimerState.Active &&
                                (sleepTimerState.totalMillis / 60000L).toInt() == minutes
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                when (minutes) {
                                    0 -> onCancelTimer()
                                    -1 -> onSetEndOfSermon()
                                    else -> onSetMinutes(minutes)
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                when (minutes) {
                                    0 -> onCancelTimer()
                                    -1 -> onSetEndOfSermon()
                                    else -> onSetMinutes(minutes)
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
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@ThemePreviews
@Composable
private fun SleepTimerDialogPreview() {
    SermonIndexTheme {
        Surface {
            SleepTimerDialog(
                sleepTimerState = SleepTimerState.Off,
                onDismissRequest = {},
                onSetMinutes = {},
                onSetEndOfSermon = {},
                onCancelTimer = {}
            )
        }
    }
}
