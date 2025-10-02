package com.tangisuteam.tangisu // Your main package

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangisuteam.tangisu.ui.alarm.AddEditAlarmScreen
import com.tangisuteam.tangisu.ui.alarm.AlarmListScreen
import com.tangisuteam.tangisu.ui.theme.TangisuTheme // Make sure this is correctly imported

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TangisuTheme { // Apply your app's theme

                // Basic navigation stub - Replace with Jetpack Navigation Compose later
                var currentScreen by remember { mutableStateOf("list") }
                var alarmToEditId by remember { mutableStateOf<String?>(null) }

                when (currentScreen) {
                    "list" -> {
                        AlarmListScreen(
                            onNavigateToAddAlarm = {
                                currentScreen = "add_edit" // Navigate to a combined Add/Edit screen
                                alarmToEditId = null // Clear ID for adding new
                                println("Navigate to Add Alarm Screen")
                            },
                            onNavigateToEditAlarm = { id ->
                                currentScreen = "add_edit"
                                alarmToEditId = id
                                println("Navigate to Edit Alarm Screen for ID: $id")
                            }
                        )
                    }
                    "add_edit" -> {
                        // Placeholder for your Add/Edit Alarm Screen
                        // You'll create a new Composable for this.
                        AddEditAlarmScreen(
                            alarmId = alarmToEditId,
                            onNavigateBack = { currentScreen = "list" }
                        )
                    }
                }
            }
        }
    }
}

// Placeholder - you will create a real AddEditAlarmScreen.kt
@Composable
fun AddEditAlarmScreenPlaceholder(alarmId: String?, onBackToList: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (alarmId == null) "Add New Alarm Screen" else "Edit Alarm Screen for ID: $alarmId")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBackToList) {
                Text("Save / Back to List")
            }
        }
    }
}

