package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.repository.AccountRepository
import com.seoulhankuko.app.data.repository.AuthRepository
import com.seoulhankuko.app.data.repository.LessonRepository
import com.seoulhankuko.app.data.repository.UserProgressRepository
import com.seoulhankuko.app.domain.model.AnswerStatus
import com.seoulhankuko.app.domain.model.ChallengeType
import com.seoulhankuko.app.domain.model.ChallengeWithOptions
import com.seoulhankuko.app.domain.model.Constants
import com.seoulhankuko.app.domain.model.LessonWithChallenges
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LessonViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<LessonUiState>(LessonUiState.Loading)
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()
    
    fun loadLesson(lessonId: Int) {
        _uiState.update { LessonUiState.Loading }
        
        viewModelScope.launch {
            try {
                Timber.d("Loading lesson $lessonId")
                
                // Get authentication token
                val token = authRepository.getCurrentToken()
                val userId = "guest"
                
                Timber.d("Using token: ${token?.take(20)}..., userId: $userId")
                
                val lessonWithChallenges = lessonRepository.getLessonWithChallenges(lessonId, userId ?: "guest", token)
                if (lessonWithChallenges != null) {
                    Timber.d("Successfully loaded lesson with ${lessonWithChallenges.challenges.size} challenges")
                    Timber.d("Lesson title: ${lessonWithChallenges.lesson.title}")
                    Timber.d("Challenges: ${lessonWithChallenges.challenges.map { it.challenge.question }}")
                    
                    _uiState.update {
                        LessonUiState.Success(
                            lessonWithChallenges = lessonWithChallenges,
                            userProgress = null,
                            currentChallengeIndex = 0
                        )
                    }
                } else {
                    Timber.w("Lesson not found for ID: $lessonId")
                    _uiState.update { 
                        LessonUiState.Error("Lesson not found. Please check your internet connection and try again.")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load lesson $lessonId")
                val errorMessage = when {
                    e.message?.contains("network", ignoreCase = true) == true -> 
                        "Network error. Please check your internet connection."
                    e.message?.contains("unauthorized", ignoreCase = true) == true -> 
                        "Authentication failed. Please sign in again."
                    e.message?.contains("timeout", ignoreCase = true) == true -> 
                        "Request timeout. Please try again."
                    else -> "Failed to load lesson: ${e.message ?: "Unknown error"}"
                }
                _uiState.update { LessonUiState.Error(errorMessage) }
            }
        }
    }
    
    private fun getCurrentUserId(): String {
        // Try to get user ID from auth state or use default
        return try {
            // This is a simplified approach - in a real app you might want to observe auth state
            "default_user" // TODO: Get actual user ID from AuthState
        } catch (e: Exception) {
            Timber.w(e, "Failed to get user ID, using default")
            "default_user"
        }
    }
    
    fun selectOption(optionId: Int) {
        val currentState = _uiState.value
        if (currentState !is LessonUiState.Success) return
        
        // Only allow changing answer if status is NONE (before checking)
        if (currentState.status != AnswerStatus.NONE) {
            return
        }
        
        _uiState.update { 
            (it as? LessonUiState.Success)?.copy(selectedOption = optionId) ?: it
        }
    }

    fun submitPracticeCorrectAnswer(lessonId: Int, questionId: Int, selectedOptionId: Int) {
        viewModelScope.launch {
            try {
                val token = authRepository.getCurrentToken()
                if (!token.isNullOrBlank() && lessonId > 0 && questionId > 0) {
                    val result = lessonRepository.submitPracticeQuestion(
                        lessonId = lessonId,
                        questionId = questionId,
                        token = token,
                        selectedOptionId = selectedOptionId
                    )
                    result.onSuccess {
                        Timber.d("Practice submit API success for lesson=$lessonId question=$questionId")
                        // Optionally update backend progress immediately
                        val update = lessonRepository.updateLessonProgress(lessonId, token)
                        update.onSuccess { 
                            Timber.d("Progress updated after submit")
                            // Update streak after quiz/question completed
                            updateStreakAfterActivity()
                        }
                        update.onFailure { e -> Timber.w(e, "Progress update failed after submit") }
                    }.onFailure { err ->
                        Timber.e(err, "Practice submit API failed")
                    }
                } else {
                    Timber.w("Skip practice submit: token=${token?.take(5)}..., lessonId=$lessonId, questionId=$questionId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to submit practice question to backend")
            }
        }
    }

    fun submitExercise(
        exerciseId: Int,
        lessonId: Int,
        selectedAnswers: Map<Int, Int>? = null,
        response: String? = null,
        audioUrl: String? = null
    ) {
        viewModelScope.launch {
            try {
                val token = authRepository.getCurrentToken()
                if (!token.isNullOrBlank() && exerciseId > 0 && lessonId > 0) {
                    val result = lessonRepository.submitExercise(
                        exerciseId = exerciseId,
                        token = token,
                        response = response,
                        audioUrl = audioUrl,
                        selectedAnswers = selectedAnswers
                    )
                    result.onSuccess {
                        Timber.d("Exercise submit API success for exercise=$exerciseId lesson=$lessonId")
                        // Update backend progress after exercise submission
                        val update = lessonRepository.updateLessonProgress(lessonId, token)
                        update.onSuccess { 
                            Timber.d("Progress updated after exercise submit")
                            // Update streak after exercise completed
                            updateStreakAfterActivity()
                            // Reload lesson data to get updated progress
                            loadLesson(lessonId)
                        }
                        update.onFailure { e -> Timber.w(e, "Progress update failed after exercise submit") }
                    }.onFailure { err ->
                        Timber.e(err, "Exercise submit API failed")
                    }
                } else {
                    Timber.w("Skip exercise submit: token=${token?.take(5)}..., exerciseId=$exerciseId, lessonId=$lessonId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to submit exercise to backend")
            }
        }
    }
    
    /**
     * Updates lesson progress when a quiz or exercise is completed
     * This method should be called from ExerciseScreen, etc.
     */
    fun updateProgress(lessonId: Int) {
        // No-op: backend progress update removed; just mark updated in UI
        _uiState.update { currentState ->
            if (currentState is LessonUiState.Success) {
                currentState.copy(progressUpdated = true)
            } else currentState
        }
    }
    
    /**
     * Updates lesson progress on backend and calls callback when done
     * Also reloads lesson data to get updated progress
     * Updates streak if lesson is newly completed
     */
    fun updateLessonProgress(lessonId: Int, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = authRepository.getCurrentToken()
                val result = lessonRepository.updateLessonProgress(lessonId, token)
                
                result.onSuccess { responseBody ->
                    Timber.d("Successfully updated lesson progress for lesson $lessonId")
                    
                    // Check if lesson was completed and update streak
                    val wasCompletedBefore = (_uiState.value as? LessonUiState.Success)?.isLessonCompleted ?: false
                    val progressPercent = (responseBody as? Map<*, *>)?.get("progress_percent") as? Number
                    val isNowCompleted = progressPercent?.toFloat() ?: 0f >= 80f
                    
                    // Update streak if lesson was newly completed
                    if (!wasCompletedBefore && isNowCompleted && token != null) {
                        updateUserStreak(token)
                    }
                    
                    // Reload lesson data to get updated progress
                    loadLesson(lessonId)
                    
                    _uiState.update { currentState ->
                        if (currentState is LessonUiState.Success) {
                            currentState.copy(progressUpdated = true)
                        } else currentState
                    }
                }.onFailure { error ->
                    Timber.e(error, "Failed to update lesson progress for lesson $lessonId")
                    // Reload anyway to try to get latest progress
                    loadLesson(lessonId)
                    
                    // Still mark as updated in UI even if API call failed
                    _uiState.update { currentState ->
                        if (currentState is LessonUiState.Success) {
                            currentState.copy(progressUpdated = true)
                        } else currentState
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while updating lesson progress")
                // Reload anyway
                loadLesson(lessonId)
                
                // Still mark as updated in UI even if exception occurs
                _uiState.update { currentState ->
                    if (currentState is LessonUiState.Success) {
                        currentState.copy(progressUpdated = true)
                    } else currentState
                    }
            } finally {
                onComplete()
            }
        }
    }
    
    /**
     * Updates user streak after completing activities
     */
    private suspend fun updateUserStreak(token: String) {
        try {
            val activeAccount = accountRepository.getActiveAccount()
            val username = activeAccount?.email ?: activeAccount?.displayName
            
            if (!username.isNullOrBlank()) {
                val result = userProgressRepository.updateUserStreak(username, token)
                result.onSuccess { streakResponse ->
                    Timber.d("Successfully updated streak: ${streakResponse.newStreak}, bonus EXP: ${streakResponse.streakBonusExp}")
                }.onFailure { error ->
                    Timber.w(error, "Failed to update streak")
                }
            } else {
                Timber.w("Cannot update streak: no active account found")
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while updating streak")
        }
    }
    
    /**
     * Updates user streak after completing quiz or exercise
     * Called from completion handlers
     */
    fun updateStreakAfterActivity() {
        viewModelScope.launch {
            val token = authRepository.getCurrentToken()
            if (token != null) {
                updateUserStreak(token)
            } else {
                Timber.w("Cannot update streak: no token available")
            }
        }
    }
    
}

// UI State - exported for use in Composables
sealed class LessonUiState {
    object Loading : LessonUiState()
    
    data class Success(
        val lessonWithChallenges: LessonWithChallenges? = null,
        val userProgress: Any? = null, // Removed LocalUserProgressRepository dependency
        val currentChallengeIndex: Int = 0,
        val selectedOption: Int? = null,
        val status: AnswerStatus = AnswerStatus.NONE,
        val isLessonCompleted: Boolean = false,
        val heartsReduced: Boolean = false,
        val completedChallenges: Set<Int> = emptySet(),
        val canProceedAfterWrong: Boolean = false,
        val progressUpdated: Boolean = false
    ) : LessonUiState() {
        fun getCurrentChallenge(): ChallengeWithOptions? {
            return lessonWithChallenges?.challenges?.getOrNull(currentChallengeIndex)
        }
        
        fun getProgressPercentage(): Float {
            val challenges = lessonWithChallenges?.challenges ?: return 0f
            if (challenges.isEmpty()) return 0f
            
            val completedCount = completedChallenges.size
            return completedCount.toFloat() / challenges.size.toFloat()
        }
        
        fun getTotalChallenges(): Int {
            return lessonWithChallenges?.challenges?.size ?: 0
        }
        
        fun isLastChallenge(): Boolean {
            return currentChallengeIndex >= (getTotalChallenges() - 1)
        }
        
        fun allChallengesCompleted(): Boolean {
            return completedChallenges.size >= getTotalChallenges() && getTotalChallenges() > 0
        }
    }
    
    data class Error(val message: String) : LessonUiState()
}

