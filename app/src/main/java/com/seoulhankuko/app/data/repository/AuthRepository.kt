package com.seoulhankuko.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.seoulhankuko.app.data.api.model.LoginRequest
import com.seoulhankuko.app.data.api.model.UserCreateRequest
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.domain.model.AuthState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.seoulhankuko.app.core.Logger
import com.seoulhankuko.app.data.api.util.ExceptionMapper
import com.seoulhankuko.app.domain.exception.AppException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    private var currentToken: String? = null
    
    init {
        // AuthRepository initialized
        // Check if user is already signed in
        checkAuthState()
    }
    
    private fun checkAuthState() {
        Logger.AuthenticationUseCase.checkAuthState()
        val token = getStoredToken()
        if (token != null) {
            Logger.AuthenticationUseCase.foundStoredToken()
            // Verify token is still valid by making a test API call
            verifyTokenAndSetState(token)
        } else {
            Logger.AuthenticationUseCase.noStoredToken()
            _authState.value = AuthState.SignedOut
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
                    val token = "${loginResponse.tokenType} ${loginResponse.accessToken}"
                    Logger.AuthenticationUseCase.tokenReceived()
                    storeToken(token)
                    currentToken = token
                    
                    // Get user info
                    Logger.AuthenticationUseCase.fetchingUserInfo()
                    val userResponse = apiService.getCurrentUser(token)
                    if (userResponse.isSuccessful) {
                        val user = userResponse.body()
                        if (user != null) {
                            Logger.AuthenticationUseCase.userInfoRetrieved(user.username, user.id.toInt())
                            storeUserId(user.id.toString())
                            _authState.value = AuthState.SignedIn(user.id.toString())
                            return Result.success(Unit)
                        }
                    }
                    
                    // If we can't get user info, still consider login successful
                    Logger.AuthenticationUseCase.userInfoFetchFailed()
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
                Logger.AuthenticationUseCase.signOutApiCall()
                apiService.logout(token)
                Logger.AuthenticationUseCase.signOutApiSuccess()
            } ?: Logger.AuthenticationUseCase.signOutApiFailed("No token found")
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Logger.AuthenticationUseCase.signOutApiFailed("Logout error: ${appException.message}")
            // Even if logout API call fails, we still want to sign out locally
        } finally {
            Logger.AuthenticationUseCase.clearingAuthData()
            clearStoredAuth()
            currentToken = null
            _authState.value = AuthState.SignedOut
            Logger.AuthenticationUseCase.signOutComplete()
        }
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
    
    private fun clearStoredAuth() {
        Logger.AuthenticationUseCase.clearingStoredAuth()
        sharedPreferences.edit()
            .remove("auth_token")
            .remove("user_id")
            .apply()
    }
}
