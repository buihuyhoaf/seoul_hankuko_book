package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.domain.model.ChallengeWithOptions
import com.seoulhankuko.app.presentation.components.rememberSoundManager
import com.seoulhankuko.app.presentation.components.TTSManager
import com.seoulhankuko.app.presentation.viewmodel.LessonUiState
import com.seoulhankuko.app.presentation.viewmodel.LessonViewModel
import com.seoulhankuko.app.presentation.utils.LessonFlowColors
import com.seoulhankuko.app.presentation.utils.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * LessonFlowScreen - Shows quiz questions in a swipeable pager
 * Each page shows one question from lesson.questions
 */
@Composable
fun LessonFlowScreen(
    lessonId: String,
    onNavigateBack: () -> Unit,
    onNavigateToListening: (exerciseId: String) -> Unit,
    viewModel: LessonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Load lesson on start
    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }
    
    when (val state = uiState) {
        is LessonUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LessonFlowColors.BackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        }
        
        is LessonUiState.Success -> {
            state.lessonWithChallenges?.let { lesson ->
                // Find listening exercise ID
                val listeningExerciseId = lesson.exercisesResponse.firstOrNull { 
                    it.type.lowercase() == "listening" 
                }?.id
                
                QuizPagerFlow(
                    lessonId = lessonId,
                    challenges = lesson.challenges,
                    viewModel = viewModel,
                    onNavigateBack = onNavigateBack,
                    onNavigateToListening = {
                        listeningExerciseId?.let { exerciseId ->
                            onNavigateToListening(exerciseId)
                        } ?: onNavigateBack()
                    },
                    onCompleteAllQuestions = { 
                        // Return to lesson screen
                        onNavigateBack()
                    }
                )
            }
        }
        
        is LessonUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LessonFlowColors.BackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Error loading lesson",
                        style = MaterialTheme.typography.titleLarge,
                        color = LessonFlowColors.ErrorColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Quiz Pager - Swipeable pages for each quiz question
 */
@Composable
fun QuizPagerFlow(
    lessonId: String,
    challenges: List<ChallengeWithOptions>,
    viewModel: LessonViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToListening: () -> Unit, // Keep original signature for QuizPagerFlow
    onCompleteAllQuestions: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { challenges.size })
    val coroutineScope = rememberCoroutineScope() // Move here to composable scope
    var currentAnswerStatus by remember { mutableStateOf<AnswerStatus>(AnswerStatus.NONE) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var showCompletionPrompt by remember { mutableStateOf(false) }
    
    // Sound manager for playing correct/incorrect sounds
    val soundManager = rememberSoundManager()
    
    // TTS Manager for reading question content - injected via ViewModel
    val ttsManager = viewModel.ttsManager
    
    // Cleanup managers when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            soundManager.cleanup()
        }
    }
    
    // Check if user is on the last question
    val isLastQuestion = pagerState.currentPage == challenges.size - 1
    
    LaunchedEffect(pagerState.currentPage) {
        // Reset answer status when page changes
        currentAnswerStatus = AnswerStatus.NONE
        selectedOption = null
        
        // Read the question content using TTS (no delay needed, TTS is already initialized)
        if (challenges.isNotEmpty()) {
            val currentChallenge = challenges[pagerState.currentPage]
            val questionText = currentChallenge.challenge.question
            if (questionText.isNotBlank() && ttsManager.isAvailable()) {
                ttsManager.speak(questionText, speed = 0.8f)
            }
        }
    }
    
    // Show completion prompt when last question is answered
    if (showCompletionPrompt) {
        CompletionPrompt(
            lessonId = lessonId,
            viewModel = viewModel,
            onYes = onNavigateToListening,
            onNo = {
                // Update progress via viewModel then navigate back
                viewModel.updateLessonProgress(lessonId) {
                    onNavigateBack()
                }
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LessonFlowColors.BackgroundColor)
        ) {
            // Progress indicator
            ProgressIndicator(
                currentPage = pagerState.currentPage + 1,
                totalPages = challenges.size
            )
            
            // Pager with questions
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                pageSpacing = 16.dp,
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) { page ->
                val challenge = challenges[page]
                QuizQuestionCard(
                    challenge = challenge,
                    selectedOption = selectedOption,
                    answerStatus = currentAnswerStatus,
                    onOptionSelected = { optionId ->
                        if (currentAnswerStatus == AnswerStatus.NONE) {
                            selectedOption = optionId
                        }
                    },
                    onAnswerSubmitted = { isCorrect ->
                        // Play sound effect based on answer correctness
                        if (isCorrect) {
                            soundManager.playCorrect()
                        } else {
                            soundManager.playIncorrect()
                        }
                        
                        currentAnswerStatus = if (isCorrect) {
                            AnswerStatus.CORRECT
                        } else {
                            AnswerStatus.WRONG
                        }
                        // If correct and an option is selected, submit to backend to increment progress
                        if (isCorrect && selectedOption != null) {
                            viewModel.submitPracticeCorrectAnswer(
                                lessonId = lessonId,
                                questionId = challenge.challenge.id,
                                selectedOptionId = selectedOption ?: ""
                            )
                        }
                        
                        // Auto-advance to next question after 1 second
                        coroutineScope.launch {
                            delay(1000)
                            
                            if (isLastQuestion) {
                                // Show completion prompt
                                showCompletionPrompt = true
                            } else {
                                pagerState.animateScrollToPage(page + 1)
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Individual quiz question card
 */
@Composable
fun QuizQuestionCard(
    challenge: ChallengeWithOptions,
    selectedOption: String?,
    answerStatus: AnswerStatus,
    onOptionSelected: (String) -> Unit,
    onAnswerSubmitted: (Boolean) -> Unit
) {
    val isAnswered = answerStatus != AnswerStatus.NONE
    val correctOption = challenge.options.find { it.correct }?.id
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Question text
            Text(
                text = challenge.challenge.question,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = LessonFlowColors.TextPrimary,
                lineHeight = 28.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Answer options
            challenge.options.forEach { option ->
                val isSelected = selectedOption == option.id
                val isCorrect = option.correct
                
                // Determine background color
                val backgroundColor = when {
                    !isAnswered && isSelected -> LessonFlowColors.PrimaryColor.copy(alpha = 0.1f)
                    answerStatus == AnswerStatus.WRONG && isSelected -> LessonFlowColors.ErrorColor.copy(alpha = 0.1f)
                    answerStatus == AnswerStatus.CORRECT && isCorrect -> LessonFlowColors.SuccessColor.copy(alpha = 0.1f)
                    else -> Color.White
                }
                
                // Determine text color
                val textColor = when {
                    !isAnswered && isSelected -> LessonFlowColors.PrimaryColor
                    answerStatus == AnswerStatus.WRONG && isSelected -> LessonFlowColors.ErrorColor
                    answerStatus == AnswerStatus.CORRECT && isCorrect -> LessonFlowColors.SuccessColor
                    else -> LessonFlowColors.TextPrimary
                }
                
                // Determine border color
                val borderColor = when {
                    !isAnswered && isSelected -> LessonFlowColors.PrimaryColor
                    answerStatus == AnswerStatus.WRONG && isSelected -> LessonFlowColors.ErrorColor
                    answerStatus == AnswerStatus.CORRECT && isCorrect -> LessonFlowColors.SuccessColor
                    else -> AppColors.LightGray
                }
                
                // Option card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isAnswered) {
                            onOptionSelected(option.id)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = backgroundColor
                    ),
                    border = if (isSelected || (isAnswered && isCorrect)) {
                        BorderStroke(2.dp, borderColor)
                    } else {
                        null
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Option indicator
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(borderColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.id.toString().takeLast(1),
                                color = AppColors.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Option text
                        Text(
                            text = option.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Submit or feedback
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isAnswered) {
                // Show feedback
                val feedbackText = if (answerStatus == AnswerStatus.CORRECT) {
                    "✓ Đúng rồi!"
                } else {
                    "✗ Sai rồi"
                }
                val feedbackColor = if (answerStatus == AnswerStatus.CORRECT) LessonFlowColors.SuccessColor else LessonFlowColors.ErrorColor
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(feedbackColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = feedbackText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = feedbackColor
                    )
                }
            } else if (selectedOption != null) {
                // Show submit button
                Button(
                    onClick = {
                        val isCorrect = correctOption == selectedOption
                        onAnswerSubmitted(isCorrect)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LessonFlowColors.PrimaryColor
                    )
                ) {
                    Text(
                        text = "Kiểm tra",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.White
                    )
                }
            }
        }
    }
}

/**
 * Progress indicator showing current question number
 */
@Composable
fun ProgressIndicator(currentPage: Int, totalPages: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(LessonFlowColors.PrimaryColor, LessonFlowColors.SecondaryColor)
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Câu hỏi $currentPage / $totalPages",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.White
        )
    }
}

/**
 * Completion prompt - Ask user if they want to proceed to listening
 */
@Composable
fun CompletionPrompt(
    lessonId: String,
    viewModel: LessonViewModel,
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LessonFlowColors.BackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(LessonFlowColors.PrimaryColor, LessonFlowColors.SecondaryColor)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎉", fontSize = 40.sp)
                }
                
                // Message
                Text(
                    text = "Hoàn thành quiz!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = LessonFlowColors.TextPrimary
                )
                
                Text(
                    text = "Bạn có muốn đến phần nghe không?",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = LessonFlowColors.TextSecondary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNo,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, LessonFlowColors.PrimaryColor)
                    ) {
                        Text(
                            text = "Không",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LessonFlowColors.PrimaryColor
                        )
                    }
                    
                    Button(
                        onClick = onYes,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LessonFlowColors.PrimaryColor
                        )
                    ) {
                        Text(
                            text = "Có",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Loading indicator
 */
@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(LessonFlowColors.PrimaryColor.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            color = LessonFlowColors.PrimaryColor,
            strokeWidth = 3.dp
        )
    }
}

/**
 * Answer status enum
 */
enum class AnswerStatus {
    NONE,   // No answer selected yet
    CORRECT, // Correct answer
    WRONG   // Wrong answer
}


