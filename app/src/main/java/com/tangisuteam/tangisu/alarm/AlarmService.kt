package com.tangisuteam.tangisu.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_STOP_SERVICE = "com.tangisuteam.tangisu.STOP_ALARM_SERVICE"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("AlarmService", "Service created")
        createNotificationChannel()

        initializeRingtone()
        initializeVibrator()
    }

    private fun initializeRingtone() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(this, alarmUri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isLooping = true
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                } else {
                    isLooping = true
                    @Suppress("DEPRECATION")
                    streamType = AudioManager.STREAM_ALARM
                }
            }
            Log.d("AlarmService", "Ringtone initialized")
        } catch (e: Exception) {
            Log.e("AlarmService", "Error initializing ringtone", e)
        }
    }

    private fun initializeVibrator() {
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
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getStringExtra("ALARM_ID") ?: currentAlarmId
        val alarmLabel = intent?.getStringExtra("ALARM_LABEL")?.ifBlank { "Time to wake up!" } ?: currentAlarmLabel

        currentAlarmId = alarmId
        currentAlarmLabel = alarmLabel

        Log.d("AlarmService", "Service started. Label: '$currentAlarmLabel', ID: '$currentAlarmId'")

        // Start foreground with notification
        val notification = createNotification(alarmId, alarmLabel)
        startForeground(NOTIFICATION_ID, notification)

        // Always ensure sound and vibration are playing
        ensureMediaPlayback(alarmId)

        return START_STICKY
    }

    private fun ensureMediaPlayback(alarmId: String?) {
        // Ensure ringtone is playing
        try {
            if (ringtone?.isPlaying != true) {
                Log.d("AlarmService", "Starting/Resuming ringtone playback")
                ringtone?.play()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Error playing ringtone", e)
            // Try to reinitialize if it failed
            initializeRingtone()
            ringtone?.play()
        }

        // Ensure vibration
        serviceScope.launch {
            try {
                val alarm = alarmId?.let { alarmRepository.getAlarmById(it) }
                if (alarm?.shouldVibrate == true) {
                    // Cancel any existing vibration first
                    vibrator?.cancel()

                    Log.d("AlarmService", "Starting/Resuming vibration")
                    val vibrationEffect = VibrationEffect.createWaveform(
                        longArrayOf(0, 1000, 500),
                        0 // Repeat indefinitely
                    )
                    vibrator?.vibrate(vibrationEffect)
                } else {
                    Log.d("AlarmService", "Vibration is disabled for this alarm")
                }
            } catch (e: Exception) {
                Log.e("AlarmService", "Error with vibration", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AlarmService", "Service destroyed. Stopping ringtone and vibration.")

        try {
            ringtone?.stop()
        } catch (e: Exception) {
            Log.e("AlarmService", "Error stopping ringtone", e)
        }

        vibrator?.cancel()

        serviceScope.cancel()
    }

    private fun createNotification(alarmId: String?, alarmLabel: String): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", alarmLabel)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            1,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // When notification is dismissed, just restart the service with same data
        // This keeps the notification persistent without stopping the alarm
        val deleteIntent = Intent(this, AlarmService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", alarmLabel)
            // NO ACTION_STOP_SERVICE - we want to keep it running
        }
        val deletePendingIntent = PendingIntent.getService(
            this,
            2,
            deleteIntent,
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
            .setAutoCancel(false)
            .addAction(0, "Dismiss", fullScreenPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for Tangisu Alarms"
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(false) // We handle vibration ourselves
            setSound(null, null) // We handle sound ourselves
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
