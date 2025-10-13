package com.seoulhankuko.app.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.seoulhankuko.app.data.repository.GoogleSignInRepository
import com.seoulhankuko.app.data.repository.GoogleSignInResult
import com.seoulhankuko.app.data.repository.UserData
import com.seoulhankuko.app.core.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Google Sign-In functionality
 */
@HiltViewModel
class GoogleSignInViewModel @Inject constructor(
    private val googleSignInRepository: GoogleSignInRepository
) : ViewModel() {

    // Private mutable state
    private val _googleSignInState = MutableStateFlow<GoogleSignInResult?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _userData = MutableStateFlow<UserData?>(null)

    // Public immutable state
    val googleSignInState: StateFlow<GoogleSignInResult?> = _googleSignInState.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    val userData: StateFlow<UserData?> = _userData.asStateFlow()

    // Get user data from repository
    val isLoggedIn: StateFlow<Boolean> = googleSignInRepository.isLoggedIn
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        // Observe user data from repository
        viewModelScope.launch {
            googleSignInRepository.userData.collect { data ->
                _userData.value = data
            }
        }
    }

    /**
     * Get Google Sign-In client
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        return googleSignInRepository.getGoogleSignInClient(context)
    }

    /**
     * Handle Google Sign-In result
     */
    fun handleGoogleSignInResult(account: GoogleSignInAccount?) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                googleSignInRepository.handleGoogleSignInResult(account)
                    .collect { result ->
                        _googleSignInState.value = result
                        
                        when (result) {
                            is GoogleSignInResult.Loading -> {
                                _isLoading.value = true
                                Logger.GoogleSignIn.apiCallStarted()
                            }
                            is GoogleSignInResult.Success -> {
                                _isLoading.value = false
                                _errorMessage.value = null
                                Logger.GoogleSignIn.signInSuccess(result.userInfo.email)
                                Logger.GoogleSignIn.userDataSaved()
                            }
                            is GoogleSignInResult.Error -> {
                                _isLoading.value = false
                                _errorMessage.value = result.message
                                Logger.GoogleSignIn.signInError(result.message)
                            }
                        }
                    }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Unexpected error: ${e.message}"
                Logger.GoogleSignIn.signInError(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Sign out from Google
     */
    fun signOut(context: Context) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Logger.GoogleSignIn.signOutAttempt()
                
                val googleSignInClient = getGoogleSignInClient(context)
                googleSignInRepository.signOut(googleSignInClient)
                
                _isLoading.value = false
                _errorMessage.value = null
                _userData.value = null
                
                Logger.GoogleSignIn.signOutSuccess()
                Logger.GoogleSignIn.userDataCleared()
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Sign out failed: ${e.message}"
                Logger.GoogleSignIn.signOutError(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Reset Google Sign-In state
     */
    fun resetGoogleSignInState() {
        _googleSignInState.value = GoogleSignInResult.Loading
    }
}

/**
 * Extension function to check if user is signed in
 */
fun GoogleSignInViewModel.isUserSignedIn(): Boolean {
    return userData.value?.isLoggedIn == true
}

/**
 * Extension function to get current user info
 */
fun GoogleSignInViewModel.getCurrentUser(): UserData? {
    return userData.value
}
