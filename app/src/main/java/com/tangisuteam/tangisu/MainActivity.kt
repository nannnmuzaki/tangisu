package com.tangisuteam.tangisu

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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tangisuteam.tangisu.ui.alarm.AddEditAlarmScreen
import com.tangisuteam.tangisu.ui.alarm.AlarmListScreen
import com.tangisuteam.tangisu.ui.navigation.Screen
import com.tangisuteam.tangisu.ui.theme.TangisuTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TangisuTheme {
                // Create the NavController
                val navController = rememberNavController()

                // Set up the NavHost
                NavHost(
                    navController = navController,
                    startDestination = Screen.AlarmList.route
                ) {
                    // --- Alarm List Screen Destination ---
                    composable(route = Screen.AlarmList.route) {
                        AlarmListScreen(
                            onNavigateToAddAlarm = {
                                // Navigate without an ID for adding
                                navController.navigate(Screen.AddEditAlarm.createRoute(null))
                            },
                            onNavigateToEditAlarm = { alarmId ->
                                // Navigate with an ID for editing
                                navController.navigate(Screen.AddEditAlarm.createRoute(alarmId))
                            }
                        )
                    }

                    // --- Add/Edit Alarm Screen Destination ---
                    composable(
                        route = Screen.AddEditAlarm.route,
                        arguments = listOf(
                            navArgument("alarmId") {
                                type = NavType.StringType
                                nullable = true // Allow null for adding new alarms
                            }
                        )
                    ) { backStackEntry ->
                        val alarmId = backStackEntry.arguments?.getString("alarmId")
                        AddEditAlarmScreen(
                            alarmId = alarmId,
                            onNavigateBack = {
                                navController.popBackStack() // Go back to the previous screen
                            }
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

