package com.tangisuteam.tangisu.data.repository

import com.tangisuteam.tangisu.data.model.Alarm
import com.tangisuteam.tangisu.data.model.ChallengeType
import com.tangisuteam.tangisu.data.model.DayOfWeek
import com.tangisuteam.tangisu.data.model.TimeFormatSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// This will be replaced by a Hilt-provided singleton later
object DummyAlarmRepositoryProvider {
    val instance: AlarmRepository by lazy { DummyAlarmRepositoryImpl() }
}


// In a real app with Hilt, this would be @Singleton and @Inject constructor
class DummyAlarmRepositoryImpl : AlarmRepository {

    private val _alarms = MutableStateFlow<List<Alarm>>(
        // Initial dummy data
        listOf(
            Alarm(
                hour = 7, minute = 0,
                label = "Morning Workout",
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                challengeType = ChallengeType.MATH
            ),
            Alarm(
                hour = 22, minute = 30,
                label = "Bedtime Reading",
                isEnabled = false,
                daysOfWeek = emptySet(),
                timeFormatPreference = TimeFormatSetting.H12
            ),
            Alarm(
                hour = 9, minute = 15,
                label = null,
                daysOfWeek = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                shouldVibrate = false
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
