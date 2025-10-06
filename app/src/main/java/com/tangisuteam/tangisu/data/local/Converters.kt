package com.tangisuteam.tangisu.data.local

import androidx.room.TypeConverter
import com.tangisuteam.tangisu.data.model.ChallengeType
import com.tangisuteam.tangisu.data.model.DayOfWeek
import com.tangisuteam.tangisu.data.model.TimeFormatSetting

class Converters {

    // --- DayOfWeek Set Converters ---
    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>): String {
        // Convert the set of enums to a comma-separated string of their names
        // e.g., {MONDAY, WEDNESDAY} -> "MONDAY,WEDNESDAY"
        return days.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toDayOfWeekSet(daysString: String): Set<DayOfWeek> {
        // If the string is empty, return an empty set
        if (daysString.isBlank()) {
            return emptySet()
        }
        // Split the string by commas and convert each part back to a DayOfWeek enum
        return daysString.split(",").map { DayOfWeek.valueOf(it) }.toSet()
    }

    // --- ChallengeType Enum Converter ---
    @TypeConverter
    fun fromChallengeType(challengeType: ChallengeType): String {
        return challengeType.name
    }

    @TypeConverter
    fun toChallengeType(challengeTypeName: String): ChallengeType {
        return ChallengeType.valueOf(challengeTypeName)
    }

    // --- TimeFormatSetting Enum Converter ---
    @TypeConverter
    fun fromTimeFormatSetting(setting: TimeFormatSetting): String {
        return setting.name
    }

    @TypeConverter
    fun toTimeFormatSetting(settingName: String): TimeFormatSetting {
        return TimeFormatSetting.valueOf(settingName)
    }
}
