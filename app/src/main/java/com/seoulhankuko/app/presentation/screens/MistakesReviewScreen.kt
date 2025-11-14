package com.seoulhankuko.app.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.data.api.model.QuestionResponse
import com.seoulhankuko.app.domain.model.QuestionType
import com.seoulhankuko.app.presentation.components.MatchingQuestion
import com.seoulhankuko.app.presentation.components.MatchingQuestionCard
import com.seoulhankuko.app.presentation.components.PronunciationEvaluationUiState
import com.seoulhankuko.app.presentation.components.PronunciationQuestionCard
import com.seoulhankuko.app.presentation.components.rememberSoundManager
import com.seoulhankuko.app.presentation.utils.LessonColors
import com.seoulhankuko.app.presentation.utils.LessonFlowColors
import kotlin.math.max
import com.seoulhankuko.app.presentation.viewmodel.GoogleSignInViewModel
import com.seoulhankuko.app.presentation.viewmodel.MistakesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakesReviewScreen(
    onNavigateBack: () -> Unit,
    mistakesViewModel: MistakesViewModel = hiltViewModel(),
    googleSignInViewModel: GoogleSignInViewModel = hiltViewModel()
) {
    val mistakes by mistakesViewModel.mistakes.collectAsStateWithLifecycle()
    val questionDetails by mistakesViewModel.questionDetails.collectAsStateWithLifecycle()
    val challenges by mistakesViewModel.challenges.collectAsStateWithLifecycle()
    val isLoading by mistakesViewModel.isLoading.collectAsStateWithLifecycle()
    val error by mistakesViewModel.error.collectAsStateWithLifecycle()
    val totalMistakes by mistakesViewModel.totalMistakes.collectAsStateWithLifecycle()
    
    val userData by googleSignInViewModel.userData.collectAsStateWithLifecycle()
    val username = userData.username?.takeIf { it.isNotBlank() } ?: ""
    val token = userData.accessToken
    
    var showExitDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val soundManager = rememberSoundManager()
    
    // Load mistakes on start
    LaunchedEffect(username) {
        if (username.isNotBlank()) {
            mistakesViewModel.loadMistakes(
                username = username,
                token = token
            )
        }
    }
    
    BackHandler(enabled = true) {
        if (mistakes.isNotEmpty()) {
            showExitDialog = true
        } else {
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ôn tập lỗi sai",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LessonColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (mistakes.isNotEmpty()) {
                            showExitDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = LessonColors.Accent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = LessonColors.TextPrimary,
                    navigationIconContentColor = LessonColors.Accent
                )
            )
        },
        containerColor = LessonColors.BackgroundWhite
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LessonColors.BackgroundWhite)
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LessonColors.BackgroundWhite)
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Không thể tải danh sách lỗi sai",
                            style = MaterialTheme.typography.titleLarge,
                            color = LessonFlowColors.ErrorColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error ?: "Đã xảy ra lỗi",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            mistakes.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LessonColors.BackgroundWhite)
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "🎉",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = "Bạn chưa có lỗi sai nào",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tiếp tục học tập để cải thiện kỹ năng nhé!",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = LessonFlowColors.TextSecondary
                        )
                    }
                }
            }
            else -> {
                MistakesPagerFlow(
                    mistakes = mistakes,
                    questionDetails = questionDetails,
                    challenges = challenges,
                    totalMistakes = totalMistakes,
                    onNavigateBack = onNavigateBack,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
    
    if (showExitDialog) {
        ConfirmExitDialog(
            onDismiss = { showExitDialog = false },
            onConfirmExit = {
                showExitDialog = false
                onNavigateBack()
            }
        )
    }
}

@Composable
private fun MistakesPagerFlow(
    mistakes: List<com.seoulhankuko.app.data.api.model.MistakeResponse>,
    questionDetails: Map<String, QuestionResponse>,
    challenges: Map<String, com.seoulhankuko.app.domain.model.ChallengeWithOptions>,
    totalMistakes: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { max(mistakes.size, 1) })
    var currentAnswerStatus by remember { mutableStateOf<AnswerStatus>(AnswerStatus.NONE) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var matchingCompleted by remember { mutableStateOf(false) }
    val soundManager = rememberSoundManager()
    
    // Reset state when page changes
    LaunchedEffect(pagerState.currentPage) {
        currentAnswerStatus = AnswerStatus.NONE
        selectedOption = null
        isSubmitted = false
        matchingCompleted = false
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LessonColors.BackgroundWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 96.dp)
        ) {
            // Progress indicator
            Text(
                text = "Câu ${pagerState.currentPage + 1} / ${mistakes.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = LessonFlowColors.TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                textAlign = TextAlign.Center
            )
            
            // Pager with mistakes
            if (mistakes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có lỗi sai nào",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LessonFlowColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    pageSpacing = 16.dp,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    userScrollEnabled = false  // ✅ DISABLE SWIPE
                ) { page ->
                    val mistake = mistakes[page]
                    val questionResponse = questionDetails[mistake.questionId]
                    val challenge = challenges[mistake.questionId]
                    val isCurrentPage = page == pagerState.currentPage
                    val pageAnswerStatus = if (isCurrentPage) currentAnswerStatus else AnswerStatus.NONE
                    val pageSelectedOption = if (isCurrentPage) selectedOption else null
                    
                    if (challenge != null && questionResponse != null) {
                        val isBlankQuestion = challenge.challenge.type == QuestionType.BLANK && questionResponse != null
                        val isMatchingQuestion = challenge.challenge.type == QuestionType.MATCHING && questionResponse != null
                        val isPronunciationQuestion = challenge.challenge.type == QuestionType.PRONUNCIATION && questionResponse != null
                        
                        // Show mistake info card
                        MistakeInfoCard(
                            mistake = mistake,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Render question card based on type
                        when {
                            isBlankQuestion -> {
                                FillInBlankQuestionCard(
                                    challenge = challenge,
                                    question = questionResponse,
                                    questionIndex = page,
                                    selectedOption = pageSelectedOption,
                                    answerStatus = pageAnswerStatus,
                                    onOptionSelected = { optionId ->
                                        if (!isCurrentPage || currentAnswerStatus != AnswerStatus.NONE) return@FillInBlankQuestionCard
                                        selectedOption = optionId
                                    },
                                    onAnswerSubmitted = { isCorrect ->
                                        if (!isCurrentPage) return@FillInBlankQuestionCard
                                        currentAnswerStatus = if (isCorrect) AnswerStatus.CORRECT else AnswerStatus.WRONG
                                        isSubmitted = true
                                        if (isCorrect) {
                                            soundManager.playCorrect()
                                        } else {
                                            soundManager.playIncorrect()
                                        }
                                    },
                                    onPlayAudio = {}
                                )
                            }
                            isMatchingQuestion -> {
                                val metadataPairs = questionResponse.metadata?.pairs
                                    ?.mapNotNull { pair ->
                                        pair.left?.let { left ->
                                            pair.right?.let { right ->
                                                left to right
                                            }
                                        }
                                    }
                                val fallbackPairs = challenge.matchingPairs
                                    ?.mapNotNull { pair ->
                                        val left = pair.leftText
                                        val right = pair.rightText
                                        if (left.isNullOrBlank() && right.isNullOrBlank()) {
                                            null
                                        } else {
                                            left.orEmpty() to right.orEmpty()
                                        }
                                    }
                                val pairsForUi = when {
                                    !metadataPairs.isNullOrEmpty() -> metadataPairs
                                    !fallbackPairs.isNullOrEmpty() -> fallbackPairs
                                    else -> emptyList()
                                }
                                
                                if (pairsForUi.isNotEmpty()) {
                                    MatchingQuestionCard(
                                        question = MatchingQuestion(
                                            content = challenge.challenge.question,
                                            pairs = pairsForUi,
                                            explanation = questionResponse.explanation.orEmpty()
                                        ),
                                        onMatchCompleted = { isCompleted ->
                                            if (!isCurrentPage) return@MatchingQuestionCard
                                            matchingCompleted = isCompleted
                                            if (isCompleted) {
                                                currentAnswerStatus = AnswerStatus.CORRECT
                                                isSubmitted = true
                                                soundManager.playCorrect()
                                            }
                                        },
                                        onMatchFeedback = { isCorrect ->
                                            if (!isCurrentPage) return@MatchingQuestionCard
                                            if (isCorrect) {
                                                soundManager.playCorrect()
                                            } else {
                                                soundManager.playIncorrect()
                                            }
                                        }
                                    )
                                }
                            }
                            isPronunciationQuestion -> {
                                val pronunciationData = challenge.pronunciation ?: questionResponse.pronunciation?.let {
                                    com.seoulhankuko.app.domain.model.QuestionPronunciation(
                                        id = it.id,
                                        targetPhrase = it.targetPhrase,
                                        referenceAudioUrl = it.referenceAudioUrl
                                    )
                                }
                                
                                PronunciationQuestionCard(
                                    prompt = pronunciationData?.targetPhrase ?: challenge.challenge.question,
                                    explanation = questionResponse.explanation,
                                    referenceAudioUrl = pronunciationData?.referenceAudioUrl,
                                    evaluationState = PronunciationEvaluationUiState(),
                                    isProcessing = false,
                                    onRecordStart = { false },
                                    onRecordStop = {},
                                    onRetry = {
                                        if (!isCurrentPage) return@PronunciationQuestionCard
                                        currentAnswerStatus = AnswerStatus.NONE
                                        isSubmitted = false
                                    },
                                    onListenNative = {}
                                )
                            }
                            else -> {
                                QuizQuestionCard(
                                    challenge = challenge,
                                    selectedOption = pageSelectedOption,
                                    answerStatus = pageAnswerStatus,
                                    onOptionSelected = { optionId ->
                                        if (!isCurrentPage || currentAnswerStatus != AnswerStatus.NONE) return@QuizQuestionCard
                                        selectedOption = optionId
                                    },
                                    onAnswerSubmitted = { isCorrect ->
                                        if (!isCurrentPage) return@QuizQuestionCard
                                        currentAnswerStatus = if (isCorrect) AnswerStatus.CORRECT else AnswerStatus.WRONG
                                        isSubmitted = true
                                        if (isCorrect) {
                                            soundManager.playCorrect()
                                        } else {
                                            soundManager.playIncorrect()
                                        }
                                    },
                                    questionIndex = page,
                                    onPlayAudio = {}
                                )
                            }
                        }
                    } else {
                        // Fallback: Show simple mistake card if question details not loaded
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = mistake.questionContent,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = LessonFlowColors.TextPrimary
                                )
                                if (!mistake.lastWrongAnswer.isNullOrBlank()) {
                                    Text(
                                        text = "Câu trả lời sai: ${mistake.lastWrongAnswer}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LessonFlowColors.ErrorColor
                                    )
                                }
                                if (!mistake.explanation.isNullOrBlank()) {
                                    Text(
                                        text = "💡 Giải thích: ${mistake.explanation}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LessonFlowColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Button "Kiểm tra" / "Tiếp tục"
        val currentPage = pagerState.currentPage
        val currentMistake = mistakes.getOrNull(currentPage)
        val currentChallenge = currentMistake?.let { challenges[it.questionId] }
        val currentQuestionResponse = currentMistake?.let { questionDetails[it.questionId] }
        val isMatchingQuestion = currentChallenge?.challenge?.type == QuestionType.MATCHING
        val isPronunciationQuestion = currentChallenge?.challenge?.type == QuestionType.PRONUNCIATION
        
        fun isSelectionCorrect(selection: String?): Boolean {
            if (selection.isNullOrEmpty() || currentChallenge == null) return false
            val correctOption = currentChallenge.options.firstOrNull { it.correct }
            val metadataChoices = currentQuestionResponse?.metadata?.choices
            val blankAnswer = currentQuestionResponse?.blank?.correctAnswer
            val isCaseSensitive = currentQuestionResponse?.blank?.caseSensitive == true
            val responseCorrectOptions = currentQuestionResponse?.options?.filter { it.isCorrect }.orEmpty()
            val selectedChallengeOption = currentChallenge.options.firstOrNull { it.id == selection }

            fun normalize(input: String?) = input
                ?.trim()
                ?.replace("\u00A0", " ")
                ?.replace("\\s+".toRegex(), " ")
                ?.let { if (isCaseSensitive) it else it.lowercase() }

            val normalizedSelectionId = normalize(selection)
            val normalizedSelectionText = normalize(selectedChallengeOption?.text ?: selection)

            if (!blankAnswer.isNullOrBlank()) {
                val normalizedAnswer = normalize(blankAnswer)
                val normalizedCorrectId = normalize(correctOption?.id)
                if (normalizedSelectionText == normalizedAnswer ||
                    normalizedSelectionId == normalizedAnswer ||
                    normalizedSelectionId == normalizedCorrectId ||
                    normalizedSelectionText == normalizedCorrectId
                ) {
                    return true
                }
            }

            if (responseCorrectOptions.isNotEmpty()) {
                val matchesResponseOption = responseCorrectOptions.any { option ->
                    val normalizedId = normalize(option.id)
                    val normalizedText = normalize(option.optionText)
                    normalizedSelectionId == normalizedId ||
                        normalizedSelectionText == normalizedText ||
                        normalizedSelectionId == normalizedText ||
                        normalizedSelectionText == normalizedId
                }
                if (matchesResponseOption) {
                    return true
                }
            }

            if (correctOption != null) {
                val expectedText = normalize(correctOption.text)
                val expectedId = normalize(correctOption.id)
                if (normalizedSelectionText == expectedText ||
                    normalizedSelectionId == expectedText ||
                    normalizedSelectionId == expectedId ||
                    normalizedSelectionText == expectedId
                ) {
                    return true
                }
            }

            if (!metadataChoices.isNullOrEmpty()) {
                val candidates = mutableSetOf<String>()
                correctOption?.id?.let { candidates.add(it) }
                correctOption?.text?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
                blankAnswer?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
                if (candidates.isNotEmpty()) {
                    val hasMatch = candidates.any { candidate ->
                        val normalizedCandidate = normalize(candidate)
                        normalizedCandidate == normalizedSelectionId || normalizedCandidate == normalizedSelectionText
                    }
                    if (hasMatch) {
                        return true
                    }
                }
            }

            return false
        }
        
        val buttonEnabled = when {
            isMatchingQuestion -> matchingCompleted || isSubmitted
            isPronunciationQuestion -> currentAnswerStatus == AnswerStatus.CORRECT || isSubmitted
            currentAnswerStatus == AnswerStatus.NONE -> selectedOption != null
            else -> true
        }
        
        val buttonText = when {
            isMatchingQuestion && !matchingCompleted && !isSubmitted -> "Hoàn thành"
            !isSubmitted -> "Kiểm tra"
            currentPage == mistakes.lastIndex -> "Hoàn thành"
            else -> "Tiếp tục"
        }
        
        if (buttonEnabled) {
            Button(
                onClick = {
                    if (!isSubmitted) {
                        // Check answer
                        val isCorrect = isSelectionCorrect(selectedOption)
                        currentAnswerStatus = if (isCorrect) AnswerStatus.CORRECT else AnswerStatus.WRONG
                        isSubmitted = true
                        if (isCorrect) {
                            soundManager.playCorrect()
                        } else {
                            soundManager.playIncorrect()
                        }
                    } else {
                        if (currentPage < mistakes.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
                        } else {
                            onNavigateBack()
                        }
                    }
                },
                enabled = buttonEnabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LessonFlowColors.PrimaryColor),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun MistakeInfoCard(
    mistake: com.seoulhankuko.app.data.api.model.MistakeResponse,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Đã sai ${mistake.errorCount} lần",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
                if (!mistake.lastWrongAt.isNullOrBlank()) {
                    Text(
                        text = "Lần cuối: ${formatDate(mistake.lastWrongAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LessonFlowColors.TextSecondary
                    )
                }
            }
            if (!mistake.lastWrongAnswer.isNullOrBlank()) {
                Text(
                    text = "Câu trả lời sai: ${mistake.lastWrongAnswer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LessonFlowColors.ErrorColor
                )
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    // Simple date formatting - can be enhanced
    return try {
        dateString.take(10) // Just show date part
    } catch (e: Exception) {
        dateString
    }
}

@Composable
private fun ConfirmExitDialog(
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        val visibleState by remember { mutableStateOf(true) }
        
        AnimatedVisibility(
            visible = visibleState,
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(animationSpec = tween(200))
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Bạn có chắc muốn thoát?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tiến độ ôn tập sẽ được lưu lại.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                    
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Tiếp tục",
                                color = LessonFlowColors.PrimaryColor,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = onConfirmExit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text(
                                text = "Thoát",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

