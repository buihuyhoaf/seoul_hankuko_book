package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.repository.UserProgressRepository
import com.seoulhankuko.app.presentation.components.DailyExpData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {
    
    val weeklyExpData: StateFlow<List<DailyExpData>> = userProgressRepository.weeklyExpData
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    fun loadWeeklyExpData(username: String, days: Int = 7, token: String? = null, isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
            }
            userProgressRepository.getWeeklyExpData(username, days, token)
            _isRefreshing.value = false
        }
    }
}

