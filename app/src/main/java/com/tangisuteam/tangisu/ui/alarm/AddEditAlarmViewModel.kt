package com.tangisuteam.tangisu.ui.alarm

import android.icu.util.Calendar
import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.tangisuteam.tangisu.data.model.*
import com.tangisuteam.tangisu.data.repository.AlarmRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.UUID
import java.util.Locale
import androidx.lifecycle.ViewModel
import com.tangisuteam.tangisu.alarm.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val DEFAULT_HOUR = 7
private const val DEFAULT_MINUTE = 0
private const val DEFAULT_SNOOZE_MINUTES = 10

@HiltViewModel
class AddEditAlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // --- UI State ---
    var hour by mutableStateOf(DEFAULT_HOUR)
        private set
    var minute by mutableStateOf(DEFAULT_MINUTE)
        private set

    var label by mutableStateOf("")
    var isEnabled by mutableStateOf(true)
    var daysOfWeek by mutableStateOf<Set<DayOfWeek>>(emptySet())

    // Placeholders for features to be fully implemented later
    var ringtoneUri by mutableStateOf<String?>(null) // Or a default URI string
    var shouldVibrate by mutableStateOf(true)
    var snoozeDurationMinutes by mutableStateOf(DEFAULT_SNOOZE_MINUTES)
    var challengeType by mutableStateOf(ChallengeType.NONE)
    var timeFormatPreference by mutableStateOf(TimeFormatSetting.SYSTEM_DEFAULT)

    private var currentAlarmId: String? = null
    val isEditMode: Boolean
        get() = currentAlarmId != null

    // --- Events for UI ---
    // Used to signal the UI that saving is complete and it should navigate back
    private val _saveEvent = MutableSharedFlow<Unit>()
    val saveEvent = _saveEvent.asSharedFlow()

    private val _loadErrorEvent = MutableSharedFlow<String>()
    val loadErrorEvent = _loadErrorEvent.asSharedFlow() // For showing errors if alarm not found

    fun loadAlarm(alarmId: String?) {
        currentAlarmId = alarmId
        if (alarmId != null) {
            viewModelScope.launch {
                val alarm = alarmRepository.getAlarmById(alarmId)
                if (alarm != null) {
                    hour = alarm.hour
                    minute = alarm.minute
                    label = alarm.label ?: ""
                    isEnabled = alarm.isEnabled
                    daysOfWeek = alarm.daysOfWeek
                    ringtoneUri = alarm.ringtoneUri
                    shouldVibrate = alarm.shouldVibrate
                    snoozeDurationMinutes = alarm.snoozeDurationMinutes
                    challengeType = alarm.challengeType
                    timeFormatPreference = alarm.timeFormatPreference
                } else {
                    // Alarm ID provided but not found, could be an error scenario
                    _loadErrorEvent.emit("Alarm not found.")
                    resetToDefaults() // Or handle differently
                }
            }
        } else {
            // New alarm, reset to defaults
            resetToDefaults()
        }
    }

    private fun resetToDefaults() {
        hour = DEFAULT_HOUR
        minute = DEFAULT_MINUTE
        label = ""
        isEnabled = true
        daysOfWeek = emptySet()
        ringtoneUri = null
        shouldVibrate = true
        snoozeDurationMinutes = DEFAULT_SNOOZE_MINUTES
        challengeType = ChallengeType.NONE
        timeFormatPreference = TimeFormatSetting.SYSTEM_DEFAULT
        currentAlarmId = null
    }

    fun saveAlarm() {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            return
        }

        viewModelScope.launch {
            val alarmToSave = Alarm(
                id = currentAlarmId ?: UUID.randomUUID().toString(),
                hour = hour,
                minute = minute,
                label = label.takeIf { it.isNotBlank() }, // Store null if blank, else the label
                isEnabled = isEnabled,
                daysOfWeek = daysOfWeek,
                ringtoneUri = ringtoneUri,
                shouldVibrate = shouldVibrate,
                snoozeDurationMinutes = snoozeDurationMinutes,
                challengeType = challengeType,
                timeFormatPreference = timeFormatPreference
            )

            if (isEditMode) {
                alarmRepository.updateAlarm(alarmToSave)
            } else {
                alarmRepository.insertAlarm(alarmToSave)
            }

            if (alarmToSave.isEnabled) {
                alarmScheduler.schedule(alarmToSave)
            } else {
                alarmScheduler.cancel(alarmToSave)
            }

            _saveEvent.emit(Unit) // Signal successful save
        }
    }

    fun getFormattedDisplayTime(isSystem24HourFormatExternal: Boolean): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val use24HourFormat = when (timeFormatPreference) {
            TimeFormatSetting.H12 -> false
            TimeFormatSetting.H24 -> true
            TimeFormatSetting.SYSTEM_DEFAULT -> isSystem24HourFormatExternal
        }
        val sdfPattern = if (use24HourFormat) "HH:mm" else "hh:mm a"
        val sdf = SimpleDateFormat(sdfPattern, Locale.getDefault())
        return sdf.format(calendar.time)
    }

    // --- UI Event Handlers ---

    fun onTimeChange(newHour: Int, newMinute: Int) {
        hour = newHour
        minute = newMinute
    }

    fun onLabelChange(newLabel: String) {
        label = newLabel
    }

    fun onIsEnabledChange(newIsEnabled: Boolean) {
        isEnabled = newIsEnabled
    }

    fun onDayOfWeekToggle(day: DayOfWeek, isSelected: Boolean) {
        daysOfWeek = if (isSelected) {
            daysOfWeek + day
        } else {
            daysOfWeek - day
        }
    }

    fun onRingtoneUriChange(newUri: String?) {
        ringtoneUri = newUri
    }

    fun onShouldVibrateChange(newShouldVibrate: Boolean) {
        shouldVibrate = newShouldVibrate
    }

    fun onSnoozeDurationChange(newSnooze: Int) {
        snoozeDurationMinutes = newSnooze.coerceIn(1, 60) // Ensure snooze is between 1 and 60 min
    }

    fun onChallengeTypeChange(newChallengeType: ChallengeType) {
        challengeType = newChallengeType
    }

    fun onTimeFormatPreferenceChange(newPreference: TimeFormatSetting) {
        timeFormatPreference = newPreference
    }
}

