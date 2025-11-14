package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.api.model.MistakeResponse
import com.seoulhankuko.app.data.api.model.MistakesListResponse
import com.seoulhankuko.app.data.api.model.QuestionResponse
import com.seoulhankuko.app.data.repository.MistakesRepository
import com.seoulhankuko.app.data.repository.LessonRepository
import com.seoulhankuko.app.domain.model.ChallengeWithOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MistakesViewModel @Inject constructor(
    private val mistakesRepository: MistakesRepository,
    private val lessonRepository: LessonRepository
) : ViewModel() {
    
    private val _mistakes = MutableStateFlow<List<MistakeResponse>>(emptyList())
    val mistakes: StateFlow<List<MistakeResponse>> = _mistakes.asStateFlow()
    
    private val _questionDetails = MutableStateFlow<Map<String, QuestionResponse>>(emptyMap())
    val questionDetails: StateFlow<Map<String, QuestionResponse>> = _questionDetails.asStateFlow()
    
    private val _challenges = MutableStateFlow<Map<String, ChallengeWithOptions>>(emptyMap())
    val challenges: StateFlow<Map<String, ChallengeWithOptions>> = _challenges.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _totalMistakes = MutableStateFlow(0)
    val totalMistakes: StateFlow<Int> = _totalMistakes.asStateFlow()
    
    fun loadMistakes(username: String, token: String? = null, page: Int = 1, itemsPerPage: Int = 50) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            mistakesRepository.getUserMistakes(
                username = username,
                page = page,
                itemsPerPage = itemsPerPage,
                questionType = null,
                token = token
            )
                .onSuccess { response ->
                    _mistakes.value = response.data
                    _totalMistakes.value = response.total
                    
                    // Load question details for each mistake
                    loadQuestionDetailsForMistakes(response.data, token)
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load mistakes"
                }
            
            _isLoading.value = false
        }
    }
    
    private suspend fun loadQuestionDetailsForMistakes(
        mistakes: List<MistakeResponse>,
        token: String?
    ) {
        val questionDetailsMap = mutableMapOf<String, QuestionResponse>()
        val challengesMap = mutableMapOf<String, ChallengeWithOptions>()
        
        // Group mistakes by lesson_id to batch load questions
        val mistakesByLesson = mistakes
            .filter { it.lessonId != null }
            .groupBy { it.lessonId!! }
        
        mistakesByLesson.forEach { (lessonId, lessonMistakes) ->
            // Load all questions from lesson
            val authHeader = if (!token.isNullOrBlank()) "Bearer $token" else null
            lessonRepository.fetchAdditionalLessonChallenges(
                lessonId = lessonId,
                offset = 0,
                limit = 100, // Load enough questions
                token = authHeader
            ).onSuccess { additionalChallenges ->
                // Map question responses by question_id
                additionalChallenges.questionResponses.forEach { (questionId, questionResponse) ->
                    questionDetailsMap[questionId] = questionResponse
                }
                
                // Map challenges by question_id
                additionalChallenges.challenges.forEach { challenge ->
                    challengesMap[challenge.challenge.id] = challenge
                }
            }
        }
        
        _questionDetails.value = questionDetailsMap
        _challenges.value = challengesMap
    }
    
    fun clearError() {
        _error.value = null
    }
}

