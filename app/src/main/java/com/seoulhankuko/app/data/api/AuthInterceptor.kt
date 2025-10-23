package com.seoulhankuko.app.data.api

import android.content.Context
import android.content.SharedPreferences
import com.seoulhankuko.app.BuildConfig
import com.seoulhankuko.app.data.api.model.RefreshTokenRequest
import com.seoulhankuko.app.data.api.model.RefreshTokenResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.Gson
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Singleton
class AuthInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    }
    
    private val accountPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
    }

    // Lock to prevent concurrent refresh attempts
    private val refreshLock = ReentrantLock()
    @Volatile
    private var isRefreshing = false
    
    // Gson for JSON serialization
    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip token addition for login, refresh, and other auth-related endpoints
        if (shouldSkipAuth(originalRequest.url.encodedPath)) {
            return chain.proceed(originalRequest)
        }

        // Check if Authorization header is already present (manually added by repositories)
        val hasAuthHeader = originalRequest.header("Authorization") != null
            
        // Only add token automatically if no Authorization header is present
        val requestWithAuth = if (!hasAuthHeader) {
            val currentToken = sharedPreferences.getString("auth_token", null)
            if (currentToken != null && !currentToken.isBlank()) {
                originalRequest.newBuilder()
                    .addHeader("Authorization", "Bearer $currentToken")
                    .build()
            } else {
                originalRequest
            }
        } else {
            originalRequest
        }

        // Execute the request
        val response = chain.proceed(requestWithAuth)

        // Handle 401 responses (token expiration) - but only retry once
        if (response.code == 401 && 
            !shouldSkipAuth(originalRequest.url.encodedPath) &&
            originalRequest.header("X-Retry-Attempt") == null) {
            
            Timber.d("Received 401 response, attempting token refresh")
            response.close() // Close the response to free resources
            
            // Check if we're already refreshing to avoid concurrent attempts
            return refreshLock.withLock {
                if (isRefreshing) {
                    Timber.d("Token refresh already in progress, waiting...")
                    // Wait for ongoing refresh to complete
                    Thread.sleep(1000) // Simple wait, in production you might want better synchronization
                    // Retry with current token
                    return@withLock chain.proceed(requestWithAuth)
                }
                
                isRefreshing = true
                try {
                    val refreshResult = refreshTokenAndRetry(chain, originalRequest)
                    // If refresh returned 401, it means refresh failed
                    if (refreshResult.code == 401) {
                        Timber.w("Token refresh failed, returning 401 response")
                        return@withLock refreshResult
                    }
                    return@withLock refreshResult
                } catch (e: Exception) {
                    Timber.e(e, "Exception during token refresh")
                    // Return 401 response instead of throwing exception
                    return@withLock createUnauthorizedResponse(originalRequest, "Token refresh failed: ${e.message}")
                } finally {
                    isRefreshing = false
                }
            }
        }

        return response
    }

    private fun refreshTokenAndRetry(chain: Interceptor.Chain, originalRequest: okhttp3.Request): Response {
        return try {
            // Get refresh token
            val refreshToken = getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                Timber.w("No refresh token available")
                // Return 401 response instead of throwing exception
                return createUnauthorizedResponse(originalRequest, "No refresh token available")
            }

            // Attempt to refresh the token synchronously
            val refreshResponse = attemptTokenRefresh(refreshToken)
            
            if (refreshResponse != null) {
                // Update stored tokens
                updateStoredTokens(refreshResponse.accessToken, refreshResponse.refreshToken)
                Timber.d("Token refreshed successfully")
                
                // Retry the original request with new token
                val newToken = refreshResponse.accessToken
                val retryRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .header("X-Retry-Attempt", "true") // Mark as retry to prevent infinite loops
                    .build()
                
                chain.proceed(retryRequest)
            } else {
                // Refresh failed, clear tokens and return 401 response
                Timber.w("Token refresh failed, clearing tokens")
                clearTokens()
                return createUnauthorizedResponse(originalRequest, "Token refresh failed")
            }
        } catch (e: Exception) {
            Timber.e(e, "Token refresh failed: ${e.message}")
            // Clear tokens and return 401 response instead of throwing exception
            clearTokens()
            return createUnauthorizedResponse(originalRequest, "Token refresh failed: ${e.message}")
        }
    }

    private fun attemptTokenRefresh(refreshToken: String): RefreshTokenResponse? {
        return try {
            Timber.d("Attempting token refresh with refresh token: ${refreshToken.take(20)}...")
            
            // Create a simple OkHttpClient for the refresh call (without this interceptor to avoid loops)
            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            // Create request body with refresh token
            val requestBody = gson.toJson(RefreshTokenRequest(refreshToken))
                .toRequestBody("application/json".toMediaType())
            
            // Build refresh request
            val refreshRequest = okhttp3.Request.Builder()
                .url("${BuildConfig.BASE_URL}v1/refresh")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()
            
            Timber.d("Making refresh token request to: ${refreshRequest.url}")
            
            // Execute refresh request
            val response = client.newCall(refreshRequest).execute()
            
            Timber.d("Refresh token response - Code: ${response.code}, Success: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    Timber.d("Refresh token response body: ${responseBody.take(100)}...")
                    gson.fromJson(responseBody, RefreshTokenResponse::class.java)
                } else {
                    Timber.w("Refresh token response body is null")
                    null
                }
            } else {
                Timber.w("Token refresh failed with code: ${response.code}")
                val errorBody = response.body?.string()
                Timber.w("Error response: $errorBody")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during token refresh: ${e.message}")
            null
        }
    }

    private fun getRefreshToken(): String? {
        // Try to get refresh token from multiple sources
        return try {
            // First try from shared preferences (legacy storage)
            val tokenFromPrefs = sharedPreferences.getString("refresh_token", null)
            if (!tokenFromPrefs.isNullOrBlank()) {
                return tokenFromPrefs
            }
            
            // Try to get from AccountRepository storage
            // This is a simplified approach - in a real implementation you might need
            // to access the database directly or use a different mechanism
            getRefreshTokenFromActiveAccount()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get refresh token")
            null
        }
    }
    
    private fun getRefreshTokenFromActiveAccount(): String? {
        // This is a simplified approach. In a real implementation, you'd need to:
        // 1. Access the database directly to get the active account
        // 2. Or use a different storage mechanism
        // For now, we'll use a simple approach
        
        // Try to get from a separate refresh token preference
        return sharedPreferences.getString("active_refresh_token", null)
    }

    private fun updateStoredTokens(accessToken: String, refreshToken: String) {
        sharedPreferences.edit()
            .putString("auth_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putString("active_refresh_token", refreshToken)
            .apply()
    }

    private fun clearTokens() {
        sharedPreferences.edit()
            .remove("auth_token")
            .remove("refresh_token")
            .remove("active_refresh_token")
            .apply()
    }

    private fun createUnauthorizedResponse(originalRequest: okhttp3.Request, message: String): Response {
        return Response.Builder()
            .request(originalRequest)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body(
                """{"error": "$message", "code": 401}""".toResponseBody("application/json".toMediaType())
            )
            .build()
    }

    private fun shouldSkipAuth(path: String): Boolean {
        val skipPaths = listOf(
            "/api/v1/login",
            "/api/v1/refresh", 
            "/api/v1/auth/google",
            "/api/v1/user" // User creation doesn't need auth
        )
        
        return skipPaths.any { skipPath -> 
            path.contains(skipPath)
        }
    }
}