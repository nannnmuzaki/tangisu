package com.tangisuteam.tangisu.data.repository

import com.tangisuteam.tangisu.data.model.Alarm
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    fun getAllAlarms(): Flow<List<Alarm>> // To observe changes
    suspend fun getAlarmById(alarmId: String): Alarm?
    suspend fun insertAlarm(alarm: Alarm)
    suspend fun updateAlarm(alarm: Alarm)
    suspend fun deleteAlarmById(alarmId: String)
}