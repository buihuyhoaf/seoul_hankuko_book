package com.seoulhankuko.app.presentation.viewmodel.canvas

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.repository.ReferenceStrokeRepository
import com.seoulhankuko.app.domain.model.ReferencePattern
import com.seoulhankuko.app.domain.model.StrokePath
import com.seoulhankuko.app.domain.usecase.StrokeAutoCorrector
import com.seoulhankuko.app.domain.usecase.StrokeComparator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StrokePracticeUiState {
    object Loading : StrokePracticeUiState()
    data class Ready(
        val referencePattern: ReferencePattern,
        val currentStrokeIndex: Int
    ) : StrokePracticeUiState()
    object Drawing : StrokePracticeUiState()
    data class Correcting(
        val correctedPath: StrokePath,
        val progress: Float
    ) : StrokePracticeUiState()
    data class Completed(
        val similarity: Float
    ) : StrokePracticeUiState()
    data class Error(val message: String) : StrokePracticeUiState()
}

@HiltViewModel
class StrokePracticeViewModel @Inject constructor(
    private val referenceStrokeRepository: ReferenceStrokeRepository,
    private val strokeComparator: StrokeComparator,
    private val strokeAutoCorrector: StrokeAutoCorrector
) : ViewModel() {

    private val _character = MutableStateFlow("")
    val character: StateFlow<String> = _character.asStateFlow()

    private val _uiState = MutableStateFlow<StrokePracticeUiState>(StrokePracticeUiState.Loading)
    val uiState: StateFlow<StrokePracticeUiState> = _uiState.asStateFlow()

    private val _currentUserStroke = MutableStateFlow<List<Offset>>(emptyList())
    val currentUserStroke: StateFlow<List<Offset>> = _currentUserStroke.asStateFlow()

    private val _completedStrokes = MutableStateFlow<List<StrokePath>>(emptyList())
    val completedStrokes: StateFlow<List<StrokePath>> = _completedStrokes.asStateFlow()

    private var referencePattern: ReferencePattern? = null
    val cachedReferencePattern: ReferencePattern?
        get() = referencePattern

    private var currentStrokeIndex: Int = 0
    private var canvasWidth: Float = 1f
    private var canvasHeight: Float = 1f

    fun initialize(character: String) {
        _character.value = character
        loadReferencePattern(character)
    }

    private fun loadReferencePattern(character: String) {
        viewModelScope.launch {
            _uiState.value = StrokePracticeUiState.Loading

            try {
                val pattern = referenceStrokeRepository.getReferencePattern(character)
                if (pattern != null && pattern.isValid) {
                    referencePattern = pattern
                    currentStrokeIndex = 0
                    _uiState.value = StrokePracticeUiState.Ready(
                        referencePattern = pattern,
                        currentStrokeIndex = 0
                    )
                } else {
                    _uiState.value = StrokePracticeUiState.Error(
                        "Không tìm thấy mẫu cho ký tự: $character"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = StrokePracticeUiState.Error(
                    "Lỗi khi tải mẫu: ${e.message}"
                )
            }
        }
    }

    fun setCanvasSize(width: Float, height: Float) {
        canvasWidth = width
        canvasHeight = height
    }

    private fun normalizePoint(point: Offset): Offset {
        return Offset(
            x = point.x / canvasWidth,
            y = point.y / canvasHeight
        )
    }

    fun startStroke(point: Offset) {
        if (_uiState.value is StrokePracticeUiState.Ready ||
            _uiState.value is StrokePracticeUiState.Completed
        ) {
            val normalizedPoint = normalizePoint(point)
            _currentUserStroke.value = mutableListOf(normalizedPoint)
            _uiState.value = StrokePracticeUiState.Drawing
        }
    }

    fun appendPoint(point: Offset) {
        if (_uiState.value is StrokePracticeUiState.Drawing) {
            val normalizedPoint = normalizePoint(point)
            _currentUserStroke.value = _currentUserStroke.value + normalizedPoint
        }
    }

    fun endStroke() {
        val userPoints = _currentUserStroke.value
        if (userPoints.isEmpty() || referencePattern == null) {
            resetCurrentStroke()
            return
        }

        val pattern = referencePattern!!
        if (currentStrokeIndex >= pattern.strokes.size) {
            resetCurrentStroke()
            return
        }

        val referenceStroke = pattern.strokes[currentStrokeIndex]
        val userStrokePath = StrokePath(userPoints)
        val referenceStrokePath = StrokePath(referenceStroke.points)

        val similarity = strokeComparator.calculateSimilarity(
            userStrokePath,
            referenceStrokePath
        )

        if (strokeComparator.isSimilarEnough(userStrokePath, referenceStrokePath)) {
            startAutoCorrection(userStrokePath, referenceStrokePath)
        } else {
            _completedStrokes.value = _completedStrokes.value + userStrokePath
            moveToNextStroke(pattern, similarity)
        }
    }

    private fun startAutoCorrection(
        userStroke: StrokePath,
        referenceStroke: StrokePath
    ) {
        viewModelScope.launch {
            val duration = 500L
            val steps = 30
            val stepDuration = duration / steps

            for (step in 0..steps) {
                val progress = step.toFloat() / steps.toFloat()
                val corrected = strokeAutoCorrector.interpolate(
                    userStroke,
                    referenceStroke,
                    progress
                )

                _uiState.value = StrokePracticeUiState.Correcting(
                    correctedPath = corrected,
                    progress = progress
                )

                kotlinx.coroutines.delay(stepDuration)
            }

            _completedStrokes.value = _completedStrokes.value + referenceStroke
            val pattern = referencePattern!!
            val similarity = strokeComparator.calculateSimilarity(userStroke, referenceStroke)
            moveToNextStroke(pattern, similarity)
        }
    }

    private fun moveToNextStroke(pattern: ReferencePattern, similarity: Float) {
        currentStrokeIndex++

        if (currentStrokeIndex >= pattern.strokes.size) {
            _uiState.value = StrokePracticeUiState.Completed(similarity)
        } else {
            _uiState.value = StrokePracticeUiState.Ready(
                referencePattern = pattern,
                currentStrokeIndex = currentStrokeIndex
            )
        }

        resetCurrentStroke()
    }

    private fun resetCurrentStroke() {
        _currentUserStroke.value = emptyList()
    }

    fun clear() {
        _completedStrokes.value = emptyList()
        _currentUserStroke.value = emptyList()
        currentStrokeIndex = 0

        val pattern = referencePattern
        if (pattern != null) {
            _uiState.value = StrokePracticeUiState.Ready(
                referencePattern = pattern,
                currentStrokeIndex = 0
            )
        }
    }

    fun getCurrentReferenceStroke(): com.seoulhankuko.app.domain.model.ReferenceStroke? {
        val pattern = referencePattern ?: return null
        if (currentStrokeIndex >= pattern.strokes.size) return null
        return pattern.strokes[currentStrokeIndex]
    }
}

