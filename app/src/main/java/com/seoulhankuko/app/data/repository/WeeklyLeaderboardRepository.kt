package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.api.model.*
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.data.api.util.ExceptionMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeeklyLeaderboardRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getWeeklyLeaderboard(token: String?): Result<WeeklyLeaderboardResponse> {
        return try {
            val response = apiService.getWeeklyLeaderboard(token)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get weekly leaderboard"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    suspend fun updateXp(exp: Int, token: String?): Result<WeeklyLeaderboardUpdateResponse> {
        return try {
            val response = apiService.updateWeeklyLeaderboardXp(
                token, 
                WeeklyLeaderboardUpdateRequest(exp = exp)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to update XP"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
}

