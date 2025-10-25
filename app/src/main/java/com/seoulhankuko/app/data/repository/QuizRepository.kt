package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.api.model.*
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.data.api.util.ExceptionMapper
import com.seoulhankuko.app.domain.exception.AppException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepository @Inject constructor(
    private val apiService: ApiService
) {
    private val _currentQuiz = MutableStateFlow<QuizDetailResponse?>(null)
    val currentQuiz: StateFlow<QuizDetailResponse?> = _currentQuiz.asStateFlow()
    
    private val _quizAttempts = MutableStateFlow<List<QuizAttemptResponse>>(emptyList())
    val quizAttempts: StateFlow<List<QuizAttemptResponse>> = _quizAttempts.asStateFlow()
    
    private val _lastQuizResult = MutableStateFlow<QuizAttemptResponse?>(null)
    val lastQuizResult: StateFlow<QuizAttemptResponse?> = _lastQuizResult.asStateFlow()
    
    suspend fun getQuiz(quizId: Int, token: String): Result<QuizDetailResponse> {
        return try {
            val response = apiService.getQuiz(quizId, token)
            
            if (response.isSuccessful) {
                val quizResponse = response.body()
                if (quizResponse != null) {
                    _currentQuiz.value = quizResponse
                    Result.success(quizResponse)
                } else {
                    Result.failure(Exception("Quiz response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get quiz"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    suspend fun submitQuizAttempt(
        quizId: Int, 
        answers: Map<String, String>, 
        token: String
    ): Result<QuizAttemptResponse> {
        return try {
            val response = apiService.submitQuizAttempt(quizId, token, answers)
            
            if (response.isSuccessful) {
                val attemptResponse = response.body()
                if (attemptResponse != null) {
                    _lastQuizResult.value = attemptResponse
                    Result.success(attemptResponse)
                } else {
                    Result.failure(Exception("Quiz attempt response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to submit quiz attempt"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    suspend fun getUserQuizAttempts(
        username: String,
        page: Int = 1,
        itemsPerPage: Int = 10,
        token: String? = null
    ): Result<PaginatedResponse<QuizAttemptResponse>> {
        return try {
            val response = apiService.getUserQuizAttempts(username, page, itemsPerPage, token)
            
            if (response.isSuccessful) {
                val attemptsResponse = response.body()
                if (attemptsResponse != null) {
                    _quizAttempts.value = attemptsResponse.data ?: emptyList()  // ← Thay đổi từ items thành data
                    Result.success(attemptsResponse)
                } else {
                    Result.failure(Exception("Quiz attempts response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get quiz attempts"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    suspend fun getListeningExercise(exerciseId: Int, token: String): Result<ListeningExerciseResponse> {
        return try {
            val response = apiService.getListeningExercise(exerciseId, token)
            
            if (response.isSuccessful) {
                val exerciseResponse = response.body()
                if (exerciseResponse != null) {
                    Result.success(exerciseResponse)
                } else {
                    Result.failure(Exception("Listening exercise response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get listening exercise"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    suspend fun getSpeakingExercise(exerciseId: Int, token: String): Result<SpeakingExerciseResponse> {
        return try {
            val response = apiService.getSpeakingExercise(exerciseId, token)
            
            if (response.isSuccessful) {
                val exerciseResponse = response.body()
                if (exerciseResponse != null) {
                    Result.success(exerciseResponse)
                } else {
                    Result.failure(Exception("Speaking exercise response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get speaking exercise"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    suspend fun getWritingExercise(exerciseId: Int, token: String): Result<WritingExerciseResponse> {
        return try {
            val response = apiService.getWritingExercise(exerciseId, token)
            
            if (response.isSuccessful) {
                val exerciseResponse = response.body()
                if (exerciseResponse != null) {
                    Result.success(exerciseResponse)
                } else {
                    Result.failure(Exception("Writing exercise response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get writing exercise"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    fun clearCurrentQuiz() {
        _currentQuiz.value = null
    }
    
    fun clearLastQuizResult() {
        _lastQuizResult.value = null
    }
}
