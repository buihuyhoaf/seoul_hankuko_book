package com.seoulhankuko.app.ui.screen.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.R
import com.seoulhankuko.app.domain.model.ReferencePattern
import com.seoulhankuko.app.domain.model.ReferenceStroke
import com.seoulhankuko.app.domain.model.StrokePath
import com.seoulhankuko.app.presentation.components.SouthKoreaLoadingIcon
import com.seoulhankuko.app.presentation.utils.UnitColors
import com.seoulhankuko.app.presentation.viewmodel.canvas.StrokePracticeUiState
import com.seoulhankuko.app.presentation.viewmodel.canvas.StrokePracticeViewModel

/**
 * Guided Stroke Practice Screen
 * Displays reference stroke and allows user to draw over it
 * Auto-corrects user stroke when similar enough
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrokePracticeScreen(
    character: String,
    viewModel: StrokePracticeViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    // Initialize ViewModel
    LaunchedEffect(character) {
        viewModel.initialize(character)
    }
    
    // Collect state
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUserStroke by viewModel.currentUserStroke.collectAsStateWithLifecycle()
    val completedStrokes by viewModel.completedStrokes.collectAsStateWithLifecycle()
    
    // Get current reference stroke - cập nhật khi uiState hoặc currentStrokeIndex thay đổi
    val currentReferenceStroke = remember(uiState, viewModel.getCurrentStrokeIndex()) {
        when (uiState) {
            is StrokePracticeUiState.Ready -> {
                viewModel.getCurrentReferenceStroke()
            }
            is StrokePracticeUiState.Drawing -> {
                viewModel.getCurrentReferenceStroke()
            }
            is StrokePracticeUiState.Correcting -> {
                viewModel.getCurrentReferenceStroke()
            }
            else -> null
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Luyện viết: $character",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = UnitColors.TextPrimary
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
        when (val state = uiState) {
            is StrokePracticeUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    SouthKoreaLoadingIcon(size = 56.dp)
                }
            }
            
            is StrokePracticeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.initialize(character) }) {
                            Text("Thử lại")
                        }
                    }
                }
            }
            
            is StrokePracticeUiState.Ready -> {
                StrokePracticeContent(
                    uiState = state,
                    currentReferenceStroke = currentReferenceStroke,
                    completedStrokes = completedStrokes,
                    currentUserStroke = currentUserStroke,
                    isCompleted = false,
                    completionSimilarity = null,
                    onCanvasSizeChanged = { w, h -> viewModel.setCanvasSize(w, h) },
                    onStrokeStart = { viewModel.startStroke(it) },
                    onStrokeMove = { viewModel.appendPoint(it) },
                    onStrokeEnd = { viewModel.endStroke() },
                    onClear = { viewModel.clear() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            is StrokePracticeUiState.Drawing -> {
                // Get reference pattern from ViewModel cache
                val cachedPattern = viewModel.cachedReferencePattern
                    ?: ReferencePattern(character, emptyList())
                val refStroke = viewModel.getCurrentReferenceStroke()
                val currentIndex = viewModel.getCurrentStrokeIndex()
                
                StrokePracticeContent(
                    uiState = StrokePracticeUiState.Ready(
                        referencePattern = cachedPattern,
                        currentStrokeIndex = currentIndex
                    ),
                    currentReferenceStroke = refStroke,
                    completedStrokes = completedStrokes,
                    currentUserStroke = currentUserStroke,
                    isCompleted = false,
                    completionSimilarity = null,
                    onCanvasSizeChanged = { w, h -> viewModel.setCanvasSize(w, h) },
                    onStrokeStart = { viewModel.startStroke(it) },
                    onStrokeMove = { viewModel.appendPoint(it) },
                    onStrokeEnd = { viewModel.endStroke() },
                    onClear = { viewModel.clear() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            is StrokePracticeUiState.Completed -> {
                // Get reference pattern from ViewModel cache
                val cachedPattern = viewModel.cachedReferencePattern
                    ?: ReferencePattern(character, emptyList())
                val refStroke = viewModel.getCurrentReferenceStroke()
                val currentIndex = viewModel.getCurrentStrokeIndex()
                
                StrokePracticeContent(
                    uiState = StrokePracticeUiState.Ready(
                        referencePattern = cachedPattern,
                        currentStrokeIndex = currentIndex
                    ),
                    currentReferenceStroke = refStroke,
                    completedStrokes = completedStrokes,
                    currentUserStroke = emptyList(),
                    isCompleted = true,
                    completionSimilarity = state.similarity,
                    onCanvasSizeChanged = { w, h -> viewModel.setCanvasSize(w, h) },
                    onStrokeStart = { },
                    onStrokeMove = { },
                    onStrokeEnd = { },
                    onClear = { viewModel.clear() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            // Correcting state không còn được sử dụng sau khi bỏ auto-correction
            // Nhưng vẫn cần xử lý để khi expression exhaustive
            is StrokePracticeUiState.Correcting -> {
                // Xử lý như Ready state
                val cachedPattern = viewModel.cachedReferencePattern
                    ?: ReferencePattern(character, emptyList())
                val refStroke = viewModel.getCurrentReferenceStroke()
                val currentIndex = viewModel.getCurrentStrokeIndex()
                
                StrokePracticeContent(
                    uiState = StrokePracticeUiState.Ready(
                        referencePattern = cachedPattern,
                        currentStrokeIndex = currentIndex
                    ),
                    currentReferenceStroke = refStroke,
                    completedStrokes = completedStrokes,
                    currentUserStroke = currentUserStroke,
                    isCompleted = false,
                    completionSimilarity = null,
                    onCanvasSizeChanged = { w, h -> viewModel.setCanvasSize(w, h) },
                    onStrokeStart = { viewModel.startStroke(it) },
                    onStrokeMove = { viewModel.appendPoint(it) },
                    onStrokeEnd = { viewModel.endStroke() },
                    onClear = { viewModel.clear() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

/**
 * Content composable for stroke practice
 */
@Composable
private fun StrokePracticeContent(
    uiState: StrokePracticeUiState.Ready,
    currentReferenceStroke: ReferenceStroke?,
    completedStrokes: List<StrokePath>,
    currentUserStroke: List<Offset>,
    isCompleted: Boolean = false,
    completionSimilarity: Float? = null,
    onCanvasSizeChanged: (Float, Float) -> Unit,
    onStrokeStart: (Offset) -> Unit,
    onStrokeMove: (Offset) -> Unit,
    onStrokeEnd: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Text hướng dẫn cách viết tiếng Hàn
        Text(
            text = "Nguyên tắc viết tiếng Hàn: Từ trên xuống dưới, từ trái sang phải.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = UnitColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        
        // Canvas area - maximized for better stroke visibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            StrokePracticeCanvas(
                referencePattern = uiState.referencePattern,
                currentStrokeIndex = uiState.currentStrokeIndex,
                referenceStroke = currentReferenceStroke,
                completedStrokes = completedStrokes,
                currentUserStroke = currentUserStroke,
                onCanvasSizeChanged = { _, _ -> },
                onStrokeStart = onStrokeStart,
                onStrokeMove = onStrokeMove,
                onStrokeEnd = onStrokeEnd,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Controls - compact to save space
        StrokePracticeControls(
            onClear = onClear,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        // Completion message - text đơn giản
        if (isCompleted) {
            val feedbackText = completionSimilarity?.let { similarity ->
                when {
                    similarity >= 0.9f -> "Tuyệt vời! 🌟"
                    similarity >= 0.8f -> "Tốt lắm! 👍"
                    similarity >= 0.7f -> "Khá tốt! 😊"
                    similarity >= 0.5f -> "Gần đúng rồi! 💪"
                    similarity >= 0.3f -> "Cần cố gắng thêm! 📚"
                    else -> "Hãy thử lại nhé! 🔄"
                }
            } ?: "Hoàn thành! 🎉"
            
            Text(
                text = feedbackText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50),
                textAlign = TextAlign.Center
            )
        }
    }
}


/**
 * Canvas for stroke practice with reference and user drawing
 */
@Composable
private fun StrokePracticeCanvas(
    referencePattern: ReferencePattern,
    currentStrokeIndex: Int,
    referenceStroke: ReferenceStroke?,
    completedStrokes: List<StrokePath>,
    currentUserStroke: List<Offset>,
    onCanvasSizeChanged: (Float, Float) -> Unit,
    onStrokeStart: (Offset) -> Unit,
    onStrokeMove: (Offset) -> Unit,
    onStrokeEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }
    
    // Constant cho vùng cho phép vẽ (vùng màu xám)
    // Vùng này là nơi người dùng có thể vẽ và hiển thị nét vẽ
    val ALLOWED_DRAWING_REGION_WIDTH = 140f // Độ rộng của vùng cho phép vẽ
    
    // Box bên ngoài chỉ là container bình thường, không có logic đặc biệt
    Box(
        modifier = modifier
            .background(
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
            .pointerInput(referenceStroke, canvasWidth, canvasHeight) {
                // Sử dụng canvas size từ state, nếu chưa có thì dùng giá trị mặc định
                // (sẽ được cập nhật khi Canvas được positioned)
                val width = if (canvasWidth > 0f) canvasWidth else 1f
                val height = if (canvasHeight > 0f) canvasHeight else 1f
                
                // Tính threshold trong không gian normalized (0-1)
                // Vùng màu xám có độ rộng ALLOWED_DRAWING_REGION_WIDTH pixels
                // Trong không gian normalized, threshold = ALLOWED_DRAWING_REGION_WIDTH / min(width, height)
                val minDimension = minOf(width, height)
                val thresholdNormalized = if (minDimension > 0f) {
                    (ALLOWED_DRAWING_REGION_WIDTH / 2f) / minDimension
                } else {
                    0.1f // Giá trị mặc định nếu chưa có canvas size
                }
                
                // Hàm kiểm tra điểm có nằm trong vùng cho phép vẽ (gần reference stroke)
                fun isPointInAllowedRegion(point: Offset, reference: ReferenceStroke?): Boolean {
                    if (reference == null || reference.points.isEmpty()) return false
                    
                    // Tìm khoảng cách ngắn nhất từ điểm đến reference stroke
                    var minDistance = Float.MAX_VALUE
                    for (i in 0 until reference.points.size - 1) {
                        val p1 = reference.points[i]
                        val p2 = reference.points[i + 1]
                        
                        // Tính khoảng cách từ điểm đến đoạn thẳng p1-p2
                        val dx = p2.x - p1.x
                        val dy = p2.y - p1.y
                        val lengthSquared = dx * dx + dy * dy
                        
                        if (lengthSquared < 0.0001f) {
                            // p1 và p2 quá gần, tính khoảng cách đến p1
                            val dist = kotlin.math.sqrt(
                                (point.x - p1.x) * (point.x - p1.x) +
                                (point.y - p1.y) * (point.y - p1.y)
                            )
                            minDistance = minOf(minDistance, dist)
                        } else {
                            // Tính khoảng cách từ điểm đến đoạn thẳng
                            val t = ((point.x - p1.x) * dx + (point.y - p1.y) * dy) / lengthSquared
                            val tClamped = t.coerceIn(0f, 1f)
                            val closestX = p1.x + tClamped * dx
                            val closestY = p1.y + tClamped * dy
                            val dist = kotlin.math.sqrt(
                                (point.x - closestX) * (point.x - closestX) +
                                (point.y - closestY) * (point.y - closestY)
                            )
                            minDistance = minOf(minDistance, dist)
                        }
                    }
                    
                    return minDistance <= thresholdNormalized
                }
                
                detectDragGestures(
                    onDragStart = { offset ->
                        // Normalize ngay tại đây bằng canvas size thực tế
                        val normalized = Offset(offset.x / width, offset.y / height)
                        // Chỉ cho phép bắt đầu vẽ nếu điểm nằm trong vùng màu xám
                        if (isPointInAllowedRegion(normalized, referenceStroke)) {
                            onStrokeStart(normalized)
                        }
                    },
                    onDrag = { change, _ ->
                        // Normalize ngay tại đây bằng canvas size thực tế
                        val normalized = Offset(change.position.x / width, change.position.y / height)
                        // Chỉ cho phép tiếp tục vẽ nếu điểm nằm trong vùng màu xám
                        if (isPointInAllowedRegion(normalized, referenceStroke)) {
                            onStrokeMove(normalized)
                        }
                    },
                    onDragEnd = {
                        // Luôn gọi endStroke khi kết thúc kéo
                        onStrokeEnd()
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates: LayoutCoordinates ->
                    val size = coordinates.size
                    canvasWidth = size.width.toFloat()
                    canvasHeight = size.height.toFloat()
                    onCanvasSizeChanged(canvasWidth, canvasHeight)
                }
        ) {
            // Use size from DrawScope if canvasWidth/Height not set yet
            val width = if (canvasWidth > 0f) canvasWidth else size.width
            val height = if (canvasHeight > 0f) canvasHeight else size.height
            
            // Vẽ outline của reference strokes để hiển thị vùng cho phép vẽ (vùng màu xám)
            // Vùng này là nơi người dùng có thể vẽ và hiển thị nét vẽ
            // Sử dụng cùng constant ALLOWED_DRAWING_REGION_WIDTH đã định nghĩa ở trên
            referencePattern.strokes.forEachIndexed { index, stroke ->
                val path = Path()
                if (stroke.points.isNotEmpty()) {
                    val first = stroke.points[0]
                    path.moveTo(first.x * width, first.y * height)
                    for (i in 1 until stroke.points.size) {
                        val p = stroke.points[i]
                        path.lineTo(p.x * width, p.y * height)
                    }
                }
                drawPath(
                    path = path,
                    color = Color(0xFFCCCCCC).copy(alpha = 0.25f),
                    style = Stroke(
                        width = ALLOWED_DRAWING_REGION_WIDTH,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                
                // Vẽ số thứ tự ở đầu mỗi nét
                if (stroke.points.isNotEmpty()) {
                    val firstPoint = stroke.points[0]
                    val textX = firstPoint.x * width
                    val textY = firstPoint.y * height
                    
                    // Sử dụng drawIntoCanvas để vẽ text
                    drawIntoCanvas { canvas ->
                        val textPaint = android.graphics.Paint().apply {
                            textSize = 40f // Kích thước text
                            color = android.graphics.Color.parseColor("#4CAF50") // Màu xanh lá
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            isFakeBoldText = true // Làm đậm text
                        }
                        
                        canvas.nativeCanvas.drawText(
                            "${index + 1}", // Số thứ tự (1, 2, 3...)
                            textX,
                            textY - 20f, // Đặt số phía trên điểm đầu một chút
                            textPaint
                        )
                    }
                }
            }
            
            // Draw completed strokes - scale from normalized to canvas
            completedStrokes.forEach { stroke ->
                val completedPath = Path()
                if (stroke.points.isNotEmpty()) {
                    val firstPoint = stroke.points[0]
                    completedPath.moveTo(
                        firstPoint.x * width,
                        firstPoint.y * height
                    )
                    for (i in 1 until stroke.points.size) {
                        val point = stroke.points[i]
                        completedPath.lineTo(
                            point.x * width,
                            point.y * height
                        )
                    }
                }
                drawPath(
                    path = completedPath,
                    color = Color(0xFF2196F3),
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                )
            }
            
            
            // Draw current user stroke - hiển thị nét vẽ của user ngay khi vẽ
            // Đảm bảo hiển thị ngay từ lần đầu vẽ
            if (currentUserStroke.isNotEmpty()) {
                val userPath = Path()
                val firstPoint = currentUserStroke[0]
                userPath.moveTo(
                    firstPoint.x * width,
                    firstPoint.y * height
                )
                for (i in 1 until currentUserStroke.size) {
                    val point = currentUserStroke[i]
                    userPath.lineTo(
                        point.x * width,
                        point.y * height
                    )
                }
                
                drawPath(
                    path = userPath,
                    color = Color(0xFF2196F3),
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                )
            }
            
        }
    }
}


/**
 * Controls for stroke practice
 */
@Composable
private fun StrokePracticeControls(
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onClear,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Xóa tất cả")
        }
    }
}


