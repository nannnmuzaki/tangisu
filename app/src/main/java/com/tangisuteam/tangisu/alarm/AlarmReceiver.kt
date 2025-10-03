package com.tangisuteam.tangisu.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.e("AlarmReceiver", "Context or Intent is null, cannot process alarm.")
            return
        }

        if (intent.action == "com.tangisuteam.tangisu.ALARM_TRIGGER") {
            Log.d("AlarmReceiver", "Alarm intent received!")

            val alarmId = intent.getStringExtra("ALARM_ID")
            val alarmLabel = intent.getStringExtra("ALARM_LABEL")
            Log.d("AlarmReceiver", "Alarm ID: $alarmId, Label: $alarmLabel")

            // --- THIS IS THE KEY CHANGE ---
            // Instead of just logging, we now create and start the AlarmService.
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                // Pass the alarm details to the service so it knows what to do.
                putExtra("ALARM_ID", alarmId)
                putExtra("ALARM_LABEL", alarmLabel)
            }

            // On modern Android, we must use startForegroundService to start a service
            // from the background. The service then has a few seconds to call
            // startForeground() itself, which we already implemented.
            context.startForegroundService(serviceIntent)
            // --- END OF CHANGE ---
        }
    }
}
