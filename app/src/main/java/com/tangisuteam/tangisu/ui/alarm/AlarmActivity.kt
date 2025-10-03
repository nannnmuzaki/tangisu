package com.tangisuteam.tangisu.ui.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.tangisuteam.tangisu.alarm.AlarmService
import com.tangisuteam.tangisu.ui.theme.TangisuTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangisuteam.tangisu.alarm.AlarmScheduler
import com.tangisuteam.tangisu.data.model.ChallengeType
import com.tangisuteam.tangisu.data.repository.DummyAlarmRepositoryProvider

class AlarmActivity : ComponentActivity() {

    private lateinit var viewModel: AlarmActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Create ViewModel with its dependencies ---
        val alarmRepository = DummyAlarmRepositoryProvider.instance
        val alarmScheduler = AlarmScheduler(applicationContext)
        val factory = AlarmActivityViewModelFactory(alarmRepository, alarmScheduler)
        viewModel = ViewModelProvider(this, factory)[AlarmActivityViewModel::class.java]
        // --- End of ViewModel creation ---

        // These flags are crucial for showing the activity over the lock screen and turning the screen on.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val alarmId = intent.getStringExtra("ALARM_ID")
        viewModel.loadAlarm(alarmId)

        setContent {
            TangisuTheme {
                val onDismiss = {
                    // 1. Perform robust dismiss logic (disable/reschedule)
                    viewModel.dismissAlarm()

                    // 2. Stop the currently playing sound/vibration
                    val stopServiceIntent = Intent(this, AlarmService::class.java).apply {
                        action = AlarmService.ACTION_STOP_SERVICE
                    }
                    startService(stopServiceIntent)

                    // 3. Close the UI
                    finishAndRemoveTask()
                }

                when (val state = viewModel.uiState) {
                    is AlarmUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is AlarmUiState.Error -> {
                        ErrorScreen(message = state.message, onDismiss = onDismiss)
                    }
                    is AlarmUiState.Success -> {
                        val alarm = state.alarm
                        val alarmLabel = alarm.label ?: "Time to wake up!"

                        // Conditionally display the correct screen based on challenge type
                        if (alarm.challengeType == ChallengeType.MATH) {
                            // --- PASS THE NEW VIEWMODEL STATE ---
                            MathChallengeScreen(
                                alarmLabel = alarmLabel,
                                progress = viewModel.progress,
                                progressText = viewModel.progressText,
                                mathProblem = viewModel.currentProblemText,
                                userAnswer = viewModel.userAnswer,
                                isChallengeComplete = viewModel.isAnswerCorrect, // This now signifies the whole challenge is done
                                onAnswerChange = { viewModel.onUserAnswerChange(it) },
                                onDismiss = onDismiss
                            )
                        } else {
                            // Default screen for ChallengeType.NONE
                            AlarmFiringScreen(
                                alarmLabel = alarmLabel,
                                onDismiss = onDismiss
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(message: String, onDismiss: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Error", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
fun AlarmFiringScreen(alarmLabel: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Alarm!",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = alarmLabel,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(128.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("Stop Alarm", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
