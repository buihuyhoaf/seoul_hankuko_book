package com.seoulhankuko.app.presentation.viewmodel.canvas

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.domain.model.Stroke
import com.seoulhankuko.app.domain.model.StrokePoint
import com.seoulhankuko.app.ml.HangulTFLiteClassifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import timber.log.Timber

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
    private val classifier: HangulTFLiteClassifier
) : ViewModel() {

    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes.asStateFlow()

    private val _currentStroke = MutableStateFlow<List<StrokePoint>>(emptyList())
    val currentStroke: StateFlow<List<StrokePoint>> = _currentStroke.asStateFlow()

    private val _uiState = MutableStateFlow<CanvasUiState>(CanvasUiState.Idle)
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()
    
    // Result text and alternatives (like tensorflow project)
    private val _resultText = MutableStateFlow("")
    val resultText: StateFlow<String> = _resultText.asStateFlow()
    
    private val _alternatives = MutableStateFlow<List<String>>(emptyList())
    val alternatives: StateFlow<List<String>> = _alternatives.asStateFlow()
    
    private val _translationText = MutableStateFlow("")
    val translationText: StateFlow<String> = _translationText.asStateFlow()

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
        _resultText.value = ""
        _alternatives.value = emptyList()
        _translationText.value = ""
        _uiState.value = CanvasUiState.Idle
    }

    fun backspace() {
        if (_resultText.value.isNotEmpty()) {
            _resultText.value = _resultText.value.dropLast(1)
        }
        _alternatives.value = emptyList()
    }
    
    fun space() {
        _resultText.value += " "
        _alternatives.value = emptyList()
    }
    
    fun useAlternative(index: Int) {
        backspace()
        val alt = _alternatives.value.getOrNull(index - 1)
        if (alt != null) {
            _resultText.value += alt
        }
        _alternatives.value = emptyList()
    }

    fun classify(bitmap: Bitmap?) {
        if (bitmap == null) {
            _uiState.value = CanvasUiState.Error("Không có ảnh để phân tích. Hãy vẽ một ký tự trước.")
            return
        }
        
        // Check if bitmap has any non-black pixels - kiểm tra toàn bộ bitmap, không chỉ góc trên trái
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height
        
        // Lấy mẫu từ nhiều vị trí: góc trên trái, giữa, góc dưới phải
        val sampleSize = minOf(1000, totalPixels / 10) // Lấy mẫu 10% hoặc tối đa 1000 pixels
        val step = maxOf(1, totalPixels / sampleSize)
        
        var hasNonBlackPixels = false
        for (i in 0 until totalPixels step step) {
            val x = i % width
            val y = i / width
            val pixel = bitmap.getPixel(x, y)
            if ((pixel and 0xFFFFFF) != 0) { // Có pixel không phải đen
                hasNonBlackPixels = true
                break
            }
        }
        
        if (!hasNonBlackPixels) {
            _uiState.value = CanvasUiState.Error("Vui lòng vẽ một ký tự trước khi nhấn Kiểm tra.")
            return
        }

        _uiState.value = CanvasUiState.Analyzing

        viewModelScope.launch {
                try {
                // Run classification on background thread
                val predictions = withContext(Dispatchers.Default) {
                    classifier.classify(bitmap)
                }

                if (predictions.isEmpty()) {
                    _uiState.value = CanvasUiState.Error("Không thể nhận diện ký tự.")
                    return@launch
                    }

                // Update result text with top prediction
                val topPrediction = predictions[0]
                _resultText.value += topPrediction.char
                
                // Update alternatives (top 2-5)
                _alternatives.value = predictions.drop(1).map { it.char }
                
                // Update UI state with result
                val topPredictions = predictions.map {
                            StrokeTopPredictionUi(
                                index = it.index,
                                char = it.char,
                                confidence = it.confidence
                            )
                        }
                
                // Handle NaN confidence
                val confidence = if (topPrediction.confidence.isNaN() || topPrediction.confidence.isInfinite()) {
                    0.5f // Default confidence if NaN
                } else {
                    topPrediction.confidence.coerceIn(0f, 1f)
                }
                
                val message = when {
                    confidence >= 0.9f -> "Mô hình rất tự tin đây là ${topPrediction.char}."
                    confidence >= 0.75f -> "Khá chắc chắn: ${topPrediction.char}."
                    confidence >= 0.5f -> "Dự đoán ${topPrediction.char}. Hãy tô nét rõ hơn để tăng độ tin cậy."
                    else -> "Độ tin cậy thấp, hãy thử lại với nét to và đều hơn."
                }
                
                // Use safe confidence value
                val safeConfidence = if (topPrediction.confidence.isNaN() || topPrediction.confidence.isInfinite()) {
                    0.5f
                } else {
                    topPrediction.confidence.coerceIn(0f, 1f)
                }
                
                        _uiState.value = CanvasUiState.FreePracticeResult(
                    predictedChar = topPrediction.char,
                    confidence = safeConfidence,
                    message = message,
                    topPredictions = topPredictions.map {
                        val safeConf = if (it.confidence.isNaN() || it.confidence.isInfinite()) {
                            0.0f
                        } else {
                            it.confidence.coerceIn(0f, 1f)
                        }
                        StrokeTopPredictionUi(
                            index = it.index,
                            char = it.char,
                            confidence = safeConf
                        )
                    }
                )
                
                Timber.d("Classification result: ${topPrediction.char} (${topPrediction.confidence})")
                } catch (e: Exception) {
                Timber.e(e, "Error during classification")
                _uiState.value = CanvasUiState.Error(
                    e.message ?: "Không thể phân tích nét vẽ."
                )
            }
        }
    }
    
    fun translate() {
        // Optional: Send text to backend for translation
        // For now, just clear translation text
        _translationText.value = ""
    }
}

