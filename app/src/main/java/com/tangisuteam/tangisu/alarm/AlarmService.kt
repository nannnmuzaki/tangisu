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
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tangisuteam.tangisu.R
import com.tangisuteam.tangisu.data.repository.AlarmRepository
import com.tangisuteam.tangisu.ui.alarm.AlarmActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmService : Service() {
    @Inject
    lateinit var alarmRepository: AlarmRepository
    private val NOTIFICATION_CHANNEL_ID = "ALARM_CHANNEL"
    private val NOTIFICATION_ID = 123

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var currentAlarmId: String? = null
    private var currentAlarmLabel: String = "Time to wake up!"
    private var isStopping = false
    private var hasStartedPlayback = false // <-- 1. ADD THIS FLAG

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        if (intent?.action == ACTION_STOP_SERVICE) {
            Log.d("AlarmService", "Stop action received. Stopping service.")
            isStopping = true
            stopSelf()
            return START_NOT_STICKY // Don't restart a service that was explicitly stopped.
        }

        // --- 2. Extract Data ---
        // Get the ID and Label from the incoming intent.
        val alarmId = intent?.getStringExtra("ALARM_ID") ?: currentAlarmId
        val alarmLabel = intent?.getStringExtra("ALARM_LABEL")?.ifBlank { "Time to wake up!" } ?: currentAlarmLabel

        // Update the service's start, crucial for restarts.
        currentAlarmId = alarmId
        currentAlarmLabel = alarmLabel

        Log.d("AlarmService", "Service started. Label: '$currentAlarmLabel', ID: '$currentAlarmId'")

        // --- 3. Start Foreground ---
        // Create the notification with the most up-to-date data.
        val notification = createNotification(alarmId, alarmLabel)
        startForeground(NOTIFICATION_ID, notification)

        // --- 4. Play Media ---
        // The hasStartedPlayback flag prevents the sound from re-playing on every service restart.
        if (!hasStartedPlayback) {
            hasStartedPlayback = true
            Log.d("AlarmService", "Starting media playback for the first time.")

            ringtone?.play()

            serviceScope.launch {
                val alarm = alarmId?.let { alarmRepository.getAlarmById(it) }
                if (alarm?.shouldVibrate == true) {
                    Log.d("AlarmService", "Starting vibration.")
                    val vibrationEffect = VibrationEffect.createWaveform(
                        longArrayOf(0, 1000, 500),
                        0 // Repeat
                    )
                    vibrator?.vibrate(vibrationEffect)
                } else {
                    Log.d("AlarmService", "Vibration is disabled for this alarm.")
                }
            }
        }

        // START_STICKY to ensure the OS tries to restart the service if it's killed.
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isStopping = false
        hasStartedPlayback = false // Reset the flag
        Log.d("AlarmService", "Service destroyed. Stopping ringtone and vibration.")
        ringtone?.stop()
        vibrator?.cancel()
        serviceScope.cancel() // <-- CANCEL THE SCOPE
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
            NotificationManager.IMPORTANCE_HIGH
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
