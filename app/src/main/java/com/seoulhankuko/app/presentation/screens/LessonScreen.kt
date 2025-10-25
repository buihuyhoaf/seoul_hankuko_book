package com.seoulhankuko.app.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.domain.model.AnswerStatus
import com.seoulhankuko.app.domain.model.ChallengeType
import com.seoulhankuko.app.domain.model.ChallengeWithOptions
import com.seoulhankuko.app.presentation.viewmodel.LessonViewModel
import com.seoulhankuko.app.presentation.viewmodel.LessonUiState
import com.seoulhankuko.app.presentation.components.AudioButton
import kotlinx.coroutines.delay

// Soft color palette
private val MintGreen = Color(0xFF98D8C8)
private val PastelBlue = Color(0xFFAED9E0)
private val LightMint = Color(0xFFE8F6F3)
private val SoftWhite = Color(0xFFFAFAFA)
private val AccentGreen = Color(0xFF5EEAD4)
private val SuccessGreen = Color(0xFF10B981)
private val ErrorRed = Color(0xFFEF4444)
private val TextDark = Color(0xFF1F2937)
private val PastelPurple = Color(0xFFDDD6FE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    lessonId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToQuiz: (quizId: Int) -> Unit,
    viewModel: LessonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }
    
    // Observe progress updates and refresh lesson data
    LaunchedEffect(uiState) {
        val currentState = uiState
        if (currentState is LessonUiState.Success && currentState.progressUpdated) {
            // Progress was updated, refresh the lesson data
            viewModel.loadLesson(lessonId)
        }
    }
    
    Scaffold(
        topBar = {
            when (val currentState = uiState) {
                is LessonUiState.Success -> AnimatedTopBar(
                    lessonTitle = currentState.lessonWithChallenges?.lesson?.title ?: "Lesson",
                    currentChallenge = currentState.currentChallengeIndex + 1,
                    totalChallenges = currentState.getTotalChallenges(),
                    hearts = currentState.userProgress?.hearts ?: 5,
                    progressPercentage = currentState.getProgressPercentage(),
                    onNavigateBack = onNavigateBack
                )
                else -> AnimatedTopBar(
                    lessonTitle = "Lesson",
                    currentChallenge = 0,
                    totalChallenges = 0,
                    hearts = 5,
                    onNavigateBack = onNavigateBack
                )
            }
        },
        containerColor = SoftWhite
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val currentState = uiState) {
                is LessonUiState.Loading -> LoadingState()
                is LessonUiState.Error -> ErrorState(
                    message = currentState.message,
                    onRetry = { viewModel.retryLoadLesson(lessonId) }
                )
                is LessonUiState.Success -> {
                    if (currentState.isLessonCompleted) {
                        LessonCompletedState(
                            onTakeQuiz = { onNavigateToQuiz(lessonId) },
                            onRetry = { viewModel.resetLesson() }
                        )
                    } else {
                        ChallengeContent(
                            uiState = currentState,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimatedTopBar(
    lessonTitle: String,
    currentChallenge: Int,
    totalChallenges: Int,
    hearts: Int,
    progressPercentage: Float = 0f,
    onNavigateBack: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    TopAppBar(
        title = {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { -50 })
            ) {
                Column {
                    Text(
                        text = lessonTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    if (totalChallenges > 0) {
                        Text(
                            text = "Challenge $currentChallenge of $totalChallenges",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark.copy(alpha = 0.6f)
                        )
                    }
                    
                    // Progress bar
                    if (progressPercentage > 0f) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progressPercentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = AccentGreen,
                            trackColor = TextDark.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "${(progressPercentage * 100).toInt()}% Complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextDark
                )
            }
        },
        actions = {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + scaleIn()
            ) {
                HeartsDisplay(hearts = hearts)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
private fun HeartsDisplay(hearts: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        repeat(5) { index ->
            val scale by animateFloatAsState(
                targetValue = if (index < hearts) 1f else 0.7f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "heart_scale"
            )
            
            Icon(
                imageVector = if (index < hearts) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Heart",
                tint = if (index < hearts) ErrorRed else Color.Gray,
                modifier = Modifier
                    .size(20.dp)
                    .scale(scale)
            )
        }
    }
}

@Composable
private fun ChallengeContent(
    uiState: LessonUiState.Success,
    viewModel: LessonViewModel
) {
    val scrollState = rememberScrollState()
    val currentChallenge = uiState.getCurrentChallenge()
    
    // Debug logging
    LaunchedEffect(currentChallenge) {
        android.util.Log.d("LessonScreen", "Current challenge: $currentChallenge")
        android.util.Log.d("LessonScreen", "Challenge question: ${currentChallenge?.challenge?.question}")
        android.util.Log.d("LessonScreen", "Challenge options: ${currentChallenge?.options?.map { "${it.id}: ${it.text} (correct: ${it.correct})" }}")
        android.util.Log.d("LessonScreen", "Challenge type: ${currentChallenge?.challenge?.type}")
        android.util.Log.d("LessonScreen", "Challenge type enum: ${currentChallenge?.challenge?.type?.name}")
        
        if (currentChallenge?.challenge?.type == ChallengeType.TRUE_FALSE) {
            android.util.Log.d("LessonScreen", "TRUE_FALSE challenge detected!")
            android.util.Log.d("LessonScreen", "Total options: ${currentChallenge.options.size}")
            currentChallenge.options.forEachIndexed { index, option ->
                android.util.Log.d("LessonScreen", "Option $index: ID=${option.id}, Text='${option.text}', Correct=${option.correct}")
            }
            android.util.Log.d("LessonScreen", "Selected option: ${uiState.selectedOption}")
            
            // Check if True/False options exist
            val trueOption = currentChallenge.options.find { it.text.equals("True", ignoreCase = true) }
            val falseOption = currentChallenge.options.find { it.text.equals("False", ignoreCase = true) }
            android.util.Log.d("LessonScreen", "True option found: $trueOption")
            android.util.Log.d("LessonScreen", "False option found: $falseOption")
        } else {
            android.util.Log.d("LessonScreen", "Not a TRUE_FALSE challenge. Type: ${currentChallenge?.challenge?.type}")
        }
    }
    
    var contentVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(uiState.currentChallengeIndex) {
        contentVisible = false
        delay(100)
        contentVisible = true
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress Bar
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -20 })
        ) {
            ProgressBar(
                progress = uiState.getProgressPercentage(),
                currentChallenge = uiState.currentChallengeIndex + 1,
                totalChallenges = uiState.getTotalChallenges()
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Challenge Card
        if (currentChallenge != null) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 100)) + 
                        slideInVertically(initialOffsetY = { 30 })
            ) {
                when (currentChallenge.challenge.type) {
                    ChallengeType.MULTIPLE_CHOICE -> MultipleChoiceChallengeCard(
                        challenge = currentChallenge,
                        selectedOption = uiState.selectedOption,
                        status = uiState.status,
                        onSelectOption = viewModel::selectOption
                    )
                    ChallengeType.FILL_IN_BLANK -> FillInBlankChallengeCard(
                        challenge = currentChallenge,
                        selectedOption = uiState.selectedOption,
                        status = uiState.status,
                        onSelectOption = viewModel::selectOption
                    )
                    ChallengeType.TRUE_FALSE -> TrueFalseChallengeCard(
                        challenge = currentChallenge,
                        selectedOption = uiState.selectedOption,
                        status = uiState.status,
                        onSelectOption = viewModel::selectOption
                    )
                    ChallengeType.AUDIO_COMPREHENSION -> AudioComprehensionChallengeCard(
                        challenge = currentChallenge,
                        selectedOption = uiState.selectedOption,
                        status = uiState.status,
                        onSelectOption = viewModel::selectOption
                    )
                    ChallengeType.WRITING_PRACTICE -> WritingPracticeChallengeCard(
                        challenge = currentChallenge,
                        selectedOption = uiState.selectedOption,
                        status = uiState.status,
                        onSelectOption = viewModel::selectOption
                    )
                    ChallengeType.READING_COMPREHENSION -> ReadingComprehensionChallengeCard(
                        challenge = currentChallenge,
                        selectedOption = uiState.selectedOption,
                        status = uiState.status,
                        onSelectOption = viewModel::selectOption
                    )
                    ChallengeType.MATCHING -> MatchingChallengeCard(
                        challenge = currentChallenge,
                        selectedOption = uiState.selectedOption,
                        status = uiState.status,
                        onSelectOption = viewModel::selectOption
                    )
                    ChallengeType.PRONUNCIATION -> PronunciationChallengeCard(
                        challenge = currentChallenge,
                        selectedOption = uiState.selectedOption,
                        status = uiState.status,
                        onSelectOption = viewModel::selectOption
                    )
                }
            }
        } else {
            // Debug: Show when no challenges
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 100)) + 
                        slideInVertically(initialOffsetY = { 30 })
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Challenges Available",
                            style = MaterialTheme.typography.titleMedium,
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This lesson doesn't have any questions yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextDark.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Total challenges: ${uiState.getTotalChallenges()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Current index: ${uiState.currentChallengeIndex}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action Buttons
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + 
                    slideInVertically(initialOffsetY = { 20 })
        ) {
            ActionButtons(
                status = uiState.status,
                hasSelectedOption = uiState.selectedOption != null,
                isLastChallenge = uiState.isLastChallenge(),
                canProceedAfterWrong = uiState.canProceedAfterWrong,
                onSubmit = viewModel::submitAnswer,
                onContinue = viewModel::nextChallenge,
                onRetry = viewModel::retryChallenge,
                onProceedAfterWrong = viewModel::proceedAfterWrongAnswer
            )
        }
    }
}

@Composable
private fun ProgressBar(
    progress: Float,
    currentChallenge: Int,
    totalChallenges: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "progress"
    )
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.labelMedium,
                color = TextDark.copy(alpha = 0.6f)
            )
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AccentGreen
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(5.dp)
                ),
            color = AccentGreen,
            trackColor = LightMint,
        )
    }
}

@Composable
private fun MultipleChoiceChallengeCard(
    challenge: ChallengeWithOptions,
    selectedOption: Int?,
    status: AnswerStatus,
    onSelectOption: (Int) -> Unit
) {
    ChallengeCardContainer(
        title = "Select the Correct Answer",
        icon = "🎯",
        iconColor = PastelBlue
    ) {
        Text(
            text = challenge.challenge.question,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            lineHeight = 32.sp
        )
        
        // Hint text for answer selection
        if (status == AnswerStatus.NONE) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to select your answer",
                style = MaterialTheme.typography.bodyMedium,
                color = TextDark.copy(alpha = 0.6f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        challenge.options.forEachIndexed { index, option ->
            val isSelected = selectedOption == option.id
            val isCorrect = option.correct
            val showResult = status != AnswerStatus.NONE
            
            AnimatedOptionButton(
                text = option.text,
                audioSrc = option.audioSrc,
                isSelected = isSelected,
                isCorrect = isCorrect,
                showResult = showResult,
                index = index,
                onClick = { if (!showResult) onSelectOption(option.id) }
            )
            
            if (index < challenge.options.size - 1) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        // Feedback
        AnimatedVisibility(
            visible = status != AnswerStatus.NONE,
            enter = fadeIn() + expandVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(20.dp))
                FeedbackCard(
                    status = status,
                    correctAnswer = challenge.options.find { it.correct }?.text
                )
            }
        }
    }
}

@Composable
private fun FillInBlankChallengeCard(
    challenge: ChallengeWithOptions,
    selectedOption: Int?,
    status: AnswerStatus,
    onSelectOption: (Int) -> Unit
) {
    var userAnswer by remember { mutableStateOf("") }
    
    ChallengeCardContainer(
        title = "Complete the Word",
        icon = "✨",
        iconColor = PastelPurple
    ) {
        Text(
            text = challenge.challenge.question,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            lineHeight = 32.sp
        )
        
        // Hint text for answer selection
        if (status == AnswerStatus.NONE) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to select your answer",
                style = MaterialTheme.typography.bodyMedium,
                color = TextDark.copy(alpha = 0.6f)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Word building interface
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            challenge.options.forEach { option ->
                val isSelected = selectedOption == option.id
                val showResult = status != AnswerStatus.NONE
                
                WordChipButton(
                    text = option.text,
                    isSelected = isSelected,
                    isCorrect = option.correct,
                    showResult = showResult,
                    onClick = { 
                        if (!showResult) {
                            onSelectOption(option.id)
                            userAnswer = option.text
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Answer display
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            color = LightMint,
            border = BorderStroke(
                2.dp,
                if (status == AnswerStatus.CORRECT) SuccessGreen 
                else if (status == AnswerStatus.WRONG) ErrorRed 
                else AccentGreen
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userAnswer.ifEmpty { "Select an answer..." },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (userAnswer.isEmpty()) TextDark.copy(alpha = 0.3f) else AccentGreen
                )
            }
        }
        
        // Feedback
        AnimatedVisibility(
            visible = status != AnswerStatus.NONE,
            enter = fadeIn() + expandVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(20.dp))
                FeedbackCard(
                    status = status,
                    correctAnswer = challenge.options.find { it.correct }?.text
                )
            }
        }
    }
}

@Composable
private fun TrueFalseChallengeCard(
    challenge: ChallengeWithOptions,
    selectedOption: Int?,
    status: AnswerStatus,
    onSelectOption: (Int) -> Unit
) {
    ChallengeCardContainer(
        title = "True or False",
        icon = "🎯",
        iconColor = AccentGreen
    ) {
        // Question text with modern styling
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LightMint),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Question icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = AccentGreen.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "❓",
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 24.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = challenge.challenge.question,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    lineHeight = 32.sp,
                    textAlign = TextAlign.Center
                )
                
                // Hint text for answer selection
                if (status == AnswerStatus.NONE) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tap TRUE or FALSE to answer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AccentGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // True/False buttons with improved design
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choose your answer:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextDark.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Debug logging for True/False options
            LaunchedEffect(challenge.options) {
                android.util.Log.d("TrueFalseChallengeCard", "Available options: ${challenge.options.map { "${it.id}: ${it.text} (correct: ${it.correct})" }}")
                android.util.Log.d("TrueFalseChallengeCard", "Options count: ${challenge.options.size}")
                android.util.Log.d("TrueFalseChallengeCard", "Selected option: $selectedOption")
                android.util.Log.d("TrueFalseChallengeCard", "Status: $status")
            }
            
            // Find True/False options - try multiple variations
            val trueOption = challenge.options.find { option ->
                option.text.equals("True", ignoreCase = true) || 
                option.text.equals("true", ignoreCase = true) ||
                option.text.equals("TRUE", ignoreCase = true)
            }
            val falseOption = challenge.options.find { option ->
                option.text.equals("False", ignoreCase = true) || 
                option.text.equals("false", ignoreCase = true) ||
                option.text.equals("FALSE", ignoreCase = true)
            }
            
            // If we can't find True/False options, use the first two options
            val finalTrueOption = trueOption ?: challenge.options.getOrNull(0)
            val finalFalseOption = falseOption ?: challenge.options.getOrNull(1)
            
            val isCorrectAnswerTrue = if (finalTrueOption != null && finalFalseOption != null) {
                finalTrueOption.correct
            } else {
                // Fallback: determine based on correct answer text
                val correctOption = challenge.options.find { it.correct }
                correctOption?.text?.let { 
                    it.contains("Hello", ignoreCase = true) || 
                    it.contains("Thank you", ignoreCase = true) ||
                    it.contains("안녕하세요", ignoreCase = true) ||
                    it.contains("감사합니다", ignoreCase = true)
                } ?: false
            }
            
            // Get actual option IDs for True/False buttons
            val trueOptionId = finalTrueOption?.id ?: challenge.options.firstOrNull()?.id ?: 1
            val falseOptionId = finalFalseOption?.id ?: challenge.options.getOrNull(1)?.id ?: 2
            
            // Always show True/False buttons, even if options don't exist
            android.util.Log.d("TrueFalseChallengeCard", "About to render buttons. Options empty: ${challenge.options.isEmpty()}")
            if (challenge.options.isNotEmpty()) {
                android.util.Log.d("TrueFalseChallengeCard", "Rendering True/False buttons with ${challenge.options.size} options")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // True Button with enhanced design
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                status != AnswerStatus.NONE && isCorrectAnswerTrue -> SuccessGreen
                                status != AnswerStatus.NONE && selectedOption == trueOptionId && !isCorrectAnswerTrue -> ErrorRed
                                selectedOption == trueOptionId -> AccentGreen
                                else -> SuccessGreen.copy(alpha = 0.8f)
                            }
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (selectedOption == trueOptionId) 8.dp else 4.dp
                        ),
                        onClick = { 
                            if (status == AnswerStatus.NONE) {
                                android.util.Log.d("TrueFalseChallengeCard", "True button clicked, optionId: $trueOptionId")
                                onSelectOption(trueOptionId)
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "TRUE",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    
                    // False Button with enhanced design
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                status != AnswerStatus.NONE && !isCorrectAnswerTrue -> SuccessGreen
                                status != AnswerStatus.NONE && selectedOption == falseOptionId && isCorrectAnswerTrue -> ErrorRed
                                selectedOption == falseOptionId -> AccentGreen
                                else -> ErrorRed.copy(alpha = 0.8f)
                            }
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (selectedOption == falseOptionId) 8.dp else 4.dp
                        ),
                        onClick = { 
                            if (status == AnswerStatus.NONE) {
                                android.util.Log.d("TrueFalseChallengeCard", "False button clicked, optionId: $falseOptionId")
                                onSelectOption(falseOptionId)
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "FALSE",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                // Fallback: Show error message if no options available
                android.util.Log.w("TrueFalseChallengeCard", "No options available! Showing fallback message")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No options available for this True/False question",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErrorRed,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Available options: ${challenge.options.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        
        // Feedback
        AnimatedVisibility(
            visible = status != AnswerStatus.NONE,
            enter = fadeIn() + expandVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(20.dp))
                FeedbackCard(
                    status = status,
                    correctAnswer = challenge.options.find { it.correct }?.text ?: ""
                )
            }
        }
    }
}

@Composable
private fun ImprovedTrueFalseButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    isCorrect: Boolean,
    showResult: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    textColor: Color
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )
    
    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = if (isSelected && !showResult) 1.02f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val finalScale = scale * pulseScale
    
    val borderColor = when {
        showResult && isCorrect -> SuccessGreen
        showResult && isSelected && !isCorrect -> ErrorRed
        isSelected -> AccentGreen
        else -> backgroundColor.copy(alpha = 0.7f)
    }
    
    val cardColor = when {
        showResult && isCorrect -> SuccessGreen.copy(alpha = 0.9f)
        showResult && isSelected && !isCorrect -> ErrorRed.copy(alpha = 0.9f)
        isSelected -> backgroundColor.copy(alpha = 0.9f)
        else -> backgroundColor.copy(alpha = 0.8f)
    }
    
    val finalTextColor = when {
        showResult && isCorrect -> Color.White
        showResult && isSelected && !isCorrect -> Color.White
        isSelected -> Color.White
        else -> textColor
    }
    
    Card(
        modifier = modifier
            .height(120.dp)
            .scale(finalScale)
            .shadow(
                elevation = if (isSelected) 16.dp else 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = backgroundColor.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(
            width = if (isSelected) 4.dp else 2.dp,
            color = borderColor
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon with enhanced styling
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = finalTextColor
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Text with enhanced styling
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = finalTextColor,
                letterSpacing = 1.sp
            )
            
            // Visual feedback
            if (isSelected && !showResult) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Result feedback
            if (showResult) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isCorrect) "✓" else "✗",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AudioComprehensionChallengeCard(
    challenge: ChallengeWithOptions,
    selectedOption: Int?,
    status: AnswerStatus,
    onSelectOption: (Int) -> Unit
) {
    ChallengeCardContainer(
        title = "Audio Comprehension",
        icon = "🎧",
        iconColor = PastelBlue
    ) {
        Text(
            text = challenge.challenge.question,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            lineHeight = 32.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Audio player and options
        challenge.options.forEachIndexed { index, option ->
            val isSelected = selectedOption == option.id
            val isCorrect = option.correct
            val showResult = status != AnswerStatus.NONE
            
            AnimatedOptionButton(
                text = option.text,
                audioSrc = option.audioSrc,
                isSelected = isSelected,
                isCorrect = isCorrect,
                showResult = showResult,
                index = index,
                onClick = { if (!showResult) onSelectOption(option.id) }
            )
            
            if (index < challenge.options.size - 1) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        // Feedback
        AnimatedVisibility(
            visible = status != AnswerStatus.NONE,
            enter = fadeIn() + expandVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(20.dp))
                FeedbackCard(
                    status = status,
                    correctAnswer = challenge.options.find { it.correct }?.text
                )
            }
        }
    }
}

@Composable
private fun WritingPracticeChallengeCard(
    challenge: ChallengeWithOptions,
    selectedOption: Int?,
    status: AnswerStatus,
    onSelectOption: (Int) -> Unit
) {
    ChallengeCardContainer(
        title = "Writing Practice",
        icon = "✍️",
        iconColor = AccentGreen
    ) {
        Text(
            text = challenge.challenge.question,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            lineHeight = 32.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Writing practice feature coming soon!",
            style = MaterialTheme.typography.bodyLarge,
            color = TextDark.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReadingComprehensionChallengeCard(
    challenge: ChallengeWithOptions,
    selectedOption: Int?,
    status: AnswerStatus,
    onSelectOption: (Int) -> Unit
) {
    ChallengeCardContainer(
        title = "Reading Comprehension",
        icon = "📖",
        iconColor = PastelPurple
    ) {
        Text(
            text = challenge.challenge.question,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            lineHeight = 32.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        challenge.options.forEachIndexed { index, option ->
            val isSelected = selectedOption == option.id
            val isCorrect = option.correct
            val showResult = status != AnswerStatus.NONE
            
            AnimatedOptionButton(
                text = option.text,
                audioSrc = option.audioSrc,
                isSelected = isSelected,
                isCorrect = isCorrect,
                showResult = showResult,
                index = index,
                onClick = { if (!showResult) onSelectOption(option.id) }
            )
            
            if (index < challenge.options.size - 1) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        // Feedback
        AnimatedVisibility(
            visible = status != AnswerStatus.NONE,
            enter = fadeIn() + expandVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(20.dp))
                FeedbackCard(
                    status = status,
                    correctAnswer = challenge.options.find { it.correct }?.text
                )
            }
        }
    }
}

@Composable
private fun MatchingChallengeCard(
    challenge: ChallengeWithOptions,
    selectedOption: Int?,
    status: AnswerStatus,
    onSelectOption: (Int) -> Unit
) {
    ChallengeCardContainer(
        title = "Matching",
        icon = "🔗",
        iconColor = MintGreen
    ) {
        Text(
            text = challenge.challenge.question,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            lineHeight = 32.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Matching feature coming soon!",
            style = MaterialTheme.typography.bodyLarge,
            color = TextDark.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PronunciationChallengeCard(
    challenge: ChallengeWithOptions,
    selectedOption: Int?,
    status: AnswerStatus,
    onSelectOption: (Int) -> Unit
) {
    ChallengeCardContainer(
        title = "Pronunciation",
        icon = "🎤",
        iconColor = ErrorRed
    ) {
        Text(
            text = challenge.challenge.question,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            lineHeight = 32.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Pronunciation practice feature coming soon!",
            style = MaterialTheme.typography.bodyLarge,
            color = TextDark.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ChallengeCardContainer(
    title: String,
    icon: String,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.2f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = icon,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            content()
        }
    }
}

@Composable
private fun AnimatedOptionButton(
    text: String,
    audioSrc: String?,
    isSelected: Boolean,
    isCorrect: Boolean,
    showResult: Boolean,
    index: Int,
    onClick: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    
    LaunchedEffect(isSelected) {
        if (isSelected) {
            scale = 0.95f
            delay(100)
            scale = 1f
        }
    }
    
    val backgroundColor = when {
        showResult && isSelected && isCorrect -> SuccessGreen.copy(alpha = 0.15f)
        showResult && isSelected && !isCorrect -> ErrorRed.copy(alpha = 0.15f)
        showResult && isCorrect -> SuccessGreen.copy(alpha = 0.15f)
        isSelected -> AccentGreen.copy(alpha = 0.2f)
        else -> LightMint
    }
    
    val borderColor = when {
        showResult && isSelected && isCorrect -> SuccessGreen
        showResult && isSelected && !isCorrect -> ErrorRed
        showResult && isCorrect -> SuccessGreen
        isSelected -> AccentGreen
        else -> Color.Transparent
    }
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(
            width = if (isSelected || (showResult && isCorrect)) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected || (showResult && isCorrect)) 
                        borderColor.copy(alpha = 0.2f) 
                    else 
                        Color.Gray.copy(alpha = 0.1f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ('A' + index).toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected || (showResult && isCorrect)) borderColor else TextDark.copy(alpha = 0.5f)
                        )
                    }
                }
                
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = TextDark
                )
            }
            
            // Audio button if available
            audioSrc?.let {
                AudioButton(
                    audioSrc = audioSrc,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            // Result icon
            if (showResult && (isSelected || isCorrect)) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isCorrect) SuccessGreen else ErrorRed,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun WordChipButton(
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    showResult: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chip_scale"
    )
    
    val backgroundColor = when {
        showResult && isCorrect -> SuccessGreen
        showResult && isSelected && !isCorrect -> ErrorRed
        isSelected -> AccentGreen
        else -> PastelPurple.copy(alpha = 0.5f)
    }
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .scale(scale)
            .size(80.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = if (isSelected) 8.dp else 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun FeedbackCard(
    status: AnswerStatus,
    correctAnswer: String?
) {
    val backgroundColor = if (status == AnswerStatus.CORRECT) 
        SuccessGreen.copy(alpha = 0.1f) 
    else 
        ErrorRed.copy(alpha = 0.1f)
    
    val borderColor = if (status == AnswerStatus.CORRECT) SuccessGreen else ErrorRed
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (status == AnswerStatus.CORRECT) 
                    Icons.Default.CheckCircle 
                else 
                    Icons.Default.Close,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(32.dp)
            )
            
            Column {
                Text(
                    text = if (status == AnswerStatus.CORRECT) 
                        "Perfect! 🎉" 
                    else 
                        "Not quite right",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = borderColor
                )
                
                if (status == AnswerStatus.WRONG && correctAnswer != null) {
                    Text(
                        text = "Correct answer: $correctAnswer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDark.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    status: AnswerStatus,
    hasSelectedOption: Boolean,
    isLastChallenge: Boolean,
    canProceedAfterWrong: Boolean,
    onSubmit: () -> Unit,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    onProceedAfterWrong: () -> Unit
) {
    when (status) {
        AnswerStatus.NONE -> {
            GradientButton(
                text = "Check Answer",
                icon = Icons.Default.Check,
                enabled = hasSelectedOption,
                onClick = onSubmit,
                gradient = listOf(AccentGreen, MintGreen)
            )
        }
        AnswerStatus.CORRECT -> {
            GradientButton(
                text = if (isLastChallenge) "Complete Lesson" else "Continue",
                icon = Icons.Default.ArrowForward,
                enabled = true,
                onClick = onContinue,
                gradient = listOf(SuccessGreen, AccentGreen)
            )
        }
        AnswerStatus.WRONG -> {
            if (canProceedAfterWrong) {
                // Show both Try Again and Continue buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GradientButton(
                        text = "Try Again",
                        icon = Icons.Default.Refresh,
                        enabled = true,
                        onClick = onRetry,
                        gradient = listOf(ErrorRed, Color(0xFFFB923C)),
                        modifier = Modifier.weight(1f)
                    )
                    GradientButton(
                        text = "Continue",
                        icon = Icons.Default.ArrowForward,
                        enabled = true,
                        onClick = onProceedAfterWrong,
                        gradient = listOf(AccentGreen, MintGreen),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Show only Try Again button
                GradientButton(
                    text = "Try Again",
                    icon = Icons.Default.Refresh,
                    enabled = true,
                    onClick = onRetry,
                    gradient = listOf(ErrorRed, Color(0xFFFB923C))
                )
            }
        }
    }
}

@Composable
private fun GradientButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.95f,
        label = "button_scale"
    )
    
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Gray.copy(alpha = 0.2f)
        ),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (enabled) 4.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) {
                        Brush.horizontalGradient(gradient)
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                Color.Gray.copy(alpha = 0.3f),
                                Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = AccentGreen,
                strokeWidth = 4.dp,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "Loading lesson...",
                style = MaterialTheme.typography.titleMedium,
                color = TextDark.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Oops!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextDark.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Try Again")
                }
            }
        }
    }
}

@Composable
private fun LessonCompletedState(
    onTakeQuiz: () -> Unit,
    onRetry: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(200)
        visible = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(initialScale = 0.8f)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Animated trophy
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
                        text = "🏆",
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.graphicsLayer(rotationZ = rotation)
                    )
                    
                    Text(
                        text = "Lesson Complete!",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    
                    Text(
                        text = "Great job! You've mastered all the challenges.\nReady for the quiz?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextDark.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    GradientButton(
                        text = "Take Quiz",
                        icon = Icons.Default.PlayArrow,
                        enabled = true,
                        onClick = onTakeQuiz,
                        gradient = listOf(AccentGreen, MintGreen, PastelBlue)
                    )
                    
                    TextButton(onClick = onRetry) {
                        Text(
                            text = "Practice Again",
                            color = TextDark.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
