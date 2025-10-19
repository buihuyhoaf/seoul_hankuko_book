package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.repository.AuthRepository
import com.seoulhankuko.app.domain.model.AuthState
import com.seoulhankuko.app.domain.model.LoggedAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.seoulhankuko.app.core.Logger
import com.seoulhankuko.app.domain.exception.AppException
import javax.inject.Inject

/**
 * UI state for auto-login operations
 */
sealed class AutoLoginState {
    object Loading : AutoLoginState()
    object Success : AutoLoginState()
    data class Error(val message: String) : AutoLoginState()
    object Idle : AutoLoginState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )
    
    val loggedAccounts: StateFlow<List<LoggedAccount>> = authRepository.loggedAccounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // UI state for auto-login operations
    private val _autoLoginState = MutableStateFlow<AutoLoginState>(AutoLoginState.Idle)
    val autoLoginState: StateFlow<AutoLoginState> = _autoLoginState.asStateFlow()
    
    fun signIn(email: String, password: String) {
        Logger.LoginScreen.signInButtonClicked(email)
        viewModelScope.launch {
            authRepository.signIn(email, password)
        }
    }
    
    fun signOut() {
        Logger.AuthenticationUseCase.signOutAttempt()
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
    
    fun hasValidToken(): Boolean {
        return authRepository.hasValidToken()
    }
    
    suspend fun getLoggedAccountsList(): List<LoggedAccount> {
        return authRepository.getLoggedAccountsList()
    }
    
    /**
     * Attempt auto-login with a specific account
     */
    fun tryAutoLogin(account: LoggedAccount) {
        viewModelScope.launch {
            _autoLoginState.value = AutoLoginState.Loading
            try {
                val result = authRepository.attemptAutoLogin(account)
                if (result.isSuccess) {
                    _autoLoginState.value = AutoLoginState.Success
                } else {
                    _autoLoginState.value = AutoLoginState.Error(
                        result.exceptionOrNull()?.message ?: "Auto-login failed"
                    )
                }
            } catch (e: Exception) {
                _autoLoginState.value = AutoLoginState.Error(
                    e.message ?: "Auto-login failed"
                )
            }
        }
    }
    
    /**
     * Logout the currently active account
     */
    fun logoutActiveAccount() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
    
    /**
     * Refresh token for current user
     */
    fun refreshToken(refreshToken: String) {
        viewModelScope.launch {
            _autoLoginState.value = AutoLoginState.Loading
            try {
                val result = authRepository.refreshCurrentToken()
                if (result.isSuccess) {
                    _autoLoginState.value = AutoLoginState.Success
                } else {
                    _autoLoginState.value = AutoLoginState.Error(
                        result.exceptionOrNull()?.message ?: "Token refresh failed"
                    )
                }
            } catch (e: Exception) {
                _autoLoginState.value = AutoLoginState.Error(
                    e.message ?: "Token refresh failed"
                )
            }
        }
    }
    
    /**
     * Clear auto-login state
     */
    fun clearAutoLoginState() {
        _autoLoginState.value = AutoLoginState.Idle
    }
    
    /**
     * Check if any accounts exist in local database
     */
    suspend fun hasAnyAccounts(): Boolean {
        return authRepository.hasAnyAccounts()
    }
    
    /**
     * Delete an account from local database
     */
    fun deleteAccount(email: String) {
        viewModelScope.launch {
            authRepository.deleteAccount(email)
        }
    }
    
    // ========== GUEST MODE METHODS ==========
    
    /**
     * Enter guest mode
     */
    fun enterGuestMode() {
        viewModelScope.launch {
            authRepository.enterGuestMode()
        }
    }
    
    /**
     * Check if user is in guest mode
     */
    suspend fun isGuestMode(): Boolean {
        return authRepository.isGuestMode()
    }
    
    /**
     * Get number of lessons completed in guest mode
     */
    suspend fun getGuestLessonsCompleted(): Int {
        return authRepository.getGuestLessonsCompleted()
    }
    
    /**
     * Increment guest lessons completed
     */
    fun incrementGuestLessonsCompleted() {
        viewModelScope.launch {
            authRepository.incrementGuestLessonsCompleted()
        }
    }
    
    /**
     * Check if guest should be prompted to login (after X lessons)
     */
    suspend fun shouldPromptGuestToLogin(threshold: Int = 3): Boolean {
        return authRepository.shouldPromptGuestToLogin(threshold)
    }
}
