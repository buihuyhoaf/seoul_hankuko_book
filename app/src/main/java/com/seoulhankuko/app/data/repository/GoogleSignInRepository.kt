package com.seoulhankuko.app.data.repository

import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.seoulhankuko.app.data.api.model.GoogleSignInRequest
import com.seoulhankuko.app.data.api.model.GoogleSignInResponse
import com.seoulhankuko.app.data.api.model.UserInfo
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.data.local.UserPreferencesManager
import com.seoulhankuko.app.core.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Google Sign-In functionality
 */
@Singleton
class GoogleSignInRepository @Inject constructor(
    private val apiService: ApiService,
    private val userPreferencesManager: UserPreferencesManager
) {
    
    /**
     * Get Google Sign-In client with required scopes
     * 
     * For server authentication, we need to use the server client ID from client_secret file
     * The server client ID is: 461063240681-n6ffjuabhskh1q0udhotlldol4k7mdga.apps.googleusercontent.com
     */
    fun getGoogleSignInClient(context: android.content.Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken("461063240681-81vk4ofnni0lru1vdmnmtvlrbd0k9hpt.apps.googleusercontent.com") // Web client ID đúng
            .build()
        
        return GoogleSignIn.getClient(context, gso)
    }
    
    /**
     * Handle Google Sign-In result and authenticate with backend
     */
    suspend fun handleGoogleSignInResult(
        account: GoogleSignInAccount?
    ): Flow<GoogleSignInResult> = flow {
        try {
            emit(GoogleSignInResult.Loading)
            
            if (account == null) {
                emit(GoogleSignInResult.Error("Google Sign-In failed: No account data"))
                return@flow
            }
            
            Logger.GoogleSignIn.signInAttempt(account.email ?: "unknown")
            
            // Lấy Google ID token
            val idToken = account.idToken
            if (idToken == null) {
                emit(GoogleSignInResult.Error("Google Sign-In failed: No ID token received"))
                return@flow
            }
            
            // Gửi request đến backend để xác minh token
            val request = GoogleSignInRequest(token = idToken)
            Logger.GoogleSignIn.signInAttempt("Sending request to backend: ${request.token.take(50)}...")
            val response = apiService.signInWithGoogle(request)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    // Handle both response types (GoogleSignInResponse and Map<String, String>)
                    val token = when (responseBody) {
                        is GoogleSignInResponse -> responseBody.token
                        is Map<*, *> -> responseBody["token"] as? String
                        else -> null
                    }
                    
                    if (token != null) {
                        // Lưu JWT token từ backend vào local storage
                        userPreferencesManager.saveUserData(
                            userId = account.id ?: "",
                            email = account.email ?: "",
                            name = account.displayName ?: "",
                            avatarUrl = account.photoUrl?.toString(),
                            accessToken = token, // JWT token thật từ backend
                            refreshToken = "", // Backend không trả về refresh token
                            isPremium = false
                        )
                        
                        val userInfo = UserInfo(
                            email = account.email ?: "",
                            name = account.displayName ?: "",
                            avatarUrl = account.photoUrl?.toString(),
                            id = account.id ?: ""
                        )
                        
                        Logger.GoogleSignIn.signInSuccess(userInfo.email)
                        emit(GoogleSignInResult.Success(userInfo))
                    } else {
                        emit(GoogleSignInResult.Error("Backend authentication failed: No token in response"))
                    }
                } else {
                    emit(GoogleSignInResult.Error("Backend authentication failed: Empty response"))
                }
            } else {
                val errorMessage = "Backend authentication failed: ${response.code()} ${response.message()}"
                Logger.GoogleSignIn.signInError(errorMessage)
                emit(GoogleSignInResult.Error(errorMessage))
            }
            
        } catch (e: ApiException) {
            val errorMessage = "Google Sign-In API error: ${e.statusCode}"
            Logger.GoogleSignIn.signInError(errorMessage)
            emit(GoogleSignInResult.Error(errorMessage))
        } catch (e: Exception) {
            val errorMessage = "Unexpected error: ${e.message}"
            Logger.GoogleSignIn.signInError(errorMessage)
            emit(GoogleSignInResult.Error(errorMessage))
        }
    }
    
    /**
     * Sign out from Google and clear local data
     */
    suspend fun signOut(googleSignInClient: GoogleSignInClient) {
        try {
            // Get current access token before clearing data
            val currentToken = userPreferencesManager.getCurrentAccessToken()
            
            // Call logout API to update backend status before clearing local data
            currentToken?.let { token ->
                try {
                    Logger.GoogleSignIn.logoutApiCallStarted()
                    val response = apiService.logout("Bearer $token")
                    
                    if (response.isSuccessful) {
                        Logger.GoogleSignIn.logoutApiCallSuccess()
                    } else {
                        Logger.GoogleSignIn.logoutApiCallError("API logout failed: ${response.code()}")
                    }
                } catch (apiException: Exception) {
                    Logger.GoogleSignIn.logoutApiCallError("API logout error: ${apiException.message}")
                    // Continue with local logout even if API call fails
                }
            }
            
            // Sign out from Google
            googleSignInClient.signOut()
            
            // Clear local user data (bao gồm JWT token)
            userPreferencesManager.clearUserData()
            
            Logger.GoogleSignIn.signOutSuccess()
        } catch (e: Exception) {
            Logger.GoogleSignIn.signOutError(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Get current user data
     */
    val userData: Flow<UserData> = userPreferencesManager.userData
    
    /**
     * Check if user is logged in
     */
    val isLoggedIn: Flow<Boolean> = userPreferencesManager.isLoggedIn
}

/**
 * Google Sign-In result states
 */
sealed class GoogleSignInResult {
    object Loading : GoogleSignInResult()
    data class Success(val userInfo: UserInfo) : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
}

/**
 * Import UserData from UserPreferencesManager
 */
typealias UserData = com.seoulhankuko.app.data.local.UserData