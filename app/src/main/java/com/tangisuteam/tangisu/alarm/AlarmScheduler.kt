package com.tangisuteam.tangisu.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.tangisuteam.tangisu.data.model.Alarm
import com.tangisuteam.tangisu.data.model.DayOfWeek // <-- IMPORT THIS
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    // Helper to check for permission
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedule(alarm: Alarm) {
        if (!canScheduleExactAlarms()) {
            Log.e("AlarmScheduler", "Cannot schedule exact alarms. Missing permission.")
            return
        }

        // Pass the alarm's daysOfWeek to the new getNextTriggerTime function
        val triggerTime = getNextTriggerTime(alarm.hour, alarm.minute, alarm.daysOfWeek)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.tangisuteam.tangisu.ALARM_TRIGGER"
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime.timeInMillis,
            pendingIntent
        )

        Log.d("AlarmScheduler", "Scheduled alarm ${alarm.id} for ${triggerTime.time}")
    }

    fun cancel(alarm: Alarm) {
        // ... (this function is correct)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.tangisuteam.tangisu.ALARM_TRIGGER"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d("AlarmScheduler", "Cancelled alarm ${alarm.id}")
    }


    private fun getNextTriggerTime(hour: Int, minute: Int, repeatDays: Set<DayOfWeek>): Calendar {
        val now = Calendar.getInstance()
        val triggerTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the alarm is not repeating (it's a one-time alarm)
        if (repeatDays.isEmpty()) {
            // If the calculated time is in the past, schedule for tomorrow. Otherwise, today.
            if (triggerTime.before(now)) {
                triggerTime.add(Calendar.DAY_OF_YEAR, 1)
            }
            return triggerTime
        }

        // --- Logic for repeating alarms ---
        // Convert our DayOfWeek enum to Calendar's integer constants
        val calendarDays = repeatDays.map { it.toCalendarDay() }.toSortedSet()

        // Iterate up to 7 days to find the next valid day
        for (i in 0..7) {
            val potentialTriggerDay = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i) // Check today (i=0), tomorrow (i=1), etc.
            }
            val dayOfWeek = potentialTriggerDay.get(Calendar.DAY_OF_WEEK)

            // If the checked day is one of the selected repeat days
            if (calendarDays.contains(dayOfWeek)) {
                // Set the time for that day
                potentialTriggerDay.set(Calendar.HOUR_OF_DAY, hour)
                potentialTriggerDay.set(Calendar.MINUTE, minute)
                potentialTriggerDay.set(Calendar.SECOND, 0)
                potentialTriggerDay.set(Calendar.MILLISECOND, 0)

                // If this potential trigger is in the future, we've found our time!
                if (potentialTriggerDay.after(now)) {
                    return potentialTriggerDay
                }
            }
        }

        // Fallback in case something unexpected happens (shouldn't be reached)
        // This will schedule for the first available day next week.
        triggerTime.add(Calendar.DAY_OF_YEAR, 7)
        return triggerTime
    }

    // Helper extension function to convert our DayOfWeek enum to Calendar's integer constants
    private fun DayOfWeek.toCalendarDay(): Int {
        return when (this) {
            DayOfWeek.SUNDAY -> Calendar.SUNDAY
            DayOfWeek.MONDAY -> Calendar.MONDAY
            DayOfWeek.TUESDAY -> Calendar.TUESDAY
            DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
            DayOfWeek.THURSDAY -> Calendar.THURSDAY
            DayOfWeek.FRIDAY -> Calendar.FRIDAY
            DayOfWeek.SATURDAY -> Calendar.SATURDAY
        }
    }
}
