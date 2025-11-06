package com.seoulhankuko.app.ui.screen.canvas

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

/**
 * UI state for stroke practice screen
 */
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

/**
 * ViewModel for Guided Stroke Practice Screen
 * Manages reference pattern loading, user drawing, and auto-correction
 */
@HiltViewModel
class StrokePracticeViewModel @Inject constructor(
    private val referenceStrokeRepository: ReferenceStrokeRepository,
    private val strokeComparator: StrokeComparator,
    private val strokeAutoCorrector: StrokeAutoCorrector
) : ViewModel() {
    
    private val _character = MutableStateFlow<String>("")
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
    
    /**
     * Initialize with character
     */
    fun initialize(character: String) {
        _character.value = character
        loadReferencePattern(character)
    }
    
    /**
     * Load reference pattern for character
     */
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
    
    private var canvasWidth: Float = 1f
    private var canvasHeight: Float = 1f
    
    /**
     * Set canvas dimensions for normalization
     */
    fun setCanvasSize(width: Float, height: Float) {
        canvasWidth = width
        canvasHeight = height
    }
    
    /**
     * Normalize point to 0-1 range
     */
    private fun normalizePoint(point: Offset): Offset {
        return Offset(
            x = point.x / canvasWidth,
            y = point.y / canvasHeight
        )
    }
    
    /**
     * Start drawing a new stroke
     */
    fun startStroke(point: Offset) {
        if (_uiState.value is StrokePracticeUiState.Ready ||
            _uiState.value is StrokePracticeUiState.Completed) {
            val normalizedPoint = normalizePoint(point)
            _currentUserStroke.value = mutableListOf(normalizedPoint)
            _uiState.value = StrokePracticeUiState.Drawing
        }
    }
    
    /**
     * Append point to current stroke
     */
    fun appendPoint(point: Offset) {
        if (_uiState.value is StrokePracticeUiState.Drawing) {
            val normalizedPoint = normalizePoint(point)
            _currentUserStroke.value = _currentUserStroke.value + normalizedPoint
        }
    }
    
    /**
     * End current stroke and check similarity
     */
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
        
        // Convert user points to normalized coordinates (assuming canvas is full size)
        // For now, we'll assume points are already in reasonable range
        // In production, you'd want to normalize based on actual canvas size
        val userStrokePath = StrokePath(userPoints)
        
        // Reference points are already normalized (0-1), convert to Offset
        val referencePoints = referenceStroke.points.map { offset ->
            // Reference points are already in normalized coordinates
            offset
        }
        val referenceStrokePath = StrokePath(referencePoints)
        
        // Calculate similarity
        val similarity = strokeComparator.calculateSimilarity(
            userStrokePath,
            referenceStrokePath
        )
        
        // Check if similar enough for auto-correction
        if (strokeComparator.isSimilarEnough(userStrokePath, referenceStrokePath)) {
            // Start auto-correction animation
            startAutoCorrection(userStrokePath, referenceStrokePath)
        } else {
            // Not similar enough, keep user stroke
            _completedStrokes.value = _completedStrokes.value + userStrokePath
            moveToNextStroke(pattern, similarity)
        }
    }
    
    /**
     * Start auto-correction animation
     */
    private fun startAutoCorrection(
        userStroke: StrokePath,
        referenceStroke: StrokePath
    ) {
        viewModelScope.launch {
            // Animate correction over 500ms
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
            
            // Correction complete, use reference stroke
            _completedStrokes.value = _completedStrokes.value + referenceStroke
            val pattern = referencePattern!!
            val similarity = strokeComparator.calculateSimilarity(userStroke, referenceStroke)
            moveToNextStroke(pattern, similarity)
        }
    }
    
    /**
     * Move to next stroke or complete
     */
    private fun moveToNextStroke(pattern: ReferencePattern, similarity: Float) {
        currentStrokeIndex++
        
        if (currentStrokeIndex >= pattern.strokes.size) {
            // All strokes completed
            _uiState.value = StrokePracticeUiState.Completed(similarity)
        } else {
            // Move to next stroke
            _uiState.value = StrokePracticeUiState.Ready(
                referencePattern = pattern,
                currentStrokeIndex = currentStrokeIndex
            )
        }
        
        resetCurrentStroke()
    }
    
    /**
     * Reset current stroke
     */
    private fun resetCurrentStroke() {
        _currentUserStroke.value = emptyList()
    }
    
    /**
     * Clear all strokes and reset
     */
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
    
    /**
     * Get current reference stroke
     */
    fun getCurrentReferenceStroke(): com.seoulhankuko.app.domain.model.ReferenceStroke? {
        val pattern = referencePattern ?: return null
        if (currentStrokeIndex >= pattern.strokes.size) return null
        return pattern.strokes[currentStrokeIndex]
    }
}

