package com.seoulhankuko.app.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.presentation.viewmodel.AuthViewModel
import com.seoulhankuko.app.presentation.viewmodel.LessonViewModel
import com.seoulhankuko.app.domain.model.AuthState
import com.seoulhankuko.app.presentation.utils.useCompletionHandler
import kotlinx.coroutines.delay

// Quiz Screen Color Theme
val QuizPrimary = Color(0xFF4A90E2)
val QuizSurfaceVariant = Color(0xFFF6F8FB)
val QuizError = Color(0xFFE74C3C)
val QuizOnPrimary = Color.White
val QuizSuccess = Color(0xFF27AE60)
val QuizBackground = Color(0xFFFAFBFC)
val QuizTextPrimary = Color(0xFF2C3E50)
val QuizTextSecondary = Color(0xFF7F8C8D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    quizId: Int,
    lessonId: Int? = null, // Optional lessonId for progress tracking
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var currentQuestion by remember { mutableStateOf(1) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var quizCompleted by remember { mutableStateOf(false) }
    
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val totalQuestions = 5 // Mock total questions
    
    // Use the shared completion handler
    useCompletionHandler(
        isCompleted = quizCompleted,
        lessonId = lessonId,
        onNavigateBack = onNavigateBack
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🎯 Quiz $quizId",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = QuizTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = QuizPrimary
                        )
                    }
                },
                actions = {
                    Text(
                        text = "$currentQuestion / $totalQuestions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = QuizTextSecondary,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = QuizTextPrimary,
                    navigationIconContentColor = QuizPrimary
                ),
                modifier = Modifier.shadow(elevation = 4.dp, shape = RoundedCornerShape(0.dp))
            )
        },
        containerColor = QuizBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress Bar
            LinearProgressIndicator(
                progress = { currentQuestion.toFloat() / totalQuestions },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = QuizPrimary,
                trackColor = QuizSurfaceVariant
            )
            
            // Main Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (!quizCompleted) {
                    AnimatedContent(
                        targetState = currentQuestion,
                        transitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(300)
                            ) + fadeIn(animationSpec = tween(300)) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(300)
                            ) + fadeOut(animationSpec = tween(300))
                        },
                        label = "question_transition"
                    ) { questionNumber ->
                        QuestionContent(
                            questionNumber = questionNumber,
                            totalQuestions = totalQuestions,
                            selectedAnswer = selectedAnswer,
                            showResult = showResult,
                            isCorrect = isCorrect,
                            onAnswerSelected = { answer ->
                                if (!showResult) {
                                    selectedAnswer = answer
                                    isCorrect = answer == "Hello"
                                    showResult = true
                                }
                            },
                            onNext = {
                                if (currentQuestion < totalQuestions) {
                                    currentQuestion++
                                    selectedAnswer = null
                                    showResult = false
                                    isCorrect = false
                                } else {
                                    quizCompleted = true
                                    // Increment guest lessons completed if in guest mode
                                    if (authState is AuthState.Guest) {
                                        authViewModel.incrementGuestLessonsCompleted()
                                    }
                                }
                            }
                        )
                    }
                } else {
                    QuizCompletionScreen(
                        onNavigateBack = onNavigateBack
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionContent(
    questionNumber: Int,
    totalQuestions: Int,
    selectedAnswer: String?,
    showResult: Boolean,
    isCorrect: Boolean,
    onAnswerSelected: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Question Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = QuizPrimary.copy(alpha = 0.1f)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Question Icon
                Text(
                    text = "🎓",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Question Text
                Text(
                    text = "What does 안녕하세요 mean?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = QuizTextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Answer Grid
        val answers = listOf("Hello", "Goodbye", "Thank you", "Please")
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(200.dp)
        ) {
            itemsIndexed(answers) { index, answer ->
                AnswerCard(
                    answer = answer,
                    index = index,
                    isSelected = selectedAnswer == answer,
                    isCorrect = answer == "Hello",
                    showResult = showResult,
                    onClick = { onAnswerSelected(answer) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Feedback
        AnimatedVisibility(
            visible = showResult,
            enter = fadeIn() + slideInVertically(initialOffsetY = { 20 }),
            label = "feedback_animation"
        ) {
            FeedbackCard(
                isCorrect = isCorrect,
                correctAnswer = "Hello"
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Next/Complete Button
        Button(
            onClick = onNext,
            enabled = showResult,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = QuizPrimary,
                disabledContainerColor = QuizSurfaceVariant
            )
        ) {
            Text(
                text = if (questionNumber < totalQuestions) "Next Question" else "Complete Quiz",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = QuizOnPrimary
            )
        }
    }
}

@Composable
private fun AnswerCard(
    answer: String,
    index: Int,
    isSelected: Boolean,
    isCorrect: Boolean,
    showResult: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            showResult && isCorrect -> QuizSuccess
            showResult && isSelected && !isCorrect -> QuizError
            isSelected -> QuizPrimary
            else -> QuizSurfaceVariant
        },
        animationSpec = tween(300),
        label = "answer_background"
    )
    
    val textColor by animateColorAsState(
        targetValue = when {
            showResult && isCorrect -> QuizOnPrimary
            showResult && isSelected && !isCorrect -> QuizOnPrimary
            isSelected -> QuizOnPrimary
            else -> QuizTextPrimary
        },
        animationSpec = tween(300),
        label = "answer_text"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "answer_scale"
    )
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (isSelected) 8.dp else 2.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Option letter
                Text(
                    text = ('A' + index).toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                // Answer text
                Text(
                    text = answer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                
                // Result icon
                if (showResult && (isSelected || isCorrect)) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = QuizOnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackCard(
    isCorrect: Boolean,
    correctAnswer: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) QuizSuccess.copy(alpha = 0.1f) else QuizError.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = null,
                tint = if (isCorrect) QuizSuccess else QuizError,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = if (isCorrect) "✅ Correct!" else "❌ Wrong! The answer is '$correctAnswer'",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCorrect) QuizSuccess else QuizError
            )
        }
    }
}

@Composable
private fun QuizCompletionScreen(
    onNavigateBack: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(200)
        visible = true
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            label = "completion_animation"
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Completion Icon with animation
                    val rotation by rememberInfiniteTransition(label = "trophy").animateFloat(
                        initialValue = -10f,
                        targetValue = 10f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "rotation"
                    )
                    
                    Text(
                        text = "🎉",
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.graphicsLayer(rotationZ = rotation)
                    )
                    
                    Text(
                        text = "Quiz Completed!",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = QuizTextPrimary
                    )
                    
                    Text(
                        text = "Great job! You've completed the quiz.",
                        style = MaterialTheme.typography.titleMedium,
                        color = QuizTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Returning to lesson...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = QuizTextSecondary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = QuizPrimary
                    )
                }
            }
        }
    }
}

