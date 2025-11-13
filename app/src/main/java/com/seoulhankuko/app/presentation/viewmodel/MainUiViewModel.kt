package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.local.CurrentLessonData
import com.seoulhankuko.app.data.local.UserData
import com.seoulhankuko.app.data.local.UserPreferencesManager
import com.seoulhankuko.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainUiViewModel @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val initialUserData = UserData(
        userId = null,
        email = null,
        name = "Học viên",
        username = null,
        avatarUrl = null,
        accessToken = null,
        refreshToken = null,
        isLoggedIn = false,
        isPremium = false,
        streakDays = 0,
        exp = 0
    )

    val userData: StateFlow<UserData> = userPreferencesManager.userData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = initialUserData
        )

    val currentLesson: StateFlow<CurrentLessonData?> = userPreferencesManager.currentLesson
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            authRepository.refreshCurrentUserData()
        }
    }

    fun clearCurrentLesson() {
        viewModelScope.launch {
            userPreferencesManager.clearCurrentLesson()
        }
    }

    fun setCurrentCourseId(courseId: String?) {
        viewModelScope.launch {
            userPreferencesManager.updateCurrentCourseId(courseId)
        }
    }

    fun refreshUserData() {
        viewModelScope.launch {
            authRepository.refreshCurrentUserData()
        }
    }
}
