package com.seoulhankuko.app.ui.screen.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.seoulhankuko.app.domain.model.Stroke
import com.seoulhankuko.app.domain.model.StrokePoint
import com.seoulhankuko.app.presentation.utils.UnitColors
import com.seoulhankuko.app.presentation.viewmodel.canvas.CanvasUiState
import com.seoulhankuko.app.presentation.viewmodel.canvas.HangulCanvasViewModel
import kotlin.math.roundToInt

/**
 * Hangul Canvas Screen for free-mode Hangul practice
 *
 * Features:
 * - Touch drawing with stroke capture
 * - Sends strokes to backend for IBM-model prediction
 * - Displays top prediction and confidence feedback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HangulCanvasScreen(
    onBack: () -> Unit = {},
    viewModel: HangulCanvasViewModel = hiltViewModel()
) {
    val strokes by viewModel.strokes.collectAsStateWithLifecycle()
    val currentStroke by viewModel.currentStroke.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Luyện viết Hangul",
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
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
            }

            CanvasControls(
                onClear = { viewModel.clear() },
                onAnalyze = { viewModel.analyze() }
            )

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
        CanvasUiState.Idle -> {
            Text(
                text = "Vẽ một chữ Hangul để bắt đầu luyện tập.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = UnitColors.TextSecondary
            )
        }
        CanvasUiState.Drawing -> {
            Text(
                text = "Đang ghi nhận nét vẽ...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = UnitColors.TextSecondary
            )
        }
        CanvasUiState.Analyzing -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Đang phân tích nét vẽ...",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = UnitColors.TextSecondary
                )
            }
        }
        is CanvasUiState.FreePracticeResult -> {
            val confidencePercent = (uiState.confidence * 100).roundToInt().coerceIn(0, 100)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Âm tiết dự đoán: ${uiState.predictedChar}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = UnitColors.TextPrimary
                )
                Text(
                    text = "Độ tự tin: $confidencePercent%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = UnitColors.TextSecondary
                )
                Text(
                    text = uiState.message,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = UnitColors.TextSecondary
                )

                if (uiState.topPredictions.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Gợi ý thêm",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = UnitColors.TextPrimary
                        )
                        uiState.topPredictions.forEach { prediction ->
                            Text(
                                text = "${prediction.char} (${(prediction.confidence * 100).roundToInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = UnitColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }
        is CanvasUiState.Error -> {
            Text(
                text = uiState.message,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD32F2F)
            )
        }
    }
}

