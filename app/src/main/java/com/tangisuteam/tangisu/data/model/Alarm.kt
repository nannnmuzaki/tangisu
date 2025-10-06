package com.tangisuteam.tangisu.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar

enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

enum class ChallengeType {
    NONE,
    MATH
}

enum class TimeFormatSetting {
    SYSTEM_DEFAULT,
    H12,
    H24
}

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    var isEnabled: Boolean = true,
    var label: String? = null,
    var daysOfWeek: Set<DayOfWeek> = emptySet(),
    var challengeType: ChallengeType = ChallengeType.NONE,
    var ringtoneUri: String? = null,
    var shouldVibrate: Boolean = true,
    var snoozeDurationMinutes: Int = 10,
    var timeFormatPreference: TimeFormatSetting = TimeFormatSetting.SYSTEM_DEFAULT
) {

    fun getFormattedTime(isSystem24HourFormat: Boolean): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }

        val sdf = when (timeFormatPreference) {
            TimeFormatSetting.H12 -> SimpleDateFormat("hh:mm a", Locale.getDefault())
            TimeFormatSetting.H24 -> SimpleDateFormat("HH:mm", Locale.getDefault())
            TimeFormatSetting.SYSTEM_DEFAULT -> {
                if (isSystem24HourFormat) {
                    SimpleDateFormat("HH:mm", Locale.getDefault())
                } else {
                    SimpleDateFormat("hh:mm a", Locale.getDefault())
                }
            }
        }
        return sdf.format(calendar.time)
    }

    fun isRepeating(): Boolean = daysOfWeek.isNotEmpty()
}

