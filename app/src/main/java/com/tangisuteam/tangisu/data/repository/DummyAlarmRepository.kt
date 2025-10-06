package com.tangisuteam.tangisu.data.repository

import com.tangisuteam.tangisu.data.model.Alarm
import com.tangisuteam.tangisu.data.model.ChallengeType
import com.tangisuteam.tangisu.data.model.DayOfWeek
import com.tangisuteam.tangisu.data.model.TimeFormatSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class DummyAlarmRepositoryImpl @Inject constructor() : AlarmRepository {

    private val _alarms = MutableStateFlow<List<Alarm>>(
        // Initial dummy data
        listOf(
            Alarm(
                hour = 4, minute = 0,
                label = "Nguli-ah",
                isEnabled = false,
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                challengeType = ChallengeType.MATH
            ),
            Alarm(
                hour = 13, minute = 55,
                label = "Kelas King",
                isEnabled = false,
                daysOfWeek = emptySet(),
                timeFormatPreference = TimeFormatSetting.H12
            )
        )
    )

    override fun getAllAlarms(): Flow<List<Alarm>> = _alarms.asStateFlow() // Expose as StateFlow

    override suspend fun getAlarmById(alarmId: String): Alarm? {
        return _alarms.value.find { it.id == alarmId }
    }

    override suspend fun insertAlarm(alarm: Alarm) {
        _alarms.update { currentList ->
            currentList + alarm // Add to the list
        }
        println("DummyRepository: Inserted ${alarm.label}")
    }

    override suspend fun updateAlarm(alarm: Alarm) {
        _alarms.update { currentList ->
            currentList.map { existingAlarm ->
                if (existingAlarm.id == alarm.id) alarm else existingAlarm
            }
        }
        println("DummyRepository: Updated ${alarm.label}")
    }

    override suspend fun deleteAlarmById(alarmId: String) {
        _alarms.update { currentList ->
            currentList.filterNot { it.id == alarmId }
        }
        println("DummyRepository: Deleted alarm with ID $alarmId")
    }
}
