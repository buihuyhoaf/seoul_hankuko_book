package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.database.entities.UserProgress
import com.seoulhankuko.app.data.repository.AuthRepository
import com.seoulhankuko.app.data.repository.LessonRepository
import com.seoulhankuko.app.data.repository.LocalUserProgressRepository
import com.seoulhankuko.app.domain.model.AnswerStatus
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
    private val userProgressRepository: LocalUserProgressRepository,
    private val authRepository: AuthRepository
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
                val userId = getCurrentUserId()
                
                Timber.d("Using token: ${token?.take(20)}..., userId: $userId")
                
                val lessonWithChallenges = lessonRepository.getLessonWithChallenges(lessonId, userId, token)
                if (lessonWithChallenges != null) {
                    Timber.d("Successfully loaded lesson with ${lessonWithChallenges.challenges.size} challenges")
                    Timber.d("Lesson title: ${lessonWithChallenges.lesson.title}")
                    Timber.d("Challenges: ${lessonWithChallenges.challenges.map { it.challenge.question }}")
                    
                    userProgressRepository.getUserProgress(userId).collect { userProgress ->
                        val currentChallengeIndex = lessonWithChallenges.challenges.indexOfFirst { !it.completed }
                        
                        _uiState.update {
                            LessonUiState.Success(
                                lessonWithChallenges = lessonWithChallenges,
                                userProgress = userProgress,
                                currentChallengeIndex = if (currentChallengeIndex == -1) 0 else currentChallengeIndex,
                                selectedOption = null,
                                status = AnswerStatus.NONE,
                                isLessonCompleted = false,
                                heartsReduced = false,
                                completedChallenges = emptySet()
                            )
                        }
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
        
        if (currentState.selectedOption != null || currentState.status != AnswerStatus.NONE) {
            return
        }
        
        _uiState.update { 
            (it as? LessonUiState.Success)?.copy(selectedOption = optionId) ?: it
        }
    }
    
    fun submitAnswer() {
        val currentState = _uiState.value
        if (currentState !is LessonUiState.Success) return
        
        val selectedOption = currentState.selectedOption ?: return
        val currentChallenge = currentState.getCurrentChallenge() ?: return
        
        val correctOption = currentChallenge.options.find { it.correct }
        val isCorrect = correctOption?.id == selectedOption
        
        viewModelScope.launch {
            val userId = getCurrentUserId()
            
            try {
                if (isCorrect) {
                    Timber.d("Correct answer submitted for challenge ${currentChallenge.challenge.id}")
                    
                    // Mark challenge as completed
                    lessonRepository.completeChallenge(userId, currentChallenge.challenge.id)
                    
                    // Award points
                    userProgressRepository.addPoints(
                        userId,
                        Constants.POINTS_PER_CHALLENGE
                    )
                    
                    _uiState.update { 
                        (it as? LessonUiState.Success)?.copy(
                            status = AnswerStatus.CORRECT,
                            completedChallenges = it.completedChallenges + currentChallenge.challenge.id
                        ) ?: it
                    }
                } else {
                    Timber.d("Wrong answer submitted for challenge ${currentChallenge.challenge.id}")
                    
                    // Reduce hearts for wrong answer
                    userProgressRepository.reduceHearts(userId)
                    
                    _uiState.update { 
                        (it as? LessonUiState.Success)?.copy(
                            status = AnswerStatus.WRONG,
                            heartsReduced = true
                        ) ?: it
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to submit answer")
                _uiState.update { 
                    LessonUiState.Error("Failed to submit answer: ${e.message ?: "Unknown error"}")
                }
            }
        }
    }
    
    fun nextChallenge() {
        val currentState = _uiState.value
        if (currentState !is LessonUiState.Success) return
        
        val nextIndex = currentState.currentChallengeIndex + 1
        val totalChallenges = currentState.lessonWithChallenges?.challenges?.size ?: 0
        
        if (nextIndex >= totalChallenges) {
            // All challenges completed
            _uiState.update { 
                (it as? LessonUiState.Success)?.copy(
                    isLessonCompleted = true,
                    status = AnswerStatus.NONE,
                    selectedOption = null
                ) ?: it
            }
        } else {
            _uiState.update { 
                (it as? LessonUiState.Success)?.copy(
                    currentChallengeIndex = nextIndex,
                    status = AnswerStatus.NONE,
                    selectedOption = null,
                    heartsReduced = false
                ) ?: it
            }
        }
    }
    
    fun retryChallenge() {
        val currentState = _uiState.value
        if (currentState !is LessonUiState.Success) return
        
        _uiState.update { 
            (it as? LessonUiState.Success)?.copy(
                status = AnswerStatus.NONE,
                selectedOption = null,
                heartsReduced = false
            ) ?: it
        }
    }
    
    fun resetLesson() {
        val currentState = _uiState.value
        if (currentState !is LessonUiState.Success) return
        
        _uiState.update { 
            (it as? LessonUiState.Success)?.copy(
                currentChallengeIndex = 0,
                selectedOption = null,
                status = AnswerStatus.NONE,
                isLessonCompleted = false,
                heartsReduced = false,
                completedChallenges = emptySet()
            ) ?: it
        }
    }
    
    fun retryLoadLesson(lessonId: Int) {
        loadLesson(lessonId)
    }
}

// UI State - exported for use in Composables
sealed class LessonUiState {
    object Loading : LessonUiState()
    
    data class Success(
        val lessonWithChallenges: LessonWithChallenges? = null,
        val userProgress: UserProgress? = null,
        val currentChallengeIndex: Int = 0,
        val selectedOption: Int? = null,
        val status: AnswerStatus = AnswerStatus.NONE,
        val isLessonCompleted: Boolean = false,
        val heartsReduced: Boolean = false,
        val completedChallenges: Set<Int> = emptySet()
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

