package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.api.model.PushTokenRegisterRequest
import com.seoulhankuko.app.data.api.model.PushTokenUnregisterRequest
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.data.local.UserPreferencesManager
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushTokenRepository @Inject constructor(
    private val apiService: ApiService,
    private val userPreferencesManager: UserPreferencesManager,
) {

    suspend fun cacheFcmToken(token: String) {
        Timber.d("Caching FCM token (length=%d)", token.length)
        userPreferencesManager.saveFcmToken(token)
    }

    suspend fun getCachedFcmToken(): String? = userPreferencesManager.getFcmToken()

    suspend fun registerCachedToken(userId: String?): Result<Unit> {
        val token = userPreferencesManager.getFcmToken()
        if (token.isNullOrBlank()) {
            Timber.w("No cached FCM token found, skip register")
            return Result.failure(IllegalStateException("FCM token not available"))
        }

        if (userId.isNullOrBlank()) {
            Timber.w("User ID missing when attempting to register FCM token")
            return Result.failure(IllegalStateException("User ID not available"))
        }

        val lastSyncedUser = userPreferencesManager.getFcmTokenSyncedUserId()
        if (lastSyncedUser == userId) {
            Timber.d("FCM token already registered for userId=%s, skip", userId)
            return Result.success(Unit)
        }

        return try {
            val response = apiService.registerPushToken(
                PushTokenRegisterRequest(token = token)
            )

            if (response.isSuccessful) {
                Timber.d("Registered FCM token successfully for userId=%s", userId)
                userPreferencesManager.markFcmTokenSynced(userId)
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.w(
                    "Failed to register FCM token. code=%d, message=%s, body=%s",
                    response.code(),
                    response.message(),
                    errorBody
                )
                Result.failure(
                    IllegalStateException(
                        "Register FCM token failed: ${errorBody ?: response.message()}"
                    )
                )
            }
        } catch (exception: Exception) {
            Timber.e(exception, "Exception while registering FCM token")
            Result.failure(exception)
        }
    }

    suspend fun unregisterCachedToken(): Result<Unit> {
        val token = userPreferencesManager.getFcmToken()
        if (token.isNullOrBlank()) {
            Timber.d("No cached FCM token to unregister, skip")
            return Result.success(Unit)
        }

        return try {
            val response = apiService.unregisterPushToken(
                PushTokenUnregisterRequest(token = token)
            )

            if (response.isSuccessful) {
                Timber.d("Unregistered FCM token successfully")
                userPreferencesManager.clearFcmTokenSyncState()
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.w(
                    "Failed to unregister FCM token. code=%d, message=%s, body=%s",
                    response.code(),
                    response.message(),
                    errorBody
                )
                Result.failure(
                    IllegalStateException(
                        "Unregister FCM token failed: ${errorBody ?: response.message()}"
                    )
                )
            }
        } catch (exception: Exception) {
            Timber.e(exception, "Exception while unregistering FCM token")
            Result.failure(exception)
        }
    }

    suspend fun clearCachedToken() {
        Timber.d("Clearing cached FCM token")
        userPreferencesManager.clearFcmToken()
    }
}

