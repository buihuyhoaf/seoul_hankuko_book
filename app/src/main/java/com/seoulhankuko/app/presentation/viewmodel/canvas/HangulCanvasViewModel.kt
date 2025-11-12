package com.seoulhankuko.app.presentation.viewmodel.canvas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.domain.model.Stroke
import com.seoulhankuko.app.domain.model.StrokePoint
import com.seoulhankuko.app.domain.usecase.AnalyzeStrokeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CanvasUiState {
    object Idle : CanvasUiState()
    object Drawing : CanvasUiState()
    object Analyzing : CanvasUiState()
    data class FreePracticeResult(
        val predictedChar: String,
        val confidence: Float,
        val message: String,
        val topPredictions: List<StrokeTopPredictionUi>
    ) : CanvasUiState()
    data class Error(val message: String) : CanvasUiState()
}

data class StrokeTopPredictionUi(
    val index: Int,
    val char: String,
    val confidence: Float
)

@HiltViewModel
class HangulCanvasViewModel @Inject constructor(
    private val analyzeStrokeUseCase: AnalyzeStrokeUseCase
) : ViewModel() {

    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes.asStateFlow()

    private val _currentStroke = MutableStateFlow<List<StrokePoint>>(emptyList())
    val currentStroke: StateFlow<List<StrokePoint>> = _currentStroke.asStateFlow()

    private val _uiState = MutableStateFlow<CanvasUiState>(CanvasUiState.Idle)
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()

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
        val currentStrokes = _strokes.value

        if (currentStrokes.isEmpty()) {
            _uiState.value = CanvasUiState.Error("Hãy vẽ chữ trước khi phân tích nhé!")
            return
        }

        _uiState.value = CanvasUiState.Analyzing

        viewModelScope.launch {
            try {
                val points = currentStrokes.flatMap { stroke ->
                    stroke.points.map { point -> listOf(point.x, point.y) }
                }

                val result = analyzeStrokeUseCase(
                    points = points,
                    imageBase64 = null,
                    targetChar = null
                )

                result.fold(
                    onSuccess = { analysis ->
                        val topPredictions = analysis.topPredictions.take(5).map {
                            StrokeTopPredictionUi(
                                index = it.index,
                                char = it.char,
                                confidence = it.confidence
                            )
                        }
                        _uiState.value = CanvasUiState.FreePracticeResult(
                            predictedChar = analysis.predictedChar,
                            confidence = analysis.confidence,
                            message = analysis.message,
                            topPredictions = topPredictions
                        )
                    },
                    onFailure = { throwable ->
                        _uiState.value = CanvasUiState.Error(
                            throwable.message ?: "Không thể phân tích nét vẽ."
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = CanvasUiState.Error(
                    e.message ?: "Không thể phân tích nét vẽ."
                )
            }
        }
    }
}

