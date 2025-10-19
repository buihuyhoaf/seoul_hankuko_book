package com.seoulhankuko.app.data.repository

import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.seoulhankuko.app.data.api.model.GoogleSignInRequest
import com.seoulhankuko.app.data.api.model.GoogleSignInResponse
import com.seoulhankuko.app.data.api.model.LogoutRequest
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
    private val userPreferencesManager: UserPreferencesManager,
    private val entryTestRepository: EntryTestRepository,
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository
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
                    // Handle GoogleSignInResponse
                    if (responseBody is GoogleSignInResponse) {
                        val accessToken = responseBody.token
                        val refreshToken = responseBody.refreshToken
                        
                        if (accessToken != null) {
                            // Log if refresh token is missing (this could cause auto-login issues)
                            if (refreshToken.isNullOrBlank()) {
                                Logger.GoogleSignIn.signInError("WARNING: No refresh token received from backend for Google Sign-In. Auto-login will not work for this account.")
                            }
                            
                            // Lấy user data từ backend để có đúng userId
                            var backendUserId = account.id ?: "" // Fallback to Google ID
                            try {
                                val userResponse = apiService.getCurrentUser("Bearer $accessToken")
                                if (userResponse.isSuccessful && userResponse.body() != null) {
                                    val userData = userResponse.body()!!
                                    backendUserId = userData.id.toString() // Use backend user ID
                                    
                                    userPreferencesManager.saveEntryTestResult(
                                        hasCompletedEntryTest = userData.hasCompletedEntryTest,
                                        currentCourseId = userData.currentCourseId,
                                        currentCourseName = null,
                                        entryTestScore = userData.entryTestScore
                                    )
                                    Logger.GoogleSignIn.signInSuccess("User data synced from backend")
                                }
                            } catch (e: Exception) {
                                Logger.GoogleSignIn.signInError("Failed to sync user data: ${e.message}")
                                // Continue with login even if sync fails
                            }
                            
                            // Lưu JWT token từ backend vào local storage
                            userPreferencesManager.saveUserData(
                                userId = backendUserId,
                                email = account.email ?: "",
                                name = account.displayName ?: "",
                                avatarUrl = account.photoUrl?.toString(),
                                accessToken = accessToken, // JWT token thật từ backend
                                refreshToken = refreshToken ?: "",
                                isPremium = false
                            )
                            
                            // Save logged account information for future auto-login với đúng userId từ backend
                            authRepository.saveLoggedAccount(
                                userId = backendUserId, // Sử dụng backend user ID, không phải Google ID
                                email = account.email ?: "",
                                name = account.displayName ?: "",
                                avatarUrl = account.photoUrl?.toString(),
                                refreshToken = refreshToken,
                                accessToken = accessToken
                            )
                        
                        // Sync offline entry test result if any
                        try {
                            val syncSuccess = entryTestRepository.syncOfflineEntryTestToServer()
                            if (syncSuccess) {
                                Logger.GoogleSignIn.signInSuccess("Offline entry test synced to server")
                            }
                        } catch (e: Exception) {
                            Logger.GoogleSignIn.signInError("Failed to sync offline entry test: ${e.message}")
                            // Continue with login even if sync fails
                        }
                        
                        val userInfo = UserInfo(
                            email = account.email ?: "",
                            name = account.displayName ?: "",
                            avatarUrl = account.photoUrl?.toString(),
                            id = account.id ?: ""
                        )
                        
                        Logger.GoogleSignIn.signInSuccess(userInfo.email)
                        emit(GoogleSignInResult.Success(userInfo))
                    } else {
                        emit(GoogleSignInResult.Error("Backend authentication failed: No access token in response"))
                    }
                    } else {
                        emit(GoogleSignInResult.Error("Backend authentication failed: Invalid response format"))
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
                    
                    // Get current active account to retrieve refresh token for logout request
                    val currentAccount = accountRepository.getActiveAccount()
                    val logoutRequest = LogoutRequest(
                        refreshToken = currentAccount?.refreshToken
                    )
                    
                    val response = apiService.logout("Bearer $token", logoutRequest)
                    
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
        } catch (e: Exception) {
            Logger.GoogleSignIn.logoutApiCallError("Unexpected error during logout API call: ${e.message}")
        } finally {
            // Always perform local cleanup, regardless of API call success/failure
            try {
                // Sign out from Google
                googleSignInClient.signOut()
                
                // Clear access token from the active account but keep refresh token for auto-login
                accountRepository.getActiveAccount()?.let { activeAccount ->
                    accountRepository.clearAccessTokenOnly(activeAccount.email)
                }
                
                // Clear local user data (bao gồm JWT token)
                userPreferencesManager.clearUserData()
                
                Logger.GoogleSignIn.signOutSuccess()
            } catch (cleanupException: Exception) {
                Logger.GoogleSignIn.signOutError("Error during cleanup: ${cleanupException.message}")
            }
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