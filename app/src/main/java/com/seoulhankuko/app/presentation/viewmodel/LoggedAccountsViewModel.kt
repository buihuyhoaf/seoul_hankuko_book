package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.repository.AuthRepository
import com.seoulhankuko.app.domain.model.LoggedAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoggedAccountsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    /**
     * StateFlow for the list of logged accounts
     */
    val loggedAccounts: StateFlow<List<LoggedAccount>> = authRepository.loggedAccounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    /**
     * Attempt auto-login for the selected account
     */
    fun attemptAutoLogin(
        account: LoggedAccount,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = authRepository.attemptAutoLogin(account)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    onFailure(result.exceptionOrNull()?.message ?: "Auto-login failed")
                }
            } catch (e: Exception) {
                onFailure(e.message ?: "Auto-login failed")
            }
        }
    }
    
    /**
     * Check if there are any logged accounts
     */
    suspend fun hasLoggedAccounts(): Boolean {
        return authRepository.hasAnyAccounts()
    }
}
