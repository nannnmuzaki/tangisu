package com.tangisuteam.tangisu.data.repository

import com.tangisuteam.tangisu.data.local.AlarmDao
import com.tangisuteam.tangisu.data.model.Alarm
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AlarmRepositoryImpl @Inject constructor(
    private val alarmDao: AlarmDao
) : AlarmRepository {

    override fun getAllAlarms(): Flow<List<Alarm>> {
        return alarmDao.getAllAlarms()
    }

    override suspend fun getAlarmById(alarmId: String): Alarm? {
        return alarmDao.getAlarmById(alarmId)
    }

    override suspend fun insertAlarm(alarm: Alarm) {
        alarmDao.insertAlarm(alarm)
    }

    override suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.updateAlarm(alarm)
    }

    override suspend fun deleteAlarmById(alarmId: String) {
        alarmDao.deleteAlarmById(alarmId)
    }
}
