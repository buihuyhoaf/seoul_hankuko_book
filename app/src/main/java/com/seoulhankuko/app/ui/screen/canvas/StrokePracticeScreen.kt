package com.seoulhankuko.app.ui.screen.canvas

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.R
import com.seoulhankuko.app.domain.model.ReferencePattern
import com.seoulhankuko.app.presentation.utils.UnitColors

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
    
    // Get current reference stroke
    val currentReferenceStroke = remember(uiState) {
        when (uiState) {
            is StrokePracticeUiState.Ready -> {
                viewModel.getCurrentReferenceStroke()
            }
            else -> null
        }
    }
    
    // Animation for correction
    val correctingPath = remember {
        mutableStateOf<com.seoulhankuko.app.domain.model.StrokePath?>(null)
    }
    
    LaunchedEffect(uiState) {
        if (uiState is StrokePracticeUiState.Correcting) {
            correctingPath.value = (uiState as StrokePracticeUiState.Correcting).correctedPath
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
                    CircularProgressIndicator()
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
                    correctingPath = null,
                    isCorrecting = false,
                    onCanvasSizeChanged = { w, h -> viewModel.setCanvasSize(w, h) },
                    onStrokeStart = { viewModel.startStroke(it) },
                    onStrokeMove = { viewModel.appendPoint(it) },
                    onStrokeEnd = { viewModel.endStroke() },
                    onClear = { viewModel.clear() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            is StrokePracticeUiState.Drawing -> {
                // Get reference pattern - use cached pattern from ViewModel
                val refStroke = viewModel.getCurrentReferenceStroke()
                val cachedPattern = remember { 
                    // Get pattern from previous Ready state
                    // We'll need to store this in ViewModel or get it from state
                    // For now, use the current reference stroke
                    refStroke?.let { 
                        ReferencePattern(
                            character = character,
                            strokes = listOf(it)
                        )
                    } ?: ReferencePattern(character, emptyList())
                }
                StrokePracticeContent(
                    uiState = StrokePracticeUiState.Ready(
                        referencePattern = cachedPattern,
                        currentStrokeIndex = cachedPattern.strokeCount - 1
                    ),
                    currentReferenceStroke = refStroke,
                    completedStrokes = completedStrokes,
                    currentUserStroke = currentUserStroke,
                    correctingPath = null,
                    isCorrecting = false,
                    onCanvasSizeChanged = { w, h -> viewModel.setCanvasSize(w, h) },
                    onStrokeStart = { viewModel.startStroke(it) },
                    onStrokeMove = { viewModel.appendPoint(it) },
                    onStrokeEnd = { viewModel.endStroke() },
                    onClear = { viewModel.clear() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            is StrokePracticeUiState.Correcting -> {
                // Get reference pattern from ViewModel cache
                val cachedPattern = viewModel.cachedReferencePattern
                val refStroke = currentReferenceStroke
                val pattern = cachedPattern ?: ReferencePattern(character, emptyList())
                StrokePracticeContent(
                    uiState = StrokePracticeUiState.Ready(
                        referencePattern = pattern,
                        currentStrokeIndex = pattern.strokeCount - 1
                    ),
                    currentReferenceStroke = refStroke,
                    completedStrokes = completedStrokes,
                    currentUserStroke = emptyList(),
                    correctingPath = state.correctedPath,
                    isCorrecting = true,
                    onCanvasSizeChanged = { w, h -> viewModel.setCanvasSize(w, h) },
                    onStrokeStart = { },
                    onStrokeMove = { },
                    onStrokeEnd = { },
                    onClear = { viewModel.clear() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            is StrokePracticeUiState.Completed -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Hoàn thành",
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Hoàn thành! 🎉",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "Độ chính xác: ${(state.similarity * 100).toInt()}%",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { viewModel.clear() }) {
                        Text("Luyện lại")
                    }
                }
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
    currentReferenceStroke: com.seoulhankuko.app.domain.model.ReferenceStroke?,
    completedStrokes: List<com.seoulhankuko.app.domain.model.StrokePath>,
    currentUserStroke: List<Offset>,
    correctingPath: com.seoulhankuko.app.domain.model.StrokePath?,
    isCorrecting: Boolean,
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
        // Stroke info
        StrokeInfoBar(
            currentStrokeIndex = uiState.currentStrokeIndex,
            totalStrokes = uiState.referencePattern.strokeCount,
            modifier = Modifier.padding(16.dp)
        )
        
        // Canvas area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
        ) {
            StrokePracticeCanvas(
                referenceStroke = currentReferenceStroke,
                completedStrokes = completedStrokes,
                currentUserStroke = currentUserStroke,
                correctingPath = correctingPath,
                isCorrecting = isCorrecting,
                onCanvasSizeChanged = { _, _ -> },
                onStrokeStart = onStrokeStart,
                onStrokeMove = onStrokeMove,
                onStrokeEnd = onStrokeEnd,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Controls
        StrokePracticeControls(
            onClear = onClear,
            modifier = Modifier.padding(16.dp)
        )
        
        // Feedback
        if (isCorrecting) {
            Text(
                text = "Đang tự động điều chỉnh...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = UnitColors.TextSecondary
            )
        }
    }
}

/**
 * Info bar showing current stroke progress
 */
@Composable
private fun StrokeInfoBar(
    currentStrokeIndex: Int,
    totalStrokes: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nét vẽ: ${currentStrokeIndex + 1} / $totalStrokes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            LinearProgressIndicator(
                progress = { (currentStrokeIndex + 1).toFloat() / totalStrokes.toFloat() },
                modifier = Modifier
                    .width(100.dp)
                    .height(8.dp)
            )
        }
    }
}

/**
 * Canvas for stroke practice with reference and user drawing
 */
@Composable
private fun StrokePracticeCanvas(
    referenceStroke: com.seoulhankuko.app.domain.model.ReferenceStroke?,
    completedStrokes: List<com.seoulhankuko.app.domain.model.StrokePath>,
    currentUserStroke: List<Offset>,
    correctingPath: com.seoulhankuko.app.domain.model.StrokePath?,
    isCorrecting: Boolean,
    onCanvasSizeChanged: (Float, Float) -> Unit,
    onStrokeStart: (Offset) -> Unit,
    onStrokeMove: (Offset) -> Unit,
    onStrokeEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onStrokeStart(offset)
                    },
                    onDrag = { change, _ ->
                        onStrokeMove(change.position)
                    },
                    onDragEnd = {
                        onStrokeEnd()
                    }
                )
            }
    ) {
        var canvasWidth by remember { mutableStateOf(0f) }
        var canvasHeight by remember { mutableStateOf(0f) }
        
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
                    style = Stroke(width = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            
            // Draw reference stroke (faint) - scale from normalized to canvas
            referenceStroke?.let { ref ->
                val refPath = Path()
                if (ref.points.isNotEmpty()) {
                    val firstPoint = ref.points[0]
                    refPath.moveTo(
                        firstPoint.x * width,
                        firstPoint.y * height
                    )
                    for (i in 1 until ref.points.size) {
                        val point = ref.points[i]
                        refPath.lineTo(
                            point.x * width,
                            point.y * height
                        )
                    }
                }
                drawPath(
                    path = refPath,
                    color = Color.LightGray.copy(alpha = 0.3f),
                    style = Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            
            // Draw correcting path (animated) - scale from normalized to canvas
            if (isCorrecting && correctingPath != null) {
                val correctedPath = Path()
                if (correctingPath.points.isNotEmpty()) {
                    val firstPoint = correctingPath.points[0]
                    correctedPath.moveTo(
                        firstPoint.x * width,
                        firstPoint.y * height
                    )
                    for (i in 1 until correctingPath.points.size) {
                        val point = correctingPath.points[i]
                        correctedPath.lineTo(
                            point.x * width,
                            point.y * height
                        )
                    }
                }
                drawPath(
                    path = correctedPath,
                    color = Color(0xFF4CAF50),
                    style = Stroke(width = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            
            // Draw current user stroke - scale from normalized to canvas
            if (currentUserStroke.isNotEmpty() && !isCorrecting) {
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
                    style = Stroke(width = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
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


