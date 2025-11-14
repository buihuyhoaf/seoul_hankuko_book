package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.api.model.WeeklyLeaderboardResponse
import com.seoulhankuko.app.data.repository.WeeklyLeaderboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeeklyLeaderboardViewModel @Inject constructor(
    private val repository: WeeklyLeaderboardRepository
) : ViewModel() {
    
    private val _leaderboard = MutableStateFlow<WeeklyLeaderboardResponse?>(null)
    val leaderboard: StateFlow<WeeklyLeaderboardResponse?> = _leaderboard.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun loadLeaderboard(token: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getWeeklyLeaderboard(token)
                .onSuccess { response ->
                    _leaderboard.value = response
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }
            
            _isLoading.value = false
        }
    }
    
    fun updateXp(exp: Int, token: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.updateXp(exp, token)
                .onSuccess { response ->
                    // Reload leaderboard to get updated rank
                    loadLeaderboard(token)
                }
                .onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
        }
    }
}

