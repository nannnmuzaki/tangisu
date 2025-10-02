package com.tangisuteam.tangisu.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangisuteam.tangisu.data.model.Alarm
import com.tangisuteam.tangisu.data.repository.AlarmRepository
import com.tangisuteam.tangisu.data.repository.DummyAlarmRepositoryProvider // Import the provider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Later, this constructor will be @Inject constructor(private val alarmRepository: AlarmRepository) with Hilt
class AlarmViewModel(
    private val alarmRepository: AlarmRepository = DummyAlarmRepositoryProvider.instance // Get instance from provider
) : ViewModel() {

    // Expose alarms as a StateFlow from the repository
    val alarms: StateFlow<List<Alarm>> = alarmRepository.getAllAlarms()
        .stateIn(
            scope = viewModelScope,
            // Keep the upstream flow active for 5 seconds after the last collector disappears.
            // This is useful for screen rotations or temporary configuration changes.
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList() // Initial value while the flow is not yet emitting
        )

    // Example of how to get the system's 24-hour format preference.
    // This is more of a UI concern or global app setting, but shown here for context.
    // In a real app, this might come from a settings repository or be passed to Composables directly.
    // val isSystem24HourFormat: StateFlow<Boolean> = ... (could be from a settings repository)

    fun onAlarmEnabledChanged(alarm: Alarm, isEnabled: Boolean) {
        viewModelScope.launch {
            // Create a copy with the updated isEnabled state
            val updatedAlarm = alarm.copy(isEnabled = isEnabled)
            alarmRepository.updateAlarm(updatedAlarm)
        }
    }

    fun deleteAlarm(alarmId: String) {
        viewModelScope.launch {
            alarmRepository.deleteAlarmById(alarmId)
        }
    }

    // Navigation-related functions like addAlarmClicked() or alarmClicked(alarm: Alarm)
    // are typically handled by the UI calling navigation lambdas/callbacks,
    // rather than being direct functions in the ViewModel that trigger navigation.
    // The ViewModel provides data; the UI reacts and navigates.
}
