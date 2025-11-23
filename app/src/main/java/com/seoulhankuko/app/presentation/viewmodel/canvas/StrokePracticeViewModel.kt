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
import kotlinx.coroutines.delay
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
        val userStrokeBeforeCorrection: StrokePath,
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
        // Đảm bảo không chia cho 0
        val width = if (canvasWidth > 0f) canvasWidth else 1f
        val height = if (canvasHeight > 0f) canvasHeight else 1f
        return Offset(
            x = point.x / width,
            y = point.y / height
        )
    }

    fun startStroke(point: Offset) {
        // Cho phép bắt đầu vẽ khi state là Ready, Completed, hoặc Drawing (để có thể vẽ lại)
        // Point đã được normalize trong Composable, lưu trực tiếp
        if (_uiState.value is StrokePracticeUiState.Ready ||
            _uiState.value is StrokePracticeUiState.Completed ||
            _uiState.value is StrokePracticeUiState.Drawing
        ) {
            // Đảm bảo currentUserStroke được cập nhật TRƯỚC để UI có thể hiển thị ngay
            // Sử dụng ArrayList để đảm bảo mutable và có thể thêm điểm nhanh chóng
            _currentUserStroke.value = arrayListOf(point)
            // Chuyển state sau khi đã cập nhật currentUserStroke để trigger re-render
            // StateFlow emit đồng bộ, nên giá trị sẽ có ngay cho collector
            _uiState.value = StrokePracticeUiState.Drawing
        }
    }

    fun appendPoint(point: Offset) {
        // Point đã được normalize trong Composable, lưu trực tiếp
        // Cho phép appendPoint hoạt động khi:
        // 1. State là Drawing (đang vẽ)
        // 2. Hoặc currentUserStroke đã có điểm (đã bắt đầu vẽ, ngay cả khi state chưa kịp chuyển)
        // Điều này đảm bảo nét vẽ hiển thị ngay lập tức, không bị mất điểm khi state chuyển
        val currentList = _currentUserStroke.value
        if (_uiState.value is StrokePracticeUiState.Drawing || currentList.isNotEmpty()) {
            // Tạo list mới để đảm bảo StateFlow emit giá trị mới
            // StateFlow chỉ emit khi giá trị thay đổi (so sánh bằng ==)
            val newList = ArrayList(currentList)
            newList.add(point)
            _currentUserStroke.value = newList
            
            // Đảm bảo state là Drawing nếu chưa phải
            if (_uiState.value !is StrokePracticeUiState.Drawing) {
                _uiState.value = StrokePracticeUiState.Drawing
            }
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

        // Bỏ auto-correction, chỉ thêm vào completedStrokes và chuyển sang nét tiếp theo
        _completedStrokes.value += userStrokePath
        val similarity = strokeComparator.calculateSimilarity(
            userStrokePath,
            referenceStrokePath
        )
        moveToNextStroke(pattern, similarity)
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

    fun getCurrentStrokeIndex(): Int {
        return currentStrokeIndex
    }
}

