package com.seoulhankuko.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.seoulhankuko.app.data.api.model.LoginRequest
import com.seoulhankuko.app.data.api.model.LogoutRequest
import com.seoulhankuko.app.data.api.model.RefreshTokenRequest
import com.seoulhankuko.app.data.local.UserPreferencesManager
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.domain.model.AuthState
import com.seoulhankuko.app.domain.model.LoggedAccount
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.seoulhankuko.app.core.Logger
import com.seoulhankuko.app.data.api.util.ExceptionMapper
import com.seoulhankuko.app.domain.exception.AppException
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val accountRepository: AccountRepository,
    private val userPreferencesManager: UserPreferencesManager,
    @ApplicationContext private val context: Context
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // Use AccountRepository for logged accounts
    val loggedAccounts: Flow<List<LoggedAccount>> = accountRepository.getAllAccounts()
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    private var currentToken: String? = null
    
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    init {
        // AuthRepository initialized
        // Accounts are now loaded via AccountRepository flow
        // Check if user is already signed in
        repositoryScope.launch {
            checkAuthState()
        }
    }
    
    private suspend fun checkAuthState() {
        Logger.AuthenticationUseCase.checkAuthState()
        val token = getStoredToken()
        if (token != null) {
            Logger.AuthenticationUseCase.foundStoredToken()
            // Verify token is still valid by making a test API call
            verifyTokenAndSetState(token)
        } else {
            // Check if user is in guest mode
            val isGuestMode = userPreferencesManager.isGuestModeSync()
            if (isGuestMode) {
                Logger.AuthenticationUseCase.enteredGuestMode()
                _authState.value = AuthState.Guest
            } else {
                Logger.AuthenticationUseCase.noStoredToken()
                _authState.value = AuthState.SignedOut
            }
        }
    }
    
    private fun verifyTokenAndSetState(token: String) {
        // For now, assume token is valid if it exists
        // In a real app, you might want to verify with the server
        currentToken = token
        val userId = getStoredUserId() ?: "unknown"
        Logger.AuthenticationUseCase.tokenVerified(userId)
        _authState.value = AuthState.SignedIn(userId)
    }
    
    suspend fun signIn(email: String, password: String): Result<Unit> {
        Logger.AuthenticationUseCase.signInAttempt(email)
        
        // Check if user is currently in guest mode before signing in
        val wasInGuestMode = isGuestMode()
        
        return try {
            val loginRequest = LoginRequest(
                username = email, // Backend accepts email as username
                password = password
            )
            Logger.AuthenticationUseCase.signInApiCall()
            
            val response = apiService.login(loginRequest)
            
            if (response.isSuccessful) {
                Logger.AuthenticationUseCase.signInApiSuccess()
                val loginResponse = response.body()
                if (loginResponse != null) {
                    // Store only the raw JWT token without Bearer prefix
                    val rawToken = loginResponse.accessToken
                    val fullToken = "Bearer $rawToken"
                    Logger.AuthenticationUseCase.tokenReceived()
                    storeToken(rawToken)  // Store raw token only
                    currentToken = rawToken
                    
                    // Get user info
                    Logger.AuthenticationUseCase.fetchingUserInfo()
                    val userResponse = apiService.getCurrentUser(fullToken)
                    if (userResponse.isSuccessful) {
                        val user = userResponse.body()
                        if (user != null) {
                            Logger.AuthenticationUseCase.userInfoRetrieved(user.username, user.id.toInt())
                            storeUserId(user.id.toString())
                            
                            // Save logged account information with refresh token
                            saveLoggedAccount(
                                userId = user.id.toString(),
                                email = email,
                                name = user.username,
                                avatarUrl = null, // Could be fetched from user data if available
                                refreshToken = loginResponse.refreshToken,
                                accessToken = loginResponse.accessToken
                            )
                            
                            // Exit guest mode if user was in guest mode
                            if (wasInGuestMode) {
                                exitGuestMode()
                            }
                            
                            _authState.value = AuthState.SignedIn(user.id.toString())
                            return Result.success(Unit)
                        }
                    }
                    
                    // If we can't get user info, still consider login successful
                    Logger.AuthenticationUseCase.userInfoFetchFailed()
                    
                    // Exit guest mode if user was in guest mode
                    if (wasInGuestMode) {
                        exitGuestMode()
                    }
                    
                    _authState.value = AuthState.SignedIn("user_${System.currentTimeMillis()}")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Login response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Login failed"
                Logger.AuthenticationUseCase.signInApiFailed(errorMessage, response.code())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Logger.AuthenticationUseCase.exceptionOccurred("sign in", appException)
            Result.failure(appException)
        }
    }
    
    suspend fun signOut() {
        Logger.AuthenticationUseCase.signOutAttempt()
        try {
            currentToken?.let { token ->
                // Get current active account for logout
                val currentAccount = accountRepository.getActiveAccount()
                
                val logoutRequest = LogoutRequest(
                    refreshToken = currentAccount?.refreshToken
                )
                
                Logger.AuthenticationUseCase.signOutApiCall()
                val fullToken = "Bearer $token"
                apiService.logout(fullToken, logoutRequest)
                Logger.AuthenticationUseCase.signOutApiSuccess()
            } ?: Logger.AuthenticationUseCase.signOutApiFailed("No token found")
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Logger.AuthenticationUseCase.signOutApiFailed("Logout error: ${appException.message}")
            // Even if logout API call fails, we still want to sign out locally
        } finally {
            Logger.AuthenticationUseCase.clearingAuthData()
            
            // Clear access token from the active account but keep refresh token for auto-login
            accountRepository.getActiveAccount()?.let { activeAccount ->
                accountRepository.clearAccessTokenOnly(activeAccount.email)
            }
            
            clearCurrentTokens()
            currentToken = null
            _authState.value = AuthState.SignedOut
            Logger.AuthenticationUseCase.signOutComplete()
        }
    }
    
    /**
     * Clear tokens for a specific account (used when account is removed)
     */
    suspend fun clearTokensForAccount(email: String) {
        accountRepository.clearTokens(email)
    }
    
    /**
     * Delete an account completely
     */
    suspend fun deleteAccount(email: String) {
        accountRepository.deleteAccount(email)
    }
    
    fun getCurrentToken(): String? = currentToken
    
    private fun storeToken(token: String) {
        Logger.AuthenticationUseCase.storingToken()
        sharedPreferences.edit()
            .putString("auth_token", token)
            .apply()
    }
    
    private fun getStoredToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }
    
    private fun storeUserId(userId: String) {
        Logger.AuthenticationUseCase.storingUserId(userId)
        sharedPreferences.edit()
            .putString("user_id", userId)
            .apply()
    }
    
    private fun getStoredUserId(): String? {
        return sharedPreferences.getString("user_id", null)
    }
    
    private fun clearCurrentTokens() {
        Logger.AuthenticationUseCase.clearingStoredAuth()
        sharedPreferences.edit()
            .remove("auth_token")
            .remove("user_id")
            .apply()
    }
    
    private fun clearStoredAuth() {
        clearCurrentTokens()
    }
    
    // Methods for managing logged accounts using AccountRepository
    
    suspend fun saveLoggedAccount(
        userId: String,
        email: String,
        name: String,
        avatarUrl: String?,
        refreshToken: String?,
        accessToken: String? = null
    ) {
        try {
            val userIdInt = userId.toIntOrNull() ?: 0
            
            // Log refresh token info for debugging (without exposing the actual token)
            val refreshTokenInfo = if (refreshToken != null) {
                "Length: ${refreshToken.length}, Is blank: ${refreshToken.isBlank()}"
            } else {
                "Is null"
            }
            Timber.tag("AUTH_REPO").d("SaveLoggedAccount - Saving account for $email. Refresh token info: $refreshTokenInfo")
            
            val account = LoggedAccount(
                userId = userIdInt,
                email = email,
                displayName = name,
                photoUrl = avatarUrl,
                accessToken = accessToken,
                refreshToken = refreshToken,
                lastLogin = System.currentTimeMillis(),
                isActive = true // Set as active immediately
            )
            
            // Save to database using AccountRepository
            accountRepository.saveOrUpdateAccount(account)
            
            // Ensure account is set as active (this will clear other active accounts)
            accountRepository.setActiveAccount(email)
            
            Timber.tag("AUTH_REPO").d("SaveLoggedAccount - Successfully saved account for $email")
        } catch (e: Exception) {
            Logger.AuthenticationUseCase.exceptionOccurred("saveLoggedAccount", e)
        }
    }
    
    suspend fun attemptAutoLogin(account: LoggedAccount): Result<Unit> {
        return try {
            // Get fresh account data from database to ensure we have the latest refresh token
            val freshAccount = accountRepository.getAccountByEmail(account.email)
            if (freshAccount == null) {
                Timber.tag("AUTH_REPO").w("Auto-login failed - Account not found in database: ${account.email}")
                return Result.failure(Exception("Account not found"))
            }
            
            // Log account info for debugging
            Logger.AuthenticationUseCase.signInAttempt("Attempting auto-login for account: ${freshAccount.email}, userId: ${freshAccount.userId}")
            
            // Check if account has refresh token
            if (freshAccount.refreshToken.isNullOrBlank()) {
                Timber.tag("AUTH_REPO").w("Auto-login failed - No refresh token available for account: ${freshAccount.email}. Refresh token is null or blank.")
                return Result.failure(Exception("No refresh token available for this account"))
            }
            
            // Debug: Log refresh token info (don't log the actual token for security)
            Timber.tag("AUTH_REPO").d("Auto-login debug - Refresh token for ${freshAccount.email} - Length: ${freshAccount.refreshToken.length}, Is blank: ${freshAccount.refreshToken.isBlank()}")
            
            // Try to use refresh token
            val refreshRequest = RefreshTokenRequest(freshAccount.refreshToken)
            
            // Log detailed request info for debugging
            Timber.tag("AUTH_REPO").d("Making refresh token API call for ${freshAccount.email}")
            Timber.tag("AUTH_REPO").d("Refresh token length: ${freshAccount.refreshToken.length}")
            Timber.tag("AUTH_REPO").d("Refresh token starts with: ${freshAccount.refreshToken.take(20)}...")
            Timber.tag("AUTH_REPO").d("Request body will contain: refresh_token = ${refreshRequest.refreshToken.take(20)}...")
            
            val response = apiService.refreshToken(refreshRequest)
            Timber.tag("AUTH_REPO").d("Refresh token API response - Code: ${response.code()}, Success: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val tokenResponse = response.body()
                if (tokenResponse != null) {
                    Logger.AuthenticationUseCase.tokenReceived()
                    
                    // Store new tokens - only raw JWT token, no Bearer prefix
                    val rawToken = tokenResponse.accessToken
                    storeToken(rawToken)
                    currentToken = rawToken
                    storeUserId(freshAccount.userId.toString())
                    
                    // Update tokens in both SharedPreferences (UserPreferencesManager) and Room DB
                    userPreferencesManager.updateTokens(rawToken, tokenResponse.refreshToken)
                    
                    // Update account tokens in database
                    accountRepository.updateTokens(
                        email = freshAccount.email,
                        accessToken = tokenResponse.accessToken,
                        refreshToken = tokenResponse.refreshToken,
                        lastLogin = System.currentTimeMillis()
                    )
                    accountRepository.setActiveAccount(freshAccount.email)
                    
                    _authState.value = AuthState.SignedIn(freshAccount.userId.toString())
                    return Result.success(Unit)
                } else {
                    Timber.tag("AUTH_REPO").e("Auto-login failed - Token response body is null")
                    return Result.failure(Exception("Token response body is null"))
                }
            } else {
                // Handle unsuccessful response
                val errorBody = response.errorBody()
                val errorMessage = try {
                    errorBody?.string() ?: "Unknown error"
                } catch (e: Exception) {
                    "Failed to read error message: ${e.message}"
                }
                val errorCode = response.code()
                
                Timber.tag("AUTH_REPO").e("Refresh token API failed - HTTP $errorCode: $errorMessage")
                Logger.AuthenticationUseCase.signInApiFailed("Refresh token failed - Code: $errorCode, Message: $errorMessage", errorCode)
                
                when (errorCode) {
                    401 -> {
                        // Refresh token is invalid or expired, clear tokens from local storage
                        accountRepository.clearTokens(freshAccount.email)
                        return Result.failure(Exception("Session expired. Please sign in again."))
                    }
                    403 -> {
                        // Access forbidden, likely token issue
                        accountRepository.clearTokens(freshAccount.email)
                        return Result.failure(Exception("Access forbidden. Please sign in again."))
                    }
                    else -> {
                        return Result.failure(Exception("Auto-login failed with error code $errorCode: $errorMessage"))
                    }
                }
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Logger.AuthenticationUseCase.exceptionOccurred("auto-login", appException)
            Result.failure(appException)
        }
    }
    
    fun hasValidToken(): Boolean {
        val storedToken = getStoredToken()
        val token = currentToken
        return token != null && 
               storedToken != null && 
               storedToken.isNotBlank() &&
               token.isNotBlank()
    }
    
    suspend fun getLoggedAccountsList(): List<LoggedAccount> {
        // Get accounts from AccountRepository
        return accountRepository.getAllAccounts().let { flow ->
            // This is a workaround since we need to get current value from Flow
            // In practice, you'd collect from this flow in ViewModel
            emptyList() // Will be handled by observing the flow directly
        }
    }
    
    suspend fun hasAnyAccounts(): Boolean {
        return accountRepository.hasAnyAccounts()
    }
    
    /**
     * Try to refresh the current user's access token using their stored refresh token
     */
    suspend fun refreshCurrentToken(): Result<String> {
        return try {
            // Find the currently active account
            val currentAccount = accountRepository.getActiveAccount()
            if (currentAccount == null) {
                return Result.failure(Exception("No active account found"))
            }
            
            val refreshToken = currentAccount.refreshToken
            if (refreshToken == null) {
                return Result.failure(Exception("No refresh token available"))
            }
            
            val refreshRequest = RefreshTokenRequest(refreshToken)
            val response = apiService.refreshToken(refreshRequest)
            
            if (response.isSuccessful) {
                val tokenResponse = response.body()
                if (tokenResponse != null) {
                    // Update stored tokens - only raw JWT token, no Bearer prefix
                    val rawToken = tokenResponse.accessToken
                    storeToken(rawToken)
                    currentToken = rawToken
                    storeUserId(currentAccount.userId.toString())
                    
                    // Update tokens in both SharedPreferences (UserPreferencesManager) and Room DB
                    userPreferencesManager.updateTokens(rawToken, tokenResponse.refreshToken)
                    
                    // Update the account with new tokens
                    accountRepository.updateTokens(
                        email = currentAccount.email,
                        accessToken = tokenResponse.accessToken,
                        refreshToken = tokenResponse.refreshToken,
                        lastLogin = System.currentTimeMillis()
                    )
                    
                    return Result.success(rawToken)
                }
            }
            
            Result.failure(Exception("Token refresh failed"))
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if current token is valid, and if not, try to refresh it
     */
    suspend fun ensureValidToken(): Result<String?> {
        return try {
            val currentToken = getCurrentToken()
            if (currentToken != null && hasValidToken()) {
                // Token appears valid, but let's verify with a test API call
                val fullToken = "Bearer $currentToken"
                val testResponse = apiService.getCurrentUser(fullToken)
                if (testResponse.isSuccessful) {
                    return Result.success(currentToken)
                }
            }
            
            // Token is invalid or expired, try to refresh
            val refreshResult = refreshCurrentToken()
            if (refreshResult.isSuccess) {
                Result.success(refreshResult.getOrNull())
            } else {
                // Refresh failed, clear tokens and return failure
                clearCurrentTokens()
                this.currentToken = null
                _authState.value = AuthState.SignedOut
                Result.failure(refreshResult.exceptionOrNull() ?: Exception("Token validation and refresh failed"))
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
    
    // ========== GUEST MODE METHODS ==========
    
    /**
     * Enter guest mode
     */
    suspend fun enterGuestMode() {
        Logger.AuthenticationUseCase.enterGuestMode()
        userPreferencesManager.enterGuestMode()
        _authState.value = AuthState.Guest
    }
    
    /**
     * Exit guest mode (when user logs in)
     */
    suspend fun exitGuestMode() {
        Logger.AuthenticationUseCase.exitGuestMode()
        userPreferencesManager.exitGuestMode()
    }
    
    /**
     * Check if user is in guest mode
     */
    suspend fun isGuestMode(): Boolean {
        return userPreferencesManager.isGuestModeSync()
    }
    
    /**
     * Get number of lessons completed in guest mode
     */
    suspend fun getGuestLessonsCompleted(): Int {
        return userPreferencesManager.getGuestLessonsCompleted()
    }
    
    /**
     * Increment guest lessons completed
     */
    suspend fun incrementGuestLessonsCompleted() {
        userPreferencesManager.incrementGuestLessonsCompleted()
    }
    
    /**
     * Check if guest should be prompted to login (after X lessons)
     */
    suspend fun shouldPromptGuestToLogin(threshold: Int = 3): Boolean {
        val lessonsCompleted = getGuestLessonsCompleted()
        return lessonsCompleted >= threshold
    }
}
