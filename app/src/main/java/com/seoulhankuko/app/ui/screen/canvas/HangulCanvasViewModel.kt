package com.seoulhankuko.app.ui.screen.canvas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.domain.model.MatchLabel
import com.seoulhankuko.app.domain.model.MatchResult
import com.seoulhankuko.app.domain.model.Stroke
import com.seoulhankuko.app.domain.model.StrokePoint
import com.seoulhankuko.app.domain.usecase.AnalyzeStrokesUseCase
import com.seoulhankuko.app.domain.usecase.AnalyzeStrokeUseCase
import com.seoulhankuko.app.domain.usecase.PredictCharacterUseCase
import com.seoulhankuko.app.domain.usecase.PredictCharacterResult
import com.seoulhankuko.app.domain.usecase.StrokePatternRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for Hangul Canvas screen
 */
sealed class CanvasUiState {
    object Idle : CanvasUiState()
    object Drawing : CanvasUiState()
    object Analyzing : CanvasUiState()
    data class Result(val matchResult: MatchResult) : CanvasUiState()
    data class MLResult(
        val predictedChar: String,
        val confidence: Float,
        val targetChar: String,
        val isCorrect: Boolean
    ) : CanvasUiState()
    data class StrokeAnalysisResult(
        val predictedChar: String,
        val confidence: Float,
        val message: String,
        val targetChar: String? = null
    ) : CanvasUiState()
}

/**
 * ViewModel for Hangul Canvas Screen
 * Manages stroke drawing state and analysis
 */
@HiltViewModel
class HangulCanvasViewModel @Inject constructor(
    private val analyzeUseCase: AnalyzeStrokesUseCase,
    private val predictUseCase: PredictCharacterUseCase,
    private val analyzeStrokeUseCase: AnalyzeStrokeUseCase,
    private val repository: StrokePatternRepository
) : ViewModel() {
    
    // Flag to use ML prediction instead of heuristic
    var useMLPrediction: Boolean = true
    
    // Flag to use new Phase 3 stroke analysis API
    var useStrokeAnalysisAPI: Boolean = true
    
    private val _character = MutableStateFlow<String>("")
    val character: StateFlow<String> = _character.asStateFlow()
    
    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes.asStateFlow()
    
    private val _currentStroke = MutableStateFlow<List<StrokePoint>>(emptyList())
    val currentStroke: StateFlow<List<StrokePoint>> = _currentStroke.asStateFlow()
    
    private val _uiState = MutableStateFlow<CanvasUiState>(CanvasUiState.Idle)
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()
    
    /**
     * Initialize with character
     */
    fun initialize(char: String) {
        _character.value = char
        clear()
    }
    
    /**
     * Start a new stroke at the given point
     */
    fun startStroke(point: StrokePoint) {
        _currentStroke.value = mutableListOf(point)
        _uiState.value = CanvasUiState.Drawing
    }
    
    /**
     * Append a point to the current stroke
     */
    fun appendPoint(point: StrokePoint) {
        if (_uiState.value is CanvasUiState.Drawing) {
            _currentStroke.value = _currentStroke.value + point
        }
    }
    
    /**
     * End the current stroke and add it to completed strokes
     */
    fun endStroke() {
        val currentPoints = _currentStroke.value
        if (currentPoints.isNotEmpty()) {
            val newStroke = Stroke(currentPoints)
            _strokes.value = _strokes.value + newStroke
            _currentStroke.value = emptyList()
            _uiState.value = CanvasUiState.Idle
        }
    }
    
    /**
     * Clear all strokes
     */
    fun clear() {
        _strokes.value = emptyList()
        _currentStroke.value = emptyList()
        _uiState.value = CanvasUiState.Idle
    }
    
    /**
     * Analyze the drawn strokes using ML prediction or heuristic
     */
    fun analyze() {
        val char = _character.value
        val currentStrokes = _strokes.value
        
        if (char.isEmpty() || currentStrokes.isEmpty()) {
            _uiState.value = CanvasUiState.Result(
                MatchResult(0f, MatchLabel.TRY_AGAIN)
            )
            return
        }
        
        _uiState.value = CanvasUiState.Analyzing
        
        viewModelScope.launch {
            if (useStrokeAnalysisAPI) {
                // Use new Phase 3 stroke analysis API
                try {
                    // Convert strokes to points format
                    val points = currentStrokes.flatMap { stroke ->
                        stroke.points.map { point -> listOf(point.x, point.y) }
                    }
                    
                    val result = analyzeStrokeUseCase(
                        points = points,
                        imageBase64 = null,
                        targetChar = char
                    )
                    
                    result.fold(
                        onSuccess = { analysis ->
                            val isCorrect = analysis.predictedChar == char
                            _uiState.value = CanvasUiState.StrokeAnalysisResult(
                                predictedChar = analysis.predictedChar,
                                confidence = analysis.confidence,
                                message = analysis.message,
                                targetChar = char
                            )
                        },
                        onFailure = { exception ->
                            // Fallback to old ML prediction
                            tryMLPrediction(char, currentStrokes)
                        }
                    )
                } catch (e: Exception) {
                    // Fallback to old ML prediction
                    tryMLPrediction(char, currentStrokes)
                }
            } else if (useMLPrediction) {
                // Use old ML prediction
                tryMLPrediction(char, currentStrokes)
            } else {
                // Use heuristic matching
                fallbackToHeuristic(char, currentStrokes)
            }
        }
    }
    
    /**
     * Try ML prediction (old API)
     */
    private suspend fun tryMLPrediction(char: String, strokes: List<Stroke>) {
        try {
            val result = predictUseCase(strokes)
            result.fold(
                onSuccess = { prediction ->
                    val isCorrect = prediction.predictedChar == char
                    _uiState.value = CanvasUiState.MLResult(
                        predictedChar = prediction.predictedChar,
                        confidence = prediction.confidence,
                        targetChar = char,
                        isCorrect = isCorrect
                    )
                },
                onFailure = { exception ->
                    // Fallback to heuristic on error
                    fallbackToHeuristic(char, strokes)
                }
            )
        } catch (e: Exception) {
            // Fallback to heuristic on error
            fallbackToHeuristic(char, strokes)
        }
    }
    
    /**
     * Fallback to heuristic matching
     */
    private suspend fun fallbackToHeuristic(char: String, strokes: List<Stroke>) {
        try {
            val result = analyzeUseCase(char, strokes)
            _uiState.value = CanvasUiState.Result(result)
        } catch (e: Exception) {
            _uiState.value = CanvasUiState.Result(
                MatchResult(0f, MatchLabel.TRY_AGAIN)
            )
        }
    }
}

