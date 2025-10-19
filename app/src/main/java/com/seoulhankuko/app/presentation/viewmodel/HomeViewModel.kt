package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.repository.AuthRepository
import com.seoulhankuko.app.domain.model.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.seoulhankuko.app.core.Logger
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )
    
    fun signOut() {
        Logger.HomeScreen.signOutClicked()
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
    
    /**
     * Enter guest mode
     */
    fun enterGuestMode() {
        viewModelScope.launch {
            authRepository.enterGuestMode()
        }
    }
}
