package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.api.model.*
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.data.api.util.ExceptionMapper
import com.seoulhankuko.app.data.database.daos.UserProgressDao
import com.seoulhankuko.app.data.database.entities.UserProgress
import com.seoulhankuko.app.domain.exception.AppException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProgressRepository @Inject constructor(
    private val apiService: ApiService,
    private val userProgressDao: UserProgressDao
) {
    private val _userProgress = MutableStateFlow<UserProgressResponse?>(null)
    val userProgress: StateFlow<UserProgressResponse?> = _userProgress.asStateFlow()
    
    private val _courseProgress = MutableStateFlow<List<CourseProgressResponse>>(emptyList())
    val courseProgress: StateFlow<List<CourseProgressResponse>> = _courseProgress.asStateFlow()
    
    private val _expHistory = MutableStateFlow<List<ExpLogResponse>>(emptyList())
    val expHistory: StateFlow<List<ExpLogResponse>> = _expHistory.asStateFlow()
    
    private val _dailyGoals = MutableStateFlow<DailyGoalsResponse?>(null)
    val dailyGoals: StateFlow<DailyGoalsResponse?> = _dailyGoals.asStateFlow()
    
    suspend fun getUserProgress(username: String, token: String? = null): Result<UserProgressResponse> {
        return try {
            val response = apiService.getUserProgress(username, token)
            
            if (response.isSuccessful) {
                val progressResponse = response.body()
                if (progressResponse != null) {
                    _userProgress.value = progressResponse
                    Result.success(progressResponse)
                } else {
                    Result.failure(Exception("User progress response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get user progress"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    suspend fun getUserCourseProgress(
        username: String,
        page: Int = 1,
        itemsPerPage: Int = 10,
        token: String? = null
    ): Result<PaginatedResponse<CourseProgressResponse>> {
        return try {
            val response = apiService.getUserCourseProgress(username, page, itemsPerPage, token)
            
            if (response.isSuccessful) {
                val progressResponse = response.body()
                if (progressResponse != null) {
                    _courseProgress.value = progressResponse.data ?: emptyList()  // ← Thay đổi từ items thành data
                    Result.success(progressResponse)
                } else {
                    Result.failure(Exception("Course progress response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get course progress"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    suspend fun getUserExpHistory(
        username: String,
        page: Int = 1,
        itemsPerPage: Int = 20,
        token: String? = null
    ): Result<PaginatedResponse<ExpLogResponse>> {
        return try {
            val response = apiService.getUserExpHistory(username, page, itemsPerPage, token)
            
            if (response.isSuccessful) {
                val expResponse = response.body()
                if (expResponse != null) {
                    _expHistory.value = expResponse.data ?: emptyList()  // ← Thay đổi từ items thành data
                    Result.success(expResponse)
                } else {
                    Result.failure(Exception("EXP history response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get EXP history"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    suspend fun updateUserStreak(username: String, token: String): Result<StreakUpdateResponse> {
        return try {
            val response = apiService.updateUserStreak(username, token)
            
            if (response.isSuccessful) {
                val streakResponse = response.body()
                if (streakResponse != null) {
                    Result.success(streakResponse)
                } else {
                    Result.failure(Exception("Streak update response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to update streak"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    suspend fun getUserDailyGoals(username: String, token: String? = null): Result<DailyGoalsResponse> {
        return try {
            val response = apiService.getUserDailyGoals(username, token)
            
            if (response.isSuccessful) {
                val goalsResponse = response.body()
                if (goalsResponse != null) {
                    _dailyGoals.value = goalsResponse
                    Result.success(goalsResponse)
                } else {
                    Result.failure(Exception("Daily goals response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get daily goals"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    suspend fun updateUserDailyGoals(
        username: String,
        goalData: Map<String, Int>,
        token: String
    ): Result<DailyGoalsUpdateResponse> {
        return try {
            val response = apiService.updateUserDailyGoals(username, token, goalData)
            
            if (response.isSuccessful) {
                val updateResponse = response.body()
                if (updateResponse != null) {
                    Result.success(updateResponse)
                } else {
                    Result.failure(Exception("Daily goals update response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to update daily goals"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    fun clearUserProgress() {
        _userProgress.value = null
    }
    
    fun clearCourseProgress() {
        _courseProgress.value = emptyList()
    }
    
    fun clearExpHistory() {
        _expHistory.value = emptyList()
    }
    
    fun clearDailyGoals() {
        _dailyGoals.value = null
    }
    
    // Local database methods for offline support
    fun getUserProgress(userId: String): Flow<UserProgress?> {
        return userProgressDao.getUserProgress(userId)
    }
    
    suspend fun updatePoints(userId: String, points: Int) {
        userProgressDao.updatePoints(userId, points)
    }
    
    suspend fun reduceHearts(userId: String): Boolean {
        val userProgress = userProgressDao.getUserProgress(userId).first()
        return if (userProgress != null && userProgress.hearts > 0) {
            val newHearts = userProgress.hearts - 1
            userProgressDao.updateHearts(userId, newHearts)
            true
        } else {
            false
        }
    }
    
    suspend fun insertOrUpdateUserProgress(userProgress: UserProgress) {
        userProgressDao.insertUserProgress(userProgress)
    }
    
    suspend fun initializeUserProgressIfNeeded(userId: String) {
        val existing = userProgressDao.getUserProgress(userId).first()
        if (existing == null) {
            userProgressDao.insertUserProgress(
                UserProgress(
                    userId = userId,
                    userName = "User",
                    hearts = 5,
                    points = 0
                )
            )
        }
    }
}