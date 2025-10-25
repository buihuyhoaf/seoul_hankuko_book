package com.seoulhankuko.app.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.seoulhankuko.app.R
import com.seoulhankuko.app.presentation.viewmodel.UnitViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Custom pastel color palette inspired by Korean minimal design
val SoftIndigo = Color(0xFF5A67D8)
val CoolGray = Color(0xFFA0AEC0)
val WarmOrange = Color(0xFFF6AD55)
val BackgroundLight = Color(0xFFF8FAFC)
val TextPrimary = Color(0xFF1A202C)
val TextSecondary = Color(0xFF4A5568)
val Lavender = Color(0xFFE9D8FD)
val LightGray = Color(0xFFEDF2F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitScreen(
    unitId: Int,
    onNavigateToLesson: (lessonId: Int) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: UnitViewModel = hiltViewModel()
) {
    // Collect unit data from ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(unitId) {
        viewModel.loadUnit(unitId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.unitTitle ?: "Unit",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = SoftIndigo
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = SoftIndigo
                ),
                modifier = Modifier.shadow(elevation = 4.dp, shape = RoundedCornerShape(0.dp))
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
        ) {
            when {
                uiState.isLoading -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = SoftIndigo
                        )
                    }
                }
                
                uiState.error != null -> {
                    // Error state
                    val errorMessage = uiState.error
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Failed to load lessons",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = errorMessage ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Button(
                                onClick = { viewModel.loadUnit(unitId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SoftIndigo
                                )
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                
                uiState.lessons.isEmpty() -> {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No lessons available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }
                
                else -> {
                    // Success state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Welcome text
                        Text(
                            text = "Select a lesson to start",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 16.dp)
                        )

                        // Lesson Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(uiState.lessons) { index, lesson ->
                                LessonCard(
                                    lesson = lesson,
                                    index = index,
                                    onClick = { onNavigateToLesson(lesson.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonCard(
    lesson: com.seoulhankuko.app.data.api.model.LessonResponse,
    index: Int,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    
    // Scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale_animation"
    )
    
    // Fade-in animation with staggered delay
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = index * 100,
            easing = FastOutSlowInEasing
        ),
        label = "alpha_animation"
    )
    
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isPressed = true
                }
                is PressInteraction.Release -> {
                    isPressed = false
                }
                is PressInteraction.Cancel -> {
                    isPressed = false
                }
                else -> {}
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 2.dp else 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = SoftIndigo.copy(alpha = 0.1f),
                spotColor = SoftIndigo.copy(alpha = 0.3f)
            )
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = interactionSource
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Lavender, LightGray)
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Lesson number badge with progress indicator if available
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftIndigo.copy(alpha = 0.2f),
                        modifier = Modifier
                            .widthIn(max = 100.dp)
                            .height(36.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Lesson ${lesson.orderIndex}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = SoftIndigo,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    // Progress indicator
                    lesson.progress?.let { progress ->
                        if (progress.isCompleted) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = WarmOrange,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column {
                    // Lesson title
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Lesson description or stats
                    Text(
                        text = lesson.description?.takeIf { it.isNotBlank() } 
                            ?: "${lesson.quizzesCount} quizzes, ${lesson.exercisesCount} exercises",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // CTA text with icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = WarmOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (lesson.progress?.isCompleted == true) "Continue" else "Tap to start",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmOrange,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
