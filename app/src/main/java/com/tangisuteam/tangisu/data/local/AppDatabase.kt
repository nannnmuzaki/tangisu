package com.tangisuteam.tangisu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tangisuteam.tangisu.data.model.Alarm

/**
 * The main database class for the application.
 *
 * @property entities The list of entity classes that are part of this database.
 * @property version The version of the database. This must be incremented whenever the schema changes.
 */
@Database(
    entities = [Alarm::class],
    version = 1 // Start with version 1. Increment this on schema changes.
)
@TypeConverters(Converters::class) // Tell Room to use our custom converters
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides an abstract method to get an instance of the AlarmDao.
     * Room will generate the implementation for this.
     */
    abstract fun alarmDao(): AlarmDao

}
