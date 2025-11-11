package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.api.model.NotificationResponse
import com.seoulhankuko.app.data.api.model.NotificationUnreadCountResponse
import com.seoulhankuko.app.data.api.model.PaginatedResponse
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.data.api.util.ExceptionMapper
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun getNotifications(
        username: String,
        page: Int,
        itemsPerPage: Int,
        unreadOnly: Boolean = false,
        notificationType: String? = null
    ): Result<PaginatedResponse<NotificationResponse>> {
        return try {
            val response = apiService.getUserNotifications(username, page, itemsPerPage, unreadOnly, notificationType)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(IllegalStateException("Notification response body is null"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.w("Failed to load notifications (HTTP ${response.code()}): $errorBody")
                Result.failure(Exception(errorBody ?: "Failed to load notifications"))
            }
        } catch (throwable: Throwable) {
            Result.failure(ExceptionMapper.mapToAppException(throwable))
        }
    }

    suspend fun getUnreadCount(username: String): Result<NotificationUnreadCountResponse> {
        return try {
            val response = apiService.getUnreadNotificationsCount(username)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(IllegalStateException("Unread count response body is null"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.w("Failed to load unread notification count (HTTP ${response.code()}): $errorBody")
                Result.failure(Exception(errorBody ?: "Failed to load unread count"))
            }
        } catch (throwable: Throwable) {
            Result.failure(ExceptionMapper.mapToAppException(throwable))
        }
    }

    suspend fun markNotificationRead(username: String, notificationId: String): Result<Unit> {
        return try {
            val response = apiService.markNotificationRead(username, notificationId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.w("Failed to mark notification as read (HTTP ${response.code()}): $errorBody")
                Result.failure(Exception(errorBody ?: "Failed to mark notification as read"))
            }
        } catch (throwable: Throwable) {
            Result.failure(ExceptionMapper.mapToAppException(throwable))
        }
    }

    suspend fun markAllNotificationsRead(username: String): Result<Unit> {
        return try {
            val response = apiService.markAllNotificationsRead(username)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.w("Failed to mark all notifications as read (HTTP ${response.code()}): $errorBody")
                Result.failure(Exception(errorBody ?: "Failed to mark all notifications as read"))
            }
        } catch (throwable: Throwable) {
            Result.failure(ExceptionMapper.mapToAppException(throwable))
        }
    }

}


