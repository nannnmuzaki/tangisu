package com.tangisuteam.tangisu.ui.alarm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.tangisuteam.tangisu.alarm.AlarmScheduler
import com.tangisuteam.tangisu.data.model.Alarm
import com.tangisuteam.tangisu.data.model.ChallengeType
import com.tangisuteam.tangisu.data.repository.AlarmRepository
import kotlinx.coroutines.launch
import kotlin.random.Random

// Sealed class for the type of math operation
private sealed class MathOperation {
    abstract val symbol: String
    abstract fun calculate(num1: Int, num2: Int): Int

    data object Add : MathOperation() {
        override val symbol = "+"
        override fun calculate(num1: Int, num2: Int): Int = num1 + num2
    }

    data object Multiply : MathOperation() {
        override val symbol = "×" // Use a proper multiplication symbol
        override fun calculate(num1: Int, num2: Int): Int = num1 * num2
    }
}

// Data class to hold a single math problem
private data class MathProblem(
    val problemText: String,
    val correctAnswer: Int
)

// State for the UI to handle loading/error/success
sealed class AlarmUiState {
    data object Loading : AlarmUiState()
    data class Success(val alarm: Alarm) : AlarmUiState()
    data class Error(val message: String) : AlarmUiState()
}

class AlarmActivityViewModel(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    var uiState by mutableStateOf<AlarmUiState>(AlarmUiState.Loading)
        private set

    // --- NEW: State for the series of math challenges ---
    private val problems = mutableListOf<MathProblem>()
    private var currentProblemIndex by mutableStateOf(0)
    private val totalProblems = 10 // Total number of problems to solve

    // --- UPDATED: UI-facing state ---
    val progress: Float
        get() = if (problems.isEmpty()) 0f else (currentProblemIndex.toFloat() / totalProblems)

    val progressText: String
        get() = if (problems.isEmpty()) "" else "Problem ${currentProblemIndex + 1} of $totalProblems"

    var currentProblemText by mutableStateOf("")
        private set
    var userAnswer by mutableStateOf("")
        private set
    var isAnswerCorrect by mutableStateOf(false)
        private set

    fun loadAlarm(alarmId: String?) {
        if (alarmId == null) {
            uiState = AlarmUiState.Error("Invalid Alarm ID.")
            return
        }

        viewModelScope.launch {
            val alarm = alarmRepository.getAlarmById(alarmId)
            if (alarm != null) {
                uiState = AlarmUiState.Success(alarm)
                if (alarm.challengeType == ChallengeType.MATH) {
                    generateProblemSeries()
                    presentCurrentProblem()
                }
            } else {
                uiState = AlarmUiState.Error("Alarm not found.")
            }
        }
    }

    private fun generateProblemSeries() {
        problems.clear()
        repeat(totalProblems) {
            val operation = if (Random.nextBoolean()) MathOperation.Add else MathOperation.Multiply
            val problem = when (operation) {
                is MathOperation.Add -> {
                    val num1 = Random.nextInt(10, 100)
                    val num2 = Random.nextInt(10, 100)
                    MathProblem(
                        problemText = "$num1 ${operation.symbol} $num2 = ?",
                        correctAnswer = operation.calculate(num1, num2)
                    )
                }
                is MathOperation.Multiply -> {
                    val num1 = Random.nextInt(2, 13) // Smaller numbers for multiplication
                    val num2 = Random.nextInt(2, 13)
                    MathProblem(
                        problemText = "$num1 ${operation.symbol} $num2 = ?",
                        correctAnswer = operation.calculate(num1, num2)
                    )
                }
            }
            problems.add(problem)
        }
    }

    private fun presentCurrentProblem() {
        if (currentProblemIndex < problems.size) {
            val problem = problems[currentProblemIndex]
            currentProblemText = problem.problemText
            userAnswer = "" // Clear previous answer
            // The dismiss button is only enabled when all problems are solved
            isAnswerCorrect = false
        } else {
            // All problems are solved
            currentProblemText = "Great job! You're awake."
            userAnswer = ""
            isAnswerCorrect = true // Enable the dismiss button
        }
    }

    fun onUserAnswerChange(answer: String) {
        userAnswer = answer
        // Immediately check if the answer is correct for the current problem
        val currentProblem = problems.getOrNull(currentProblemIndex) ?: return
        if (answer.toIntOrNull() == currentProblem.correctAnswer) {
            // If correct, move to the next problem
            currentProblemIndex++
            presentCurrentProblem()
        }
    }

    fun dismissAlarm() {
        val currentState = uiState
        if (currentState !is AlarmUiState.Success) return

        val alarm = currentState.alarm

        viewModelScope.launch {
            if (alarm.isRepeating()) {
                alarmScheduler.schedule(alarm)
            } else {
                // For a one-time alarm, disable it and cancel it from AlarmManager.
                val updatedAlarm = alarm.copy(isEnabled = false)
                alarmRepository.updateAlarm(updatedAlarm)
                alarmScheduler.cancel(updatedAlarm)
            }
        }
    }
}

// Factory to provide dependencies to the ViewModel
class AlarmActivityViewModelFactory(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(AlarmActivityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlarmActivityViewModel(alarmRepository, alarmScheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
