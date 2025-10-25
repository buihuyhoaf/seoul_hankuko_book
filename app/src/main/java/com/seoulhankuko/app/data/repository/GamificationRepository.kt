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
class GamificationRepository @Inject constructor(
    private val apiService: ApiService
) {
    private val _badges = MutableStateFlow<List<BadgeResponse>>(emptyList())
    val badges: StateFlow<List<BadgeResponse>> = _badges.asStateFlow()
    
    private val _leaderboard = MutableStateFlow<List<LeaderboardEntryResponse>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntryResponse>> = _leaderboard.asStateFlow()
    
    private val _friends = MutableStateFlow<List<FriendResponse>>(emptyList())
    val friends: StateFlow<List<FriendResponse>> = _friends.asStateFlow()
    
    suspend fun getAllBadges(
        page: Int = 1,
        itemsPerPage: Int = 20,
        token: String? = null
    ): Result<PaginatedResponse<BadgeResponse>> {
        return try {
            val response = apiService.getAllBadges(page, itemsPerPage, token)
            
            if (response.isSuccessful) {
                val badgesResponse = response.body()
                if (badgesResponse != null) {
                    _badges.value = badgesResponse.data ?: emptyList()  // ← Thay đổi từ items thành data
                    Result.success(badgesResponse)
                } else {
                    Result.failure(Exception("Badges response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get badges"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    suspend fun getLeaderboard(
        page: Int = 1,
        itemsPerPage: Int = 50,
        season: String? = null,
        token: String? = null
    ): Result<PaginatedResponse<LeaderboardEntryResponse>> {
        return try {
            val response = apiService.getLeaderboard(page, itemsPerPage, season, token)
            
            if (response.isSuccessful) {
                val leaderboardResponse = response.body()
                if (leaderboardResponse != null) {
                    _leaderboard.value = leaderboardResponse.data ?: emptyList()  // ← Thay đổi từ items thành data
                    Result.success(leaderboardResponse)
                } else {
                    Result.failure(Exception("Leaderboard response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get leaderboard"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    suspend fun getUserFriends(
        username: String,
        page: Int = 1,
        itemsPerPage: Int = 20,
        status: String? = null,
        token: String? = null
    ): Result<PaginatedResponse<FriendResponse>> {
        return try {
            val response = apiService.getUserFriends(username, page, itemsPerPage, status, token)
            
            if (response.isSuccessful) {
                val friendsResponse = response.body()
                if (friendsResponse != null) {
                    _friends.value = friendsResponse.data ?: emptyList()  // ← Thay đổi từ items thành data
                    Result.success(friendsResponse)
                } else {
                    Result.failure(Exception("Friends response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get friends"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    fun clearBadges() {
        _badges.value = emptyList()
    }
    
    fun clearLeaderboard() {
        _leaderboard.value = emptyList()
    }
    
    fun clearFriends() {
        _friends.value = emptyList()
    }
}

