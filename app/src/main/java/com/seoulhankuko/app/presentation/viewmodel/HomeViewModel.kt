package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.api.model.CourseResponse
import com.seoulhankuko.app.data.repository.AuthRepository
import com.seoulhankuko.app.data.repository.CourseRepository
import com.seoulhankuko.app.data.repository.AccountRepository
import com.seoulhankuko.app.domain.model.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.seoulhankuko.app.core.Logger
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val courseRepository: CourseRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {
    
    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )
    
    private val _courses = MutableStateFlow<List<CourseResponse>>(emptyList())
    val courses: StateFlow<List<CourseResponse>> = _courses.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _currentUserName = MutableStateFlow("Học viên")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()
    
    init {
        loadCourses()
        loadCurrentUserName()
    }
    
    fun loadCourses() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = authRepository.getCurrentToken()
                val result = courseRepository.getCourses(page = 1, itemsPerPage = 10, token = token)
                
                result.fold(
                    onSuccess = { response ->
                        val courses = response.data ?: emptyList()  // ← Thay đổi từ items thành data
                        _courses.value = courses
                        Timber.d("Loaded ${courses.size} courses from backend")
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "Failed to load courses")
                        // Fallback to empty list
                        _courses.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception while loading courses")
                _courses.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun loadCurrentUserName() {
        viewModelScope.launch {
            try {
                val currentAccount = accountRepository.getActiveAccount()
                if (currentAccount != null) {
                    _currentUserName.value = currentAccount.displayName ?: "Học viên"
                    Timber.d("Loaded current user name: ${currentAccount.displayName}")
                } else {
                    _currentUserName.value = "Học viên"
                    Timber.d("No active account found, using default name")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load current user name")
                _currentUserName.value = "Học viên"
            }
        }
    }
    
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
