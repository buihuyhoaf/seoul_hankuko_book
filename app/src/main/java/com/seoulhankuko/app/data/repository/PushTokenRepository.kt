package com.seoulhankuko.app.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.seoulhankuko.app.data.api.model.PushTokenRegisterRequest
import com.seoulhankuko.app.data.api.model.PushTokenUnregisterRequest
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.data.local.UserPreferencesManager
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
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
        Timber.d("🔔 registerCachedToken called with userId=%s", userId)
        
        if (userId.isNullOrBlank()) {
            Timber.w("❌ User ID missing when attempting to register FCM token")
            return Result.failure(IllegalStateException("User ID not available"))
        }

        // First, try to get token from cache
        var token = userPreferencesManager.getFcmToken()
        Timber.d("Checking for FCM token - cached: %s", if (token.isNullOrBlank()) "not found" else "found (length=${token.length})")
        
        // If no cached token, try to get it from Firebase
        if (token.isNullOrBlank()) {
            Timber.d("No cached FCM token found, attempting to get from Firebase")
            token = try {
                val task = FirebaseMessaging.getInstance().token
                Timber.d("Waiting for Firebase token task to complete...")
                
                // Wait for task with timeout (10 seconds)
                val firebaseToken = try {
                    Tasks.await(task, 10, TimeUnit.SECONDS)
                } catch (e: TimeoutException) {
                    Timber.e(e, "Timeout waiting for FCM token from Firebase (10s)")
                    null
                } catch (e: Exception) {
                    Timber.e(e, "Exception waiting for FCM token: %s", e.message)
                    null
                }
                
                if (firebaseToken != null && firebaseToken.isNotBlank()) {
                    Timber.d("✅ Got FCM token from Firebase (length=%d): %s", firebaseToken.length, firebaseToken.take(20) + "...")
                    firebaseToken
                } else {
                    Timber.w("❌ FCM token from Firebase is null or blank")
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Exception getting FCM token from Firebase: %s", e.message)
                null
            }
            
            if (!token.isNullOrBlank()) {
                Timber.d("✅ Got FCM token from Firebase (length=%d), caching it", token.length)
                cacheFcmToken(token)
            } else {
                Timber.e("❌ No FCM token available from Firebase or cache, cannot register")
                Timber.e("   This usually means:")
                Timber.e("   1. Firebase Messaging not initialized properly")
                Timber.e("   2. Google Play Services not available")
                Timber.e("   3. App needs to be restarted to get token")
                Timber.e("   4. Network issue preventing token retrieval")
                return Result.failure(IllegalStateException("FCM token not available"))
            }
        }

        val lastSyncedUser = userPreferencesManager.getFcmTokenSyncedUserId()
        if (lastSyncedUser == userId) {
            Timber.d("FCM token already registered for userId=%s, skip", userId)
            return Result.success(Unit)
        }

        return try {
            Timber.d("Calling API to register FCM token for userId=%s (token length=%d)", userId, token.length)
            val response = apiService.registerPushToken(
                PushTokenRegisterRequest(token = token)
            )

            if (response.isSuccessful) {
                Timber.d("✅ Registered FCM token successfully for userId=%s", userId)
                userPreferencesManager.markFcmTokenSynced(userId)
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e(
                    "❌ Failed to register FCM token. HTTP code=%d, message=%s, body=%s",
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
            Timber.e(exception, "❌ Exception while registering FCM token: %s", exception.message)
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

