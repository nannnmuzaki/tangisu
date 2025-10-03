package com.tangisuteam.tangisu.ui.alarm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangisuteam.tangisu.alarm.AlarmScheduler
import com.tangisuteam.tangisu.data.model.Alarm
import com.tangisuteam.tangisu.data.repository.AlarmRepository
import com.tangisuteam.tangisu.data.repository.DummyAlarmRepositoryProvider // Import the provider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider

// We need the application context for the scheduler, so we change it to AndroidViewModel
class AlarmListViewModel(
    application: Application, // Add application context
    private val alarmRepository: AlarmRepository = DummyAlarmRepositoryProvider.instance
) : AndroidViewModel(application) {

    // Initialize the scheduler
    private val alarmScheduler = AlarmScheduler(application)

    val alarms: StateFlow<List<Alarm>> = alarmRepository.getAllAlarms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun onAlarmEnabledChanged(alarm: Alarm, isEnabled: Boolean) {
        viewModelScope.launch {
            val updatedAlarm = alarm.copy(isEnabled = isEnabled)
            // 1. Update the alarm in our database
            alarmRepository.updateAlarm(updatedAlarm)

            // 2. Schedule or cancel the alarm with Android's AlarmManager
            if (updatedAlarm.isEnabled) {
                alarmScheduler.schedule(updatedAlarm)
            } else {
                alarmScheduler.cancel(updatedAlarm)
            }
        }
    }

    fun deleteAlarm(alarmId: String) {
        viewModelScope.launch {
            // First, find the alarm to cancel it from AlarmManager
            val alarmToCancel = alarmRepository.getAlarmById(alarmId)
            if (alarmToCancel != null) {
                alarmScheduler.cancel(alarmToCancel)
            }
            // Then, delete it from our database
            alarmRepository.deleteAlarmById(alarmId)
        }
    }
}

class AlarmListViewModelFactory(
    private val application: Application,
    private val alarmRepository: AlarmRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlarmListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlarmListViewModel(application, alarmRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
