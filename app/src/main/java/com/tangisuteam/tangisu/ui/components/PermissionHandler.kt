package com.tangisuteam.tangisu.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangisuteam.tangisu.alarm.AlarmScheduler

private data class PermissionInfo(
    val title: String,
    val rationale: String,
    val isGranted: Boolean
)

@Composable
fun PermissionHandler(
    onAllPermissionsGranted: @Composable () -> Unit
) {
    val context = LocalContext.current
    var permissionsState by remember { mutableStateOf<Map<String, PermissionInfo>>(emptyMap()) }

    val checkPermissions = remember {
        { ctx: Context, onStateUpdated: (Map<String, PermissionInfo>) -> Unit ->
            val newStates = mutableMapOf<String, PermissionInfo>()

            // 1. Check Notification Permission (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                newStates[Manifest.permission.POST_NOTIFICATIONS] = PermissionInfo(
                    title = "Notification Permission",
                    rationale = "Required to show the alarm notification, even when the app is in the background.",
                    isGranted = ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                )
            }

            // 2. Check Exact Alarm Scheduling Permission (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmScheduler = AlarmScheduler(ctx)
                newStates["EXACT_ALARM"] = PermissionInfo(
                    title = "Exact Alarm Scheduling",
                    rationale = "Allows the app to fire alarms at the precise time you set. It is essential for the app to function.",
                    isGranted = alarmScheduler.canScheduleExactAlarms()
                )
            }
            onStateUpdated(newStates)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            // Re-check permissions after the user responds to the dialog
            checkPermissions(context) { newState -> permissionsState = newState }
        }
    )

    // Check permissions when the composable first launches
    LaunchedEffect(Unit) {
        checkPermissions(context) { newState -> permissionsState = newState }
    }

    val allPermissionsGranted = permissionsState.values.all { it.isGranted }

    if (allPermissionsGranted) {
        onAllPermissionsGranted()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Permissions Required", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "This app needs the following permissions to work correctly.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            permissionsState.filter { !it.value.isGranted }.forEach { (key, state) ->
                PermissionRequestCard(
                    title = state.title,
                    rationale = state.rationale,
                    onClick = {
                        when (key) {
                            Manifest.permission.POST_NOTIFICATIONS -> {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            "EXACT_ALARM" -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        }
                    }
                )
                Spacer(Modifier.height(16.dp))
            }

            // A button to re-check permissions after the user returns from settings
            Button(onClick = { checkPermissions(context) { newState -> permissionsState = newState } }) {
                Text("Refresh Permissions")
            }
        }
    }
}

@Composable
private fun PermissionRequestCard(
    title: String,
    rationale: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(rationale, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Start)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Grant Permission")
            }
        }
    }
}
