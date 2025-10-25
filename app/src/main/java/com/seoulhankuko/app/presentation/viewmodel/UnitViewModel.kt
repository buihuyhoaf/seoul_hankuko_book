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
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for unit screen
 */
data class UnitUiState(
    val isLoading: Boolean = false,
    val lessons: List<LessonResponse> = emptyList(),
    val unitTitle: String? = null,
    val error: String? = null
)

@HiltViewModel
class UnitViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnitUiState())
    val uiState: StateFlow<UnitUiState> = _uiState.asStateFlow()

    fun loadUnit(unitId: Int) {
        viewModelScope.launch {
            _uiState.value = UnitUiState(isLoading = true)
            
            try {
                // Ensure we have a valid token
                val tokenResult = authRepository.ensureValidToken()
                val token = if (tokenResult.isSuccess) {
                    tokenResult.getOrNull()
                } else {
                    authRepository.getCurrentToken() ?: null
                }
                
                // Load unit details from repository
                courseRepository.getUnit(unitId, token).fold(
                    onSuccess = { unitDetail ->
                        _uiState.value = UnitUiState(
                            isLoading = false,
                            lessons = unitDetail.lessons.sortedBy { it.orderIndex },
                            unitTitle = unitDetail.title,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        // If we get a token expiry error, try to refresh and retry once
                        if (exception is AppException.AuthException.TokenExpired) {
                            val refreshResult = authRepository.refreshCurrentToken()
                            if (refreshResult.isSuccess) {
                                // Retry with new token
                                val newToken = refreshResult.getOrNull()
                                courseRepository.getUnit(unitId, newToken).fold(
                                    onSuccess = { unitDetail ->
                                        _uiState.value = UnitUiState(
                                            isLoading = false,
                                            lessons = unitDetail.lessons.sortedBy { it.orderIndex },
                                            unitTitle = unitDetail.title,
                                            error = null
                                        )
                                    },
                                    onFailure = { retryException ->
                                        val errorMessage = when (retryException) {
                                            is AppException -> retryException.message ?: "Unknown error occurred"
                                            else -> retryException.message ?: "Unknown error occurred"
                                        }
                                        _uiState.value = UnitUiState(
                                            isLoading = false,
                                            error = errorMessage
                                        )
                                    }
                                )
                            } else {
                                _uiState.value = UnitUiState(
                                    isLoading = false,
                                    error = "Session expired. Please login again."
                                )
                            }
                        } else {
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
                )
            } catch (e: Exception) {
                _uiState.value = UnitUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load unit"
                )
            }
        }
    }

    fun clearState() {
        _uiState.value = UnitUiState()
    }
}
