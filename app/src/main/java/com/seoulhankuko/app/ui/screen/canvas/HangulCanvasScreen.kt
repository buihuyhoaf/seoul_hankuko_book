package com.seoulhankuko.app.ui.screen.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
 * Hangul Canvas Screen - Similar to tensorflow project MainActivity
 * 
 * Features:
 * - PaintView area for drawing (like tensorflow)
 * - Buttons: Clear, Space, Backspace, Classify
 * - EditText to display recognized characters
 * - Alternative buttons for top 2-5 predictions
 * - Translation text (optional)
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
    val resultText by viewModel.resultText.collectAsStateWithLifecycle()
    val alternatives by viewModel.alternatives.collectAsStateWithLifecycle()
    val translationText by viewModel.translationText.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Luyện viết Hangul",
                        style = MaterialTheme.typography.titleLarge,
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
            // PaintView Area - cố định kích thước
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp) // Kích thước cố định
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
                
                // "Vẽ ở đây" hint (center, visible when no strokes)
                if (strokes.isEmpty() && currentStroke.isEmpty()) {
                    Text(
                        text = "Vẽ ở đây",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.titleLarge,
                        color = UnitColors.TextSecondary.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Button Row: Chỉ có 2 nút - Xóa và Kiểm tra
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.clear() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Xóa")
                }
                
                Button(
                    onClick = {
                        val bitmap = StrokeBitmapEncoder.toBitmap(strokes)
                        viewModel.classify(bitmap)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UnitColors.SoftIndigo
                    )
                ) {
                    Text("Kiểm tra")
                }
            }
            
            // Feedback Area (for analyzing state and results)
            FeedbackArea(uiState = uiState, alternatives = alternatives)
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
                color = Color.Black, // Black background like tensorflow
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
private fun FeedbackArea(uiState: CanvasUiState, alternatives: List<String>) {
    when (uiState) {
        CanvasUiState.Idle -> {
            // No feedback needed when idle
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
            val predictedChar = uiState.predictedChar
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        confidencePercent >= 90 -> Color(0xFFE8F5E9) // Light green
                        confidencePercent >= 75 -> Color(0xFFFFF9C4) // Light yellow
                        confidencePercent >= 50 -> Color(0xFFFFE0B2) // Light orange
                        else -> Color(0xFFFFEBEE) // Light red
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Độ tự tin - nổi bật
                    Text(
                        text = "Độ tự tin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = UnitColors.TextSecondary
                    )
                    Text(
                        text = "$confidencePercent%",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = when {
                            confidencePercent >= 90 -> Color(0xFF2E7D32) // Dark green
                            confidencePercent >= 75 -> Color(0xFFF57F17) // Dark yellow
                            confidencePercent >= 50 -> Color(0xFFE65100) // Dark orange
                            else -> Color(0xFFC62828) // Dark red
                        }
                    )
                    
                    // Message với ký tự dự đoán nổi bật
                    val messageParts = uiState.message.split(predictedChar)
                    if (messageParts.size == 2) {
                        // Có ký tự dự đoán trong message
                        Text(
                            text = buildAnnotatedString {
                                append(messageParts[0])
                                withStyle(
                                    style = SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = UnitColors.SoftIndigo
                                    )
                                ) {
                                    append(predictedChar)
                                }
                                append(messageParts[1])
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = UnitColors.TextPrimary
                        )
                    } else {
                        // Fallback nếu không tìm thấy ký tự trong message
                        Text(
                            text = uiState.message,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = UnitColors.TextPrimary
                        )
                    }
                    
                    // Hiển thị lựa chọn khác nếu có
                    if (alternatives.isNotEmpty()) {
                        val alternativesText = alternatives.take(3).joinToString(", ")
                        Text(
                            text = "Nếu không phải, có phải bạn đang vẽ $alternativesText?",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = UnitColors.TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
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
