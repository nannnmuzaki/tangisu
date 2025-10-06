package com.tangisuteam.tangisu.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tangisuteam.tangisu.data.model.Alarm
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    /**
     * Gets a real-time stream of all alarms from the database, ordered by time.
     * The Flow will automatically emit a new list whenever the data changes.
     */
    @Query("SELECT * FROM alarms ORDER BY hour, minute ASC")
    fun getAllAlarms(): Flow<List<Alarm>>

    /**
     * Fetches a single alarm by its unique ID. This is a one-time operation.
     */
    @Query("SELECT * FROM alarms WHERE id = :alarmId")
    suspend fun getAlarmById(alarmId: String): Alarm?

    /**
     * Inserts a new alarm into the database. If an alarm with the same ID already
     * exists, it will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm)

    /**
     * Updates an existing alarm in the database.
     */
    @Update
    suspend fun updateAlarm(alarm: Alarm)

    /**
     * Deletes an alarm from the database using its ID.
     */
    @Query("DELETE FROM alarms WHERE id = :alarmId")
    suspend fun deleteAlarmById(alarmId: String)
}
