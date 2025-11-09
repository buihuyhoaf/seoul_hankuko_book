package com.seoulhankuko.app.presentation.viewmodel.canvas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.domain.model.MatchLabel
import com.seoulhankuko.app.domain.model.MatchResult
import com.seoulhankuko.app.domain.model.Stroke
import com.seoulhankuko.app.domain.model.StrokePoint
import com.seoulhankuko.app.domain.usecase.AnalyzeStrokesUseCase
import com.seoulhankuko.app.domain.usecase.AnalyzeStrokeUseCase
import com.seoulhankuko.app.domain.usecase.PredictCharacterUseCase
import com.seoulhankuko.app.domain.usecase.StrokePatternRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

@HiltViewModel
class HangulCanvasViewModel @Inject constructor(
    private val analyzeUseCase: AnalyzeStrokesUseCase,
    private val predictUseCase: PredictCharacterUseCase,
    private val analyzeStrokeUseCase: AnalyzeStrokeUseCase,
    private val repository: StrokePatternRepository
) : ViewModel() {

    var useMLPrediction: Boolean = true
    var useStrokeAnalysisAPI: Boolean = true

    private val _character = MutableStateFlow("")
    val character: StateFlow<String> = _character.asStateFlow()

    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes.asStateFlow()

    private val _currentStroke = MutableStateFlow<List<StrokePoint>>(emptyList())
    val currentStroke: StateFlow<List<StrokePoint>> = _currentStroke.asStateFlow()

    private val _uiState = MutableStateFlow<CanvasUiState>(CanvasUiState.Idle)
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()

    fun initialize(char: String) {
        _character.value = char
        clear()
    }

    fun startStroke(point: StrokePoint) {
        _currentStroke.value = mutableListOf(point)
        _uiState.value = CanvasUiState.Drawing
    }

    fun appendPoint(point: StrokePoint) {
        if (_uiState.value is CanvasUiState.Drawing) {
            _currentStroke.value = _currentStroke.value + point
        }
    }

    fun endStroke() {
        val currentPoints = _currentStroke.value
        if (currentPoints.isNotEmpty()) {
            val newStroke = Stroke(currentPoints)
            _strokes.value = _strokes.value + newStroke
            _currentStroke.value = emptyList()
            _uiState.value = CanvasUiState.Idle
        }
    }

    fun clear() {
        _strokes.value = emptyList()
        _currentStroke.value = emptyList()
        _uiState.value = CanvasUiState.Idle
    }

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
                try {
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
                            _uiState.value = CanvasUiState.StrokeAnalysisResult(
                                predictedChar = analysis.predictedChar,
                                confidence = analysis.confidence,
                                message = analysis.message,
                                targetChar = char
                            )
                        },
                        onFailure = {
                            tryMLPrediction(char, currentStrokes)
                        }
                    )
                } catch (e: Exception) {
                    tryMLPrediction(char, currentStrokes)
                }
            } else if (useMLPrediction) {
                tryMLPrediction(char, currentStrokes)
            } else {
                fallbackToHeuristic(char, currentStrokes)
            }
        }
    }

    private suspend fun tryMLPrediction(char: String, strokes: List<Stroke>) {
        try {
            val result = predictUseCase(strokes)
            result.fold(
                onSuccess = { prediction ->
                    _uiState.value = CanvasUiState.MLResult(
                        predictedChar = prediction.predictedChar,
                        confidence = prediction.confidence,
                        targetChar = char,
                        isCorrect = prediction.predictedChar == char
                    )
                },
                onFailure = {
                    fallbackToHeuristic(char, strokes)
                }
            )
        } catch (e: Exception) {
            fallbackToHeuristic(char, strokes)
        }
    }

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

