package com.tangisuteam.tangisu.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangisuteam.tangisu.alarm.AlarmScheduler
import com.tangisuteam.tangisu.data.model.Alarm
import com.tangisuteam.tangisu.data.repository.AlarmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = alarmRepository.getAllAlarms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun onAlarmEnabledChanged(alarm: Alarm, isEnabled: Boolean) {
        viewModelScope.launch {
            val updatedAlarm = alarm.copy(isEnabled = isEnabled)
            alarmRepository.updateAlarm(updatedAlarm)

            // Schedule or cancel the alarm with Android's AlarmManager
            if (updatedAlarm.isEnabled) {
                alarmScheduler.schedule(updatedAlarm)
            } else {
                alarmScheduler.cancel(updatedAlarm)
            }
        }
    }

    fun deleteAlarm(alarmId: String) {
        viewModelScope.launch {
            val alarmToCancel = alarmRepository.getAlarmById(alarmId)
            if (alarmToCancel != null) {
                alarmScheduler.cancel(alarmToCancel)
            }
            alarmRepository.deleteAlarmById(alarmId)
        }
    }
}
