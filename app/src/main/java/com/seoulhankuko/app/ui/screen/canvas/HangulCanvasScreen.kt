package com.seoulhankuko.app.ui.screen.canvas

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
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
import com.seoulhankuko.app.domain.model.StrokePoint
import com.seoulhankuko.app.presentation.utils.UnitColors
import androidx.compose.runtime.rememberCoroutineScope
import com.seoulhankuko.app.domain.model.Stroke
import kotlinx.coroutines.launch
import com.seoulhankuko.app.presentation.viewmodel.canvas.CanvasUiState
import com.seoulhankuko.app.presentation.viewmodel.canvas.HangulCanvasViewModel

/**
 * Hangul Canvas Screen for drawing and learning Hangul characters
 * 
 * Features:
 * - Touch drawing with stroke capture
 * - Template overlay (toggleable)
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
    
    // Template state
    var showTemplate by remember { mutableStateOf(false) }
    var templateStrokes by remember { mutableStateOf<List<Stroke>?>(null) }
    val scope = rememberCoroutineScope()
    
    // Load template pattern
    LaunchedEffect(character) {
        scope.launch {
            // In production, inject repository via ViewModel or parameter
            // For now, template loading is optional
            templateStrokes = null
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
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = character,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = UnitColors.SoftIndigo
                        )
                    }
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
                colors = TopAppBarDefaults.topAppBarColors(
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
                    templateStrokes = templateStrokes,
                    showTemplate = showTemplate,
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
                showTemplate = showTemplate,
                onToggleTemplate = { showTemplate = !showTemplate },
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
    strokes: List<com.seoulhankuko.app.domain.model.Stroke>,
    currentStroke: List<StrokePoint>,
    templateStrokes: List<com.seoulhankuko.app.domain.model.Stroke>?,
    showTemplate: Boolean,
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
            templateStrokes = templateStrokes,
            showTemplate = showTemplate,
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
        is CanvasUiState.Result -> {
            val result = uiState.matchResult
            val (message, color) = when (result.label) {
                MatchLabel.PERFECT -> "Hoàn hảo! 🎉" to Color(0xFF4CAF50)
                MatchLabel.ALMOST -> "Tốt! Cần cải thiện thêm một chút" to Color(0xFFFF9800)
                MatchLabel.TRY_AGAIN -> "Hãy thử lại! 💪" to Color(0xFFF44336)
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "Điểm số: ${(result.score * 100).toInt()}%",
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = UnitColors.TextSecondary
                )
            }
        }
        is CanvasUiState.MLResult -> {
            val result = uiState
            val (message, color) = when {
                result.confidence >= 0.85f && result.isCorrect -> {
                    "Hoàn hảo! 🎉" to Color(0xFF4CAF50)
                }
                result.isCorrect && result.confidence >= 0.7f -> {
                    "Tốt! Cần cải thiện thêm một chút" to Color(0xFFFF9800)
                }
                result.isCorrect -> {
                    "Đúng nhưng chưa đủ tự tin. Hãy thử lại! 💪" to Color(0xFFFF9800)
                }
                else -> {
                    "Hãy thử lại! Gợi ý: vẽ theo mẫu" to Color(0xFFF44336)
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                
                Text(
                    text = "Ký tự vẽ: ${result.predictedChar}",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = UnitColors.TextSecondary
                )
                
                if (!result.isCorrect) {
                    Text(
                        text = "Ký tự đúng: ${result.targetChar}",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                
                Text(
                    text = "Độ tin cậy: ${(result.confidence * 100).toInt()}%",
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = UnitColors.TextSecondary
                )
            }
        }
        is CanvasUiState.StrokeAnalysisResult -> {
            val result = uiState
            val isCorrect = result.targetChar == null || result.predictedChar == result.targetChar
            val (message, color) = when {
                result.confidence >= 0.85f && isCorrect -> {
                    "Hoàn hảo! 🎉" to Color(0xFF4CAF50)
                }
                isCorrect && result.confidence >= 0.7f -> {
                    "Tốt! Cần cải thiện thêm một chút" to Color(0xFFFF9800)
                }
                isCorrect -> {
                    "Đúng nhưng chưa đủ tự tin. Hãy thử lại! 💪" to Color(0xFFFF9800)
                }
                else -> {
                    result.message to Color(0xFFF44336)
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                
                Text(
                    text = "Ký tự vẽ: ${result.predictedChar}",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = UnitColors.TextSecondary
                )
                
                if (result.targetChar != null && !isCorrect) {
                    Text(
                        text = "Ký tự đúng: ${result.targetChar}",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                
                Text(
                    text = "Độ tin cậy: ${(result.confidence * 100).toInt()}%",
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = UnitColors.TextSecondary
                )
            }
        }
    }
}

