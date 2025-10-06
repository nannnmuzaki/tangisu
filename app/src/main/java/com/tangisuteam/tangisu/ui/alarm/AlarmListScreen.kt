package com.tangisuteam.tangisu.ui.alarm

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.tangisuteam.tangisu.data.model.Alarm
import com.tangisuteam.tangisu.data.model.ChallengeType
import com.tangisuteam.tangisu.data.model.DayOfWeek
import com.tangisuteam.tangisu.ui.theme.TangisuTheme
import com.tangisuteam.tangisu.ui.components.PermissionHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    alarmListViewModel: AlarmListViewModel = hiltViewModel(),
    onNavigateToAddAlarm: () -> Unit,
    onNavigateToEditAlarm: (alarmId: String) -> Unit
) {
    // Collect the alarms StateFlow from the ViewModel
    val alarms by alarmListViewModel.alarms.collectAsState()
    val isSystem24Hour = DateFormat.is24HourFormat(LocalContext.current)

    TangisuTheme {
        PermissionHandler {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Tangisu たんぎす") },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = onNavigateToAddAlarm) {
                        Icon(Icons.Filled.Add, contentDescription = "Add new alarm")
                    }
                }
            ) { paddingValues ->
                if (alarms.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues) // Apply padding from Scaffold
                            .padding(16.dp), // Additional padding for content
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No alarms yet. Tap '+' to add one!",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues), // Apply padding from Scaffold
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(alarms, key = { alarm -> alarm.id }) { alarm ->
                            AlarmItem(
                                alarm = alarm,
                                isSystem24HourFormat = isSystem24Hour,
                                onEnabledChange = { isEnabled ->
                                    alarmListViewModel.onAlarmEnabledChanged(alarm, isEnabled)
                                },
                                onDeleteClick = {
                                    alarmListViewModel.deleteAlarm(alarm.id)
                                },
                                onClick = {
                                    onNavigateToEditAlarm(alarm.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmItem(
    alarm: Alarm,
    isSystem24HourFormat: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // Make the whole card clickable for editing
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val timeText = alarm.getFormattedTime(isSystem24HourFormat)
                val challengeDisplay = when (alarm.challengeType) {
                    ChallengeType.NONE -> "" // No challenge, so empty string for the challenge part
                    ChallengeType.MATH -> "Math"
                    // Add other cases here as you define more ChallengeType enums
                    // e.g., ChallengeType.PUZZLE -> "Puzzle"
                    else -> alarm.challengeType.name // Fallback to the enum name if not explicitly handled
                }

                val displayText = if (alarm.challengeType == ChallengeType.NONE) {
                    timeText
                } else {
                    "$timeText | $challengeDisplay"
                }

                Text( // TIME and optional CHALLENGE
                    text = displayText,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp)) // Small space
                Column {
                    if (alarm.label != null && alarm.label!!.isNotBlank()) {
                        Text(
                            text = alarm.label!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        if (alarm.isRepeating()) Spacer(modifier = Modifier.height(4.dp)) // Add space if both label and days are present
                    }
                    Text(
                        text = formatDaysOfWeek(alarm.daysOfWeek),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp)) // Space before switch and delete
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                )
            )
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete alarm ${alarm.label ?: ""}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun formatDaysOfWeek(days: Set<DayOfWeek>): String {
    if (days.isEmpty()) return "One-time" // Or perhaps don't display anything for one-time
    if (days.size == 7) return "Every day"

    val sortedDays = days.toList().sortedBy { it.ordinal } // Sort by enum order (MON, TUE..)

    // Handle common cases
    val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    if (days == weekdays) return "Weekdays"
    if (days == weekend) return "Weekends"

    return sortedDays.joinToString(", ") {
        when (it) {
            DayOfWeek.MONDAY -> "Mon"
            DayOfWeek.TUESDAY -> "Tue"
            DayOfWeek.WEDNESDAY -> "Wed"
            DayOfWeek.THURSDAY -> "Thu"
            DayOfWeek.FRIDAY -> "Fri"
            DayOfWeek.SATURDAY -> "Sat"
            DayOfWeek.SUNDAY -> "Sun"
        }
    }
}
