package com.seoulhankuko.app.ui.screen.canvas

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.R
import com.seoulhankuko.app.domain.model.MatchLabel
import com.seoulhankuko.app.domain.model.Stroke
import com.seoulhankuko.app.domain.model.StrokePoint
import com.seoulhankuko.app.presentation.utils.UnitColors
import com.seoulhankuko.app.presentation.viewmodel.canvas.CanvasUiState
import com.seoulhankuko.app.presentation.viewmodel.canvas.HangulCanvasViewModel
import com.seoulhankuko.app.presentation.components.rememberSoundManager
import kotlin.math.roundToInt

/**
 * Hangul Canvas Screen for drawing and learning Hangul characters
 * 
 * Features:
 * - Touch drawing with stroke capture
 * - Stroke analysis and matching
 * - Success animations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HangulCanvasScreen(
    character: String,
    viewModel: HangulCanvasViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    // Initialize ViewModel with character
    LaunchedEffect(character) {
        viewModel.initialize(character)
    }
    
    // Collect state
    val strokes by viewModel.strokes.collectAsStateWithLifecycle()
    val currentStroke by viewModel.currentStroke.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val soundManager = rememberSoundManager()

    LaunchedEffect(uiState) {
        extractConfidence(uiState)?.let { confidence ->
            if (confidence > 0.5f) {
                soundManager.playCorrect()
            } else {
                soundManager.playIncorrect()
            }
        }
    }

    // Animation for success check (ML or heuristic)
    val isPerfect = when (val state = uiState) {
        is CanvasUiState.MLResult -> {
            state.confidence >= 0.85f && state.isCorrect
        }
        is CanvasUiState.StrokeAnalysisResult -> {
            state.confidence >= 0.85f && 
            (state.targetChar == null || state.predictedChar == state.targetChar)
        }
        is CanvasUiState.Result -> {
            state.matchResult.label == MatchLabel.PERFECT
        }
        else -> false
    }
    val scale by animateFloatAsState(
        targetValue = if (isPerfect) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "successScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPerfect) 1f else 0f,
        animationSpec = tween(300),
        label = "successAlpha"
    )
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = character,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = UnitColors.SoftIndigo
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = UnitColors.SoftIndigo
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = UnitColors.TextPrimary,
                    navigationIconContentColor = UnitColors.SoftIndigo
                ),
                modifier = Modifier.shadow(elevation = 4.dp, shape = RoundedCornerShape(0.dp))
            )
        },
        containerColor = UnitColors.BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Drawing canvas with touch handling
                CanvasDrawingArea(
                    strokes = strokes,
                    currentStroke = currentStroke,
                    onStrokeStart = { x, y ->
                        viewModel.startStroke(StrokePoint(x, y))
                    },
                    onStrokeMove = { x, y ->
                        viewModel.appendPoint(StrokePoint(x, y))
                    },
                    onStrokeEnd = {
                        viewModel.endStroke()
                    }
                )
                
                // Success animation overlay
                if (isPerfect) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(alpha),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Hoàn hảo!",
                            modifier = Modifier
                                .size(120.dp * scale)
                                .align(Alignment.Center),
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
            }
            
            // Controls
            CanvasControls(
                onClear = { viewModel.clear() },
                onAnalyze = { viewModel.analyze() }
            )
            
            // Feedback area
            FeedbackArea(uiState = uiState)
        }
    }
}

/**
 * Canvas drawing area with touch gesture handling
 */
@Composable
private fun CanvasDrawingArea(
    strokes: List<Stroke>,
    currentStroke: List<StrokePoint>,
    onStrokeStart: (Float, Float) -> Unit,
    onStrokeMove: (Float, Float) -> Unit,
    onStrokeEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onStrokeStart(offset.x, offset.y)
                    },
                    onDrag = { change, _ ->
                        onStrokeMove(change.position.x, change.position.y)
                    },
                    onDragEnd = {
                        onStrokeEnd()
                    }
                )
            }
    ) {
        StrokeRenderer(
            completedStrokes = strokes,
            currentStroke = currentStroke,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Feedback area showing analysis results
 */
@Composable
private fun FeedbackArea(uiState: CanvasUiState) {
    when (uiState) {
        is CanvasUiState.Idle -> {
            // Empty state
        }
        is CanvasUiState.Drawing -> {
            Text(
                text = "Đang vẽ...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = UnitColors.TextSecondary
            )
        }
        is CanvasUiState.Analyzing -> {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            Text(
                text = "Đang phân tích...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = UnitColors.TextSecondary
            )
        }
        is CanvasUiState.Result,
        is CanvasUiState.MLResult,
        is CanvasUiState.StrokeAnalysisResult -> {
            val confidence = extractConfidence(uiState) ?: return
            val confidencePercent = (confidence * 100).roundToInt().coerceIn(0, 100)
            val message = encouragementMessage(confidencePercent)
            val color = feedbackColor(confidencePercent)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Độ tin cậy: $confidencePercent%",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = UnitColors.TextSecondary
                )
            }
        }
    }
}

private fun extractConfidence(uiState: CanvasUiState): Float? {
    return when (uiState) {
        is CanvasUiState.Result -> uiState.matchResult.score
        is CanvasUiState.MLResult -> uiState.confidence
        is CanvasUiState.StrokeAnalysisResult -> uiState.confidence
        else -> null
    }?.coerceIn(0f, 1f)
}

private fun encouragementMessage(confidencePercent: Int): String {
    return when {
        confidencePercent >= 85 -> "Tuyệt vời! Bạn làm rất xuất sắc!"
        confidencePercent >= 65 -> "Bạn làm tốt lắm!"
        confidencePercent > 50 -> "Khá ổn rồi, tiếp tục phát huy nhé!"
        confidencePercent >= 30 -> "Cố lên! Bạn sắp làm được rồi!"
        else -> "Hãy thử lại nhé, bạn sẽ làm được!"
    }
}

private fun feedbackColor(confidencePercent: Int): Color {
    return when {
        confidencePercent >= 85 -> Color(0xFF4CAF50)
        confidencePercent >= 65 -> Color(0xFF81C784)
        confidencePercent > 50 -> Color(0xFFFFC107)
        confidencePercent >= 30 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

