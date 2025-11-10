package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.api.model.LessonResponse
import com.seoulhankuko.app.data.api.model.UnitDetailResponse
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
 * UI state for unit screen
 */
data class UnitUiState(
    val isLoading: Boolean = false,
    val lessons: List<LessonResponse> = emptyList(),
    val unitTitle: String? = null,
    val courseId: String? = null,
    val error: String? = null
)

@HiltViewModel
class UnitViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnitUiState())
    val uiState: StateFlow<UnitUiState> = _uiState.asStateFlow()

    fun loadUnit(unitId: String) {
        viewModelScope.launch {
            // First, try to get cached data immediately (synchronous, fast)
            val cachedUnit = try {
                courseRepository.getCachedUnit(unitId)
            } catch (e: Exception) {
                null
            }
            
            if (cachedUnit != null) {
                // Show cached data immediately - no loading state
                _uiState.value = UnitUiState(
                    isLoading = false,
                    lessons = cachedUnit.lessons.sortedBy { it.orderIndex },
                    unitTitle = cachedUnit.title,
                    courseId = cachedUnit.courseId,
                    error = null
                )
            } else {
                // No cache - show loading state
                _uiState.value = UnitUiState(isLoading = true)
            }
            
            try {
                // Then ensure we have a valid token
                val tokenResult = authRepository.ensureValidToken()
                val token = if (tokenResult.isSuccess) {
                    tokenResult.getOrNull()
                } else {
                    authRepository.getCurrentToken() ?: null
                }
                
                // Use Flow-based approach - will emit cached data first (if not already shown), then fresh data
                courseRepository.getUnitFlow(unitId, token)
                    .catch { exception ->
                        // Only show error if we don't have cached data
                        if (cachedUnit == null) {
                            val errorMessage = when (exception) {
                                is AppException -> exception.message ?: "Unknown error occurred"
                                else -> exception.message ?: "Unknown error occurred"
                            }
                            _uiState.value = UnitUiState(
                                isLoading = false,
                                error = errorMessage
                            )
                        }
                    }
                    .collect { result ->
                        result.fold(
                            onSuccess = { unitDetail ->
                                // Always update with fresh data from API
                                _uiState.value = UnitUiState(
                                    isLoading = false,
                                    lessons = unitDetail.lessons.sortedBy { it.orderIndex },
                                    unitTitle = unitDetail.title,
                                    courseId = unitDetail.courseId,
                                    error = null
                                )
                            },
                            onFailure = { exception ->
                                // If we get a token expiry error, try to refresh and retry once
                                if (exception is AppException.AuthException.TokenExpired) {
                                    val refreshResult = authRepository.refreshCurrentToken()
                                    if (refreshResult.isSuccess) {
                                        // Retry with new token - use suspend function for retry
                                        val newToken = refreshResult.getOrNull()
                                        val retryResult = courseRepository.getUnit(unitId, newToken)
                                        retryResult.fold(
                                            onSuccess = { unitDetail ->
                            _uiState.value = UnitUiState(
                                isLoading = false,
                                lessons = unitDetail.lessons.sortedBy { it.orderIndex },
                                unitTitle = unitDetail.title,
                                courseId = unitDetail.courseId,
                                error = null
                            )
                                            },
                                            onFailure = { retryException ->
                                                // Only show error if we don't have cached data
                                                if (cachedUnit == null) {
                                                    val errorMessage = when (retryException) {
                                                        is AppException -> retryException.message ?: "Unknown error occurred"
                                                        else -> retryException.message ?: "Unknown error occurred"
                                                    }
                                                    _uiState.value = UnitUiState(
                                                        isLoading = false,
                                                        error = errorMessage
                                                    )
                                                }
                                            }
                                        )
                                    } else {
                                        // Only show error if we don't have cached data
                                        if (cachedUnit == null) {
                                            _uiState.value = UnitUiState(
                                                isLoading = false,
                                                error = "Session expired. Please login again."
                                            )
                                        }
                                    }
                                } else {
                                    // Only show error if we don't have cached data
                                    if (cachedUnit == null) {
                                        val errorMessage = when (exception) {
                                            is AppException -> exception.message ?: "Unknown error occurred"
                                            else -> exception.message ?: "Unknown error occurred"
                                        }
                                        _uiState.value = UnitUiState(
                                            isLoading = false,
                                            error = errorMessage
                                        )
                                    }
                                }
                            }
                        )
                    }
            } catch (e: Exception) {
                // Only show error if we don't have cached data
                if (cachedUnit == null) {
                    _uiState.value = UnitUiState(
                        isLoading = false,
                        error = e.message ?: "Failed to load unit"
                    )
                }
            }
        }
    }

    fun clearState() {
        _uiState.value = UnitUiState()
    }
}
