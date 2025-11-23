package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.api.model.MissionResponse
import com.seoulhankuko.app.data.repository.MissionRepository
import com.seoulhankuko.app.domain.manager.ExpBonusManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MissionViewModel @Inject constructor(
    private val missionRepository: MissionRepository,
    private val expBonusManager: ExpBonusManager
) : ViewModel() {
    
    private val _missions = MutableStateFlow<List<MissionResponse>>(emptyList())
    val missions: StateFlow<List<MissionResponse>> = _missions.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _bonusRemainingTime = MutableStateFlow(expBonusManager.getRemainingTimeSeconds())
    val bonusRemainingTime: StateFlow<Long> = _bonusRemainingTime.asStateFlow()
    
    private val _isBonusActive = MutableStateFlow(expBonusManager.isActive())
    val isBonusActive: StateFlow<Boolean> = _isBonusActive.asStateFlow()
    
    init {
        // Start countdown timer
        startCountdownTimer()
    }
    
    fun loadTodayMissions(token: String?, isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
            } else {
                _isLoading.value = true
            }
            _error.value = null
            
            missionRepository.getTodayMissions(token)
                .onSuccess { response ->
                    _missions.value = response.missions
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }
            
            _isLoading.value = false
            _isRefreshing.value = false
        }
    }
    
    fun updateBonusStatus() {
        _bonusRemainingTime.value = expBonusManager.getRemainingTimeSeconds()
        _isBonusActive.value = expBonusManager.isActive()
    }
    
    private fun startCountdownTimer() {
        viewModelScope.launch {
            while (true) {
                delay(1000) // Update every second
                updateBonusStatus()
            }
        }
    }
}

