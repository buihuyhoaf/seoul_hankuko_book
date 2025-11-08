package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.api.model.CourseDetailResponse
import com.seoulhankuko.app.data.api.model.UnitResponse
import com.seoulhankuko.app.data.repository.AuthRepository
import com.seoulhankuko.app.data.repository.CourseRepository
import com.seoulhankuko.app.domain.exception.AppException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for course screen
 */
sealed class CourseUiState {
    object Loading : CourseUiState()
    data class Success(
        val course: CourseDetailResponse,
        val units: List<UnitResponse>
    ) : CourseUiState()
    data class Error(val message: String) : CourseUiState()
}

/**
 * Additional state for pull-to-refresh functionality
 */
data class CourseScreenState(
    val uiState: CourseUiState,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CourseUiState>(CourseUiState.Loading)
    val uiState: StateFlow<CourseUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun loadCourse(courseId: String) {
        viewModelScope.launch {
            // First, try to get cached data immediately (synchronous, fast)
            val cachedCourse = try {
                courseRepository.getCachedCourse(courseId)
            } catch (e: Exception) {
                null
            }
            
            if (cachedCourse != null) {
                // Show cached data immediately - no loading state
                _uiState.value = CourseUiState.Success(
                    course = cachedCourse,
                    units = cachedCourse.units.sortedBy { it.orderIndex }
                )
            } else {
                // No cache - show loading state
                _uiState.value = CourseUiState.Loading
            }
            
            try {
                // Then ensure we have a valid token
                val tokenResult = authRepository.ensureValidToken()
                val token = if (tokenResult.isSuccess) {
                    tokenResult.getOrNull()
                } else {
                    // Try to refresh token
                    authRepository.getCurrentToken() ?: null
                }
                
                // Use Flow-based approach - will emit cached data first (if not already shown), then fresh data
                courseRepository.getCourseFlow(courseId, token)
                    .catch { exception ->
                        // Only show error if we don't have cached data
                        if (cachedCourse == null) {
                            val errorMessage = when (exception) {
                                is AppException -> exception.message ?: "Unknown error occurred"
                                else -> exception.message ?: "Unknown error occurred"
                            }
                            _uiState.value = CourseUiState.Error(errorMessage)
                        }
                    }
                    .collect { result ->
                        result.fold(
                            onSuccess = { courseDetail ->
                                // Always update with fresh data from API
                                _uiState.value = CourseUiState.Success(
                                    course = courseDetail,
                                    units = courseDetail.units.sortedBy { it.orderIndex }
                                )
                            },
                            onFailure = { exception ->
                                // If we get a token expiry error, try to refresh and retry once
                                if (exception is AppException.AuthException.TokenExpired) {
                                    val refreshResult = authRepository.refreshCurrentToken()
                                    if (refreshResult.isSuccess) {
                                        // Retry with new token - use suspend function for retry
                                        val newToken = refreshResult.getOrNull()
                                        val retryResult = courseRepository.getCourse(courseId, newToken)
                                        retryResult.fold(
                                            onSuccess = { courseDetail ->
                                                _uiState.value = CourseUiState.Success(
                                                    course = courseDetail,
                                                    units = courseDetail.units.sortedBy { it.orderIndex }
                                                )
                                            },
                                            onFailure = { retryException ->
                                                // Only show error if we don't have cached data
                                                if (cachedCourse == null) {
                                                    val errorMessage = when (retryException) {
                                                        is AppException -> retryException.message ?: "Unknown error occurred"
                                                        else -> retryException.message ?: "Unknown error occurred"
                                                    }
                                                    _uiState.value = CourseUiState.Error(errorMessage)
                                                }
                                            }
                                        )
                                    } else {
                                        // Only show error if we don't have cached data
                                        if (cachedCourse == null) {
                                            _uiState.value = CourseUiState.Error("Session expired. Please login again.")
                                        }
                                    }
                                } else {
                                    // Only show error if we don't have cached data
                                    if (cachedCourse == null) {
                                        val errorMessage = when (exception) {
                                            is AppException -> exception.message ?: "Unknown error occurred"
                                            else -> exception.message ?: "Unknown error occurred"
                                        }
                                        _uiState.value = CourseUiState.Error(errorMessage)
                                    }
                                }
                            }
                        )
                    }
            } catch (e: Exception) {
                // Only show error if we don't have cached data
                if (cachedCourse == null) {
                    _uiState.value = CourseUiState.Error(
                        e.message ?: "Failed to load course"
                    )
                }
            }
        }
    }

    fun refreshCourse(courseId: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            
            try {
                // Get the current authentication token, ensuring it's valid
                val tokenResult = authRepository.ensureValidToken()
                val token = if (tokenResult.isSuccess) {
                    tokenResult.getOrNull()
                } else {
                    authRepository.getCurrentToken() ?: null
                }
                
                // Load course details without changing the loading state
                courseRepository.getCourse(courseId, token).fold(
                    onSuccess = { courseDetail ->
                        _uiState.value = CourseUiState.Success(
                            course = courseDetail,
                            units = courseDetail.units.sortedBy { it.orderIndex }
                        )
                    },
                    onFailure = { exception ->
                        // If we get a token expiry error, try to refresh and retry once
                        if (exception is AppException.AuthException.TokenExpired) {
                            val refreshResult = authRepository.refreshCurrentToken()
                            if (refreshResult.isSuccess) {
                                val newToken = refreshResult.getOrNull()
                                courseRepository.getCourse(courseId, newToken).fold(
                                    onSuccess = { courseDetail ->
                                        _uiState.value = CourseUiState.Success(
                                            course = courseDetail,
                                            units = courseDetail.units.sortedBy { it.orderIndex }
                                        )
                                    },
                                    onFailure = { retryException ->
                                        val errorMessage = when (retryException) {
                                            is AppException -> retryException.message ?: "Unknown error occurred"
                                            else -> retryException.message ?: "Unknown error occurred"
                                        }
                                        _uiState.value = CourseUiState.Error(errorMessage)
                                    }
                                )
                            } else {
                                // Don't change the state on refresh failure, just keep current state
                                // User can try again manually
                            }
                        } else {
                            val errorMessage = when (exception) {
                                is AppException -> exception.message ?: "Unknown error occurred"
                                else -> exception.message ?: "Unknown error occurred"
                            }
                            // Don't change UI state during refresh if there's an error
                            // Let the user retry manually
                        }
                    }
                )
            } catch (e: Exception) {
                // Don't change UI state during refresh if there's an error
                // Let the user retry manually
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
