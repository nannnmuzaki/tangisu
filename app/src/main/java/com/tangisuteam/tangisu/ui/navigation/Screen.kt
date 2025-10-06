package com.tangisuteam.tangisu.ui.navigation

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable


sealed class Screen(val route: String) {
    // For the main list of alarms
    data object AlarmList : Screen("alarm_list_screen")

    data object AddEditAlarm : Screen("add_edit_alarm_screen?alarmId={alarmId}") {
        // Helper function to create the route with an actual alarm ID
        fun createRoute(alarmId: String?) = "add_edit_alarm_screen?alarmId=$alarmId"
    }
}
