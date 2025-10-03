package com.tangisuteam.tangisu.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect // Import this
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tangisuteam.tangisu.R
import com.tangisuteam.tangisu.data.repository.DummyAlarmRepositoryProvider // Import repository
import com.tangisuteam.tangisu.ui.alarm.AlarmActivity
import kotlinx.coroutines.GlobalScope // Import CoroutineScope
import kotlinx.coroutines.launch // Import launch

class AlarmService : Service() {

    private val NOTIFICATION_CHANNEL_ID = "ALARM_CHANNEL"
    private val NOTIFICATION_ID = 123

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var currentAlarmId: String? = null
    private var currentAlarmLabel: String = "Time to wake up!"
    private var isStopping = false

    companion object {
        const val ACTION_STOP_SERVICE = "com.tangisuteam.tangisu.STOP_ALARM_SERVICE"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("AlarmService", "Service created")
        createNotificationChannel()

        // Initialize Ringtone
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, alarmUri)
        ringtone?.isLooping = true

        // Initialize Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // --- HANDLE THE STOP ACTION ---
        if (intent?.action == ACTION_STOP_SERVICE) {
            Log.d("AlarmService", "Stop action received. Stopping service.")
            isStopping = true
            stopSelf() // This will trigger onDestroy()
            return START_NOT_STICKY // Don't restart
        }

        intent?.getStringExtra("ALARM_ID")?.let {
            currentAlarmId = it
        }
        intent?.getStringExtra("ALARM_LABEL")?.let {
            currentAlarmLabel = it.ifBlank { "Time to wake up!" }
        }

        Log.d("AlarmService", "Service started. Label: '$currentAlarmLabel', ID: '$currentAlarmId'")

        // This ensures the notification is always present while the service is running
        startForeground(NOTIFICATION_ID, createNotification(currentAlarmId, currentAlarmLabel))

        // --- START SOUND AND VIBRATION ---
        // We only want to start them the first time, not on service restart
        if (ringtone?.isPlaying == false) {
            Log.d("AlarmService", "Starting media playback")
            ringtone?.play()

            // Fetch the specific alarm to check if it should vibrate
            GlobalScope.launch {
                val alarm = currentAlarmId?.let { DummyAlarmRepositoryProvider.instance.getAlarmById(it) }
                if (alarm?.shouldVibrate == true) {
                    Log.d("AlarmService", "Starting vibration")
                    // --- THIS IS THE CORRECTED CODE ---
                    // Create an effect for an insistent, repeating buzz
                    val vibrationEffect = VibrationEffect.createWaveform(
                        longArrayOf(0, 1000, 500), // Wait 0ms, Vibrate 1s, Wait 0.5s
                        0 // Repeat from the start of the pattern (index 0)
                    )
                    vibrator?.vibrate(vibrationEffect)
                } else {
                    Log.d("AlarmService", "Vibration is disabled for this alarm.")
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isStopping = false
        Log.d("AlarmService", "Service destroyed. Stopping ringtone and vibration.")
        // --- STOP SOUND AND VIBRATION ---
        ringtone?.stop()
        vibrator?.cancel()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // This makes sure that if the user swipes the app from recents, the alarm continues.
        // We can restart the service to be safe.
        if (!isStopping) {
            val restartServiceIntent = Intent(applicationContext, this.javaClass).apply {
                setPackage(packageName)
                putExtra("ALARM_ID", currentAlarmId)
                putExtra("ALARM_LABEL", currentAlarmLabel)
            }
            startService(restartServiceIntent)
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun createNotification(alarmId: String?, alarmLabel: String): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId) // Pass ID to the activity too
            putExtra("ALARM_LABEL", alarmLabel)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            1, // Request code
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // This is the PendingIntent that gets fired when the notification is swiped away
        val deleteIntent = PendingIntent.getService(
            this,
            2, // Different request code
            Intent(this, AlarmService::class.java).apply {
                putExtra("ALARM_ID", alarmId)
                putExtra("ALARM_LABEL", alarmLabel)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Alarm")
            .setContentText(alarmLabel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .addAction(0, "Dismiss", fullScreenPendingIntent)
            // This ensures that if the notification is cleared, the service attempts to restart
            .setDeleteIntent(deleteIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH // MAX is crucial
        ).apply {
            description = "Channel for Tangisu Alarms"
            setBypassDnd(true) // Allows the alarm to sound even in Do Not Disturb
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
