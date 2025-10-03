package com.tangisuteam.tangisu.ui.alarm

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MathChallengeScreen(
    alarmLabel: String,
    // --- UPDATED PARAMETERS ---
    progress: Float,
    progressText: String,
    mathProblem: String,
    userAnswer: String,
    isChallengeComplete: Boolean, // Renamed for clarity
    onAnswerChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
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
                text = alarmLabel,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            // --- NEW: Progress Indicator ---
            if (!isChallengeComplete) { // Only show progress if not yet complete
                Text(text = progressText, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress }, // Use lambda for Compose 1.7+
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(8.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = mathProblem,
                style = MaterialTheme.typography.headlineLarge
            )

            // --- Don't show the text field if the challenge is done ---
            if (!isChallengeComplete) {
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = userAnswer,
                    onValueChange = onAnswerChange,
                    label = { Text("Your Answer") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(Modifier.height(64.dp))

            Button(
                onClick = onDismiss,
                enabled = isChallengeComplete, // Button is enabled only if all problems are solved
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("Dismiss", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
