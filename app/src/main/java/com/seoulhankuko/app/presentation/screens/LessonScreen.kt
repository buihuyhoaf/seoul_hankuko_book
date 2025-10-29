package com.seoulhankuko.app.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.data.api.model.LessonDetailResponse
import com.seoulhankuko.app.domain.model.LessonTask
import com.seoulhankuko.app.domain.model.TaskType
import com.seoulhankuko.app.presentation.viewmodel.LessonUiState
import com.seoulhankuko.app.presentation.viewmodel.LessonViewModel
import kotlinx.coroutines.delay
import timber.log.Timber

// Lesson Screen Color Palette
private val PrimaryColor = Color(0xFFFF6F61)
private val SecondaryColor = Color(0xFFFFE0B2)
private val BackgroundColor = Color(0xFFFFF8E7)
private val LessonTextPrimary = Color(0xFF333333)
private val LessonTextSecondary = Color(0xFF757575)

/**
 * Redesigned LessonScreen - Shows simple intro with OK button
 * User clicks OK to navigate to LessonFlowScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    lessonId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToLessonFlow: () -> Unit
) {
    val lessonViewModel: LessonViewModel = hiltViewModel()
    var isLoading by remember { mutableStateOf(true) }
    var visible by remember { mutableStateOf(false) }
    
    // Load lesson data using LessonViewModel
    LaunchedEffect(lessonId) {
        isLoading = true
        lessonViewModel.loadLesson(lessonId)
    }
    
    // Observe UI state from ViewModel
    val uiState by lessonViewModel.uiState.collectAsStateWithLifecycle()
    
    // Update local state based on ViewModel UI state
    LaunchedEffect(uiState) {
        when (val currentState = uiState) {
            is LessonUiState.Loading -> {
                isLoading = true
            }
            is LessonUiState.Success -> {
                isLoading = false
                delay(300)
                visible = true
            }
            is LessonUiState.Error -> {
                isLoading = false
                Timber.e("Error loading lesson: ${currentState.message}")
            }
        }
    }
    
    // Get lesson info
    val lessonInfo = (uiState as? LessonUiState.Success)?.lessonWithChallenges
    
    Scaffold(
        topBar = {
            LessonTopAppBar(
                lessonTitle = lessonInfo?.lesson?.title ?: "Lesson $lessonId",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = BackgroundColor
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            }
            else -> {
                lessonInfo?.let { lesson ->
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(600)),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        LessonIntroContent(
                            lessonTitle = "Lesson ${lesson.lesson.id}: ${lesson.lesson.title}",
                            lessonDescription = "Let's start learning!",
                            totalExercises = lesson.challenges.size,
                            progressPercent = lesson.progressPercent,
                            onStartClick = onNavigateToLessonFlow
                        )
                    }
                } ?: run {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Lesson not found")
                    }
                }
            }
        }
    }
}

/**
 * Lesson intro content with title, description, and OK button
 */
@Composable
fun LessonIntroContent(
    lessonTitle: String,
    lessonDescription: String,
    totalExercises: Int,
    progressPercent: Int = 0,
    onStartClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(2.dp, SecondaryColor)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Lesson Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(PrimaryColor, SecondaryColor)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📚",
                        fontSize = 40.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Title
                Text(
                    text = lessonTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = LessonTextPrimary,
                    textAlign = TextAlign.Center
                )
                
                // Description
                Text(
                    text = lessonDescription,
                    style = MaterialTheme.typography.bodyLarge,
                    color = LessonTextSecondary,
                    textAlign = TextAlign.Center
                )
                
                // Total exercises
                Text(
                    text = "$totalExercises exercises",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryColor,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                
                // Progress indicator
                if (progressPercent > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Progress bar
                        LinearProgressIndicator(
                            progress = { progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = PrimaryColor,
                            trackColor = SecondaryColor
                        )
                        
                        // Progress text
                        Text(
                            text = "Progress: $progressPercent%",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryColor,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // OK Button
        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryColor
            )
        ) {
            Text(
                text = "Bắt đầu",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Top AppBar with gradient background
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonTopAppBar(
    lessonTitle: String,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = lessonTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFF8A65), Color(0xFFFFD180))
                )
            )
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(0.dp)
            )
    )
}

// Task UI and progress components removed as per the simplified LessonScreen intro design

