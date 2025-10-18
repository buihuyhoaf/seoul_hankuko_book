package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.api.model.*
import com.seoulhankuko.app.data.repository.EntryTestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EntryTestUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val questions: List<EntryTestQuestionResponse> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<Int, Int?> = emptyMap(), // questionId -> selectedOptionId
    val isCompleted: Boolean = false,
    val submissionResult: EntryTestSubmissionResponse? = null
)

@HiltViewModel
class EntryTestViewModel @Inject constructor(
    private val entryTestRepository: EntryTestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntryTestUiState())
    val uiState: StateFlow<EntryTestUiState> = _uiState.asStateFlow()

    private val _questionsMap = mutableMapOf<Int, EntryTestQuestionResponse>()
    private val _selectedAnswersMap = mutableMapOf<Int, Int?>() // questionId -> optionId

    fun loadEntryTestQuestions(allowOffline: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val response = entryTestRepository.getEntryTestQuestions()
                if (response.isSuccessful) {
                    val entryTest = response.body()
                    if (entryTest != null && entryTest.questions.isNotEmpty()) {
                        // Populate questions map
                        _questionsMap.clear()
                        entryTest.questions.forEach { question ->
                            _questionsMap[question.id] = question
                        }
                        
                        // Initialize selected answers map with question IDs as keys
                        _selectedAnswersMap.clear()
                        entryTest.questions.forEach { question ->
                            _selectedAnswersMap[question.id] = null
                        }
                        
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                questions = entryTest.questions,
                                selectedAnswers = _selectedAnswersMap.toMap(),
                                currentQuestionIndex = 0,
                                isCompleted = false,
                                submissionResult = null,
                                error = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "No questions available",
                                questions = emptyList()
                            )
                        }
                    }
                } else {
                    if (allowOffline && response.code() == 401) {
                        // User not authenticated but offline mode allowed
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Please login to load questions, or contact support for offline access",
                                questions = emptyList()
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to load questions: ${response.code()}",
                                questions = emptyList()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                if (allowOffline) {
                    // For offline mode, show a generic error but don't block the flow completely
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Unable to load questions. Please check your connection.",
                            questions = emptyList()
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Unknown error occurred",
                            questions = emptyList()
                        )
                    }
                }
            }
        }
    }

    fun selectAnswer(questionId: Int, optionId: Int) {
        _selectedAnswersMap[questionId] = optionId
        
        _uiState.update { currentState ->
            currentState.copy(
                selectedAnswers = _selectedAnswersMap.toMap()
            )
        }
    }

    fun nextQuestion() {
        _uiState.update { currentState ->
            val currentIndex = currentState.currentQuestionIndex
            val totalQuestions = currentState.questions.size
            if (currentIndex < totalQuestions - 1) {
                currentState.copy(currentQuestionIndex = currentIndex + 1)
            } else {
                currentState
            }
        }
    }

    fun previousQuestion() {
        _uiState.update { currentState ->
            val currentIndex = currentState.currentQuestionIndex
            if (currentIndex > 0) {
                currentState.copy(currentQuestionIndex = currentIndex - 1)
            } else {
                currentState
            }
        }
    }

    fun submitTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Prepare submission request using question IDs as keys
                val answers = mutableListOf<EntryTestAnswerRequest>()
                
                _uiState.value.selectedAnswers.forEach { (questionId, selectedOptionId) ->
                    if (selectedOptionId != null) {
                        answers.add(
                            EntryTestAnswerRequest(
                                questionId = questionId,
                                selectedOptionId = selectedOptionId
                            )
                        )
                    }
                }
                
                // Check if all questions are answered
                val allQuestionsAnswered = _uiState.value.questions.all { question ->
                    _uiState.value.selectedAnswers[question.id] != null
                }
                
                if (!allQuestionsAnswered || answers.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Please answer all questions before submitting"
                        )
                    }
                    return@launch
                }
                
                val submissionRequest = EntryTestSubmissionRequest(answers = answers)
                val response = entryTestRepository.submitEntryTest(submissionRequest)
                
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isCompleted = true,
                                submissionResult = result
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to get test results"
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to submit test: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }
}
