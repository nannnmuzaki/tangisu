package com.tangisuteam.tangisu.ui.alarm

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tangisuteam.tangisu.data.model.ChallengeType
import com.tangisuteam.tangisu.data.model.DayOfWeek
import com.tangisuteam.tangisu.data.model.TimeFormatSetting
import com.tangisuteam.tangisu.ui.components.DisplayTimePickerDialog
import com.tangisuteam.tangisu.ui.theme.TangisuTheme
import kotlinx.coroutines.flow.collectLatest
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmScreen(
    alarmId: String?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditAlarmViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = Unit) {
        viewModel.loadAlarm(alarmId)
    }
    LaunchedEffect(key1 = viewModel.saveEvent) {
        viewModel.saveEvent.collectLatest {
            onNavigateBack()
        }
    }
    LaunchedEffect(key1 = viewModel.loadErrorEvent) {
        viewModel.loadErrorEvent.collectLatest { errorMessage ->
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
        }
    }

    // --- Time Picker State ---
    val isSystem24Hour = DateFormat.is24HourFormat(context)
    val timePickerIs24Hour = remember(viewModel.timeFormatPreference, isSystem24Hour) {
        // Determine if the TimePicker should be in 24hr mode based on ViewModel's preference
        when (viewModel.timeFormatPreference) {
            TimeFormatSetting.H24 -> true
            TimeFormatSetting.H12 -> false
            TimeFormatSetting.SYSTEM_DEFAULT -> isSystem24Hour
        }
    }

    val timePickerState = remember(timePickerIs24Hour) {
        TimePickerState( // Use the constructor directly for more control with keys
            initialHour = viewModel.hour,
            initialMinute = viewModel.minute,
            is24Hour = timePickerIs24Hour
        )
    }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    // Effect to update TimePickerState if ViewModel's hour/minute change (e.g., after loading an alarm)
    // or if the is24Hour configuration for the picker needs to change.
    LaunchedEffect(viewModel.hour, viewModel.minute, timePickerIs24Hour) {
        timePickerState.hour = viewModel.hour
        timePickerState.minute = viewModel.minute
    }


    TangisuTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (viewModel.isEditMode) "Edit Alarm" else "Add New Alarm",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.saveAlarm() },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Icon(Icons.Filled.Check, "Save alarm")
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 1. Time Display Button (Shows Time Picker Dialog)
                FilledTonalButton(
                    onClick = { showTimePickerDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        // Use the helper from ViewModel for accurate display based on preference
                        text = viewModel.getFormattedDisplayTime(isSystem24Hour),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                Spacer(Modifier.height(2.dp))

                // 2. Label
                OutlinedTextField(
                    value = viewModel.label,
                    onValueChange = { viewModel.onLabelChange(it) },
                    label = { Text("Alarm Label (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                // 3. Days of Week Selector (Using FilterChip)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip( // FilterChip is stable
                            selected = viewModel.daysOfWeek.contains(day),
                            onClick = { viewModel.onDayOfWeekToggle(day, !viewModel.daysOfWeek.contains(day)) },
                            label = { Text(day.name.take(3).uppercase()) } // e.g., MON
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text("Alarm Status", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        onClick = { viewModel.onIsEnabledChange(false) },
                        selected = !viewModel.isEnabled
                    ) {
                        Text("Off")
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        onClick = { viewModel.onIsEnabledChange(true) },
                        selected = viewModel.isEnabled
                    ) {
                        Text("On")
                    }
                }

                // --- Vibration ---
                Text("Vibration", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        onClick = { viewModel.onShouldVibrateChange(false) },
                        selected = !viewModel.shouldVibrate
                    ) {
                        Text("Off")
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        onClick = { viewModel.onShouldVibrateChange(true) },
                        selected = viewModel.shouldVibrate
                    ) {
                        Text("On")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // --- Challenge Type ---
                Text("Dismiss Challenge", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ChallengeType.entries.forEachIndexed { index, challenge ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ChallengeType.entries.size),
                            onClick = { viewModel.onChallengeTypeChange(challenge) },
                            selected = viewModel.challengeType == challenge,
                            icon = { /* SegmentedButton requires an icon lambda, even if empty */ }
                        ) {
                            Text(challenge.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
                        }
                    }
                }

                // --- Time Format Preference ---
                Text("Time Display Format", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TimeFormatSetting.entries.forEachIndexed { index, formatSetting ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = TimeFormatSetting.entries.size),
                            onClick = { viewModel.onTimeFormatPreferenceChange(formatSetting) },
                            selected = viewModel.timeFormatPreference == formatSetting,
                            icon = { /* SegmentedButton requires an icon lambda, even if empty */ }
                        ) {
                            Text(
                                when(formatSetting) {
                                    TimeFormatSetting.SYSTEM_DEFAULT -> "System"
                                    TimeFormatSetting.H12 -> "12 Hour"
                                    TimeFormatSetting.H24 -> "24 Hour"
                                }
                            )
                        }
                    }
                }

                // 6. Ringtone (Placeholder)
                Text("Ringtone: ${viewModel.ringtoneUri ?: "Default"}", style = MaterialTheme.typography.bodyLarge)
                // TODO: Button to open ringtone picker

                // 7. Snooze Duration (Placeholder)
                Text("Snooze: ${viewModel.snoozeDurationMinutes} min", style = MaterialTheme.typography.bodyLarge)
                // TODO: Slider or TextField for snooze

                Spacer(Modifier.height(64.dp))
            }
        }

        // --- Time Picker Dialog ---
        if (showTimePickerDialog) {
            DisplayTimePickerDialog(
                timePickerState = timePickerState,
                onDismissRequest = { showTimePickerDialog = false },
                onConfirm = {
                    viewModel.onTimeChange(timePickerState.hour, timePickerState.minute)
                    showTimePickerDialog = false
                }
            )
        }
    }
}
