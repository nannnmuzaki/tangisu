package com.tangisuteam.tangisu.data.model

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

data class VibrationPattern(val pattern: LongArray?, val repeat: Int = -1) {
    companion object {
        val INSISTENT_BUZZ = VibrationPattern(longArrayOf(0, 1000, 500), 0)
        val NONE = VibrationPattern(null, -1)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VibrationPattern

        if (pattern != null) {
            if (other.pattern == null) return false
            if (!pattern.contentEquals(other.pattern)) return false
        } else if (other.pattern != null) return false
        if (repeat != other.repeat) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pattern?.contentHashCode() ?: 0
        result = 31 * result + repeat
        return result
    }
}

enum class TimeFormatSetting {
    SYSTEM_DEFAULT,
    H12,
    H24
}

data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    var isEnabled: Boolean = true,
    var label: String? = null,
    var daysOfWeek: Set<DayOfWeek> = emptySet(),
    var challengeType: ChallengeType = ChallengeType.NONE,
    var ringtoneUri: String? = null,
    var shouldVibrate: Boolean = true,
    var vibrationPattern: VibrationPattern = VibrationPattern.INSISTENT_BUZZ,
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

