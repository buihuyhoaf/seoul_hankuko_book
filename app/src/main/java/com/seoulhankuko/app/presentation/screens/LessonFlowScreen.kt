package com.seoulhankuko.app.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.R
import com.seoulhankuko.app.data.api.model.QuestionResponse
import com.seoulhankuko.app.domain.model.ChallengeWithOptions
import com.seoulhankuko.app.domain.model.QuestionType
import com.seoulhankuko.app.presentation.components.MatchingQuestion
import com.seoulhankuko.app.presentation.components.MatchingQuestionCard
import com.seoulhankuko.app.presentation.components.rememberSoundManager
import com.seoulhankuko.app.presentation.utils.AppColors
import com.seoulhankuko.app.presentation.utils.LessonColors
import com.seoulhankuko.app.presentation.utils.LessonFlowColors
import com.seoulhankuko.app.presentation.viewmodel.LessonUiState
import com.seoulhankuko.app.presentation.viewmodel.LessonViewModel
import com.seoulhankuko.app.presentation.viewmodel.StreakCelebrationEvent
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * LessonFlowScreen - Shows quiz questions in a swipeable pager
 * Each page shows one question from lesson.questions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonFlowScreen(
    lessonId: String,
    onNavigateBack: () -> Unit,
    onNavigateToListening: (exerciseId: String) -> Unit,
    viewModel: LessonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        showExitDialog = true
    }

    // Load lesson on start
    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }

    Scaffold(
        topBar = {
            val lessonTitle = (uiState as? LessonUiState.Success)
                ?.lessonWithChallenges
                ?.lesson
                ?.title
                ?: "Bài học"

            TopAppBar(
                title = {
                    Text(
                        text = lessonTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LessonColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
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
                ),
                modifier = Modifier.shadow(elevation = 4.dp)
            )
        },
        containerColor = LessonColors.BackgroundWhite
    ) { innerPadding ->
        when (val state = uiState) {
            is LessonUiState.Loading -> {
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

            is LessonUiState.Success -> {
                state.lessonWithChallenges?.let { lesson ->
                    val listeningExerciseId = lesson.exercisesResponse
                        .firstOrNull { it.type.lowercase() == "listening" }
                        ?.id

                    QuizPagerFlow(
                        lessonId = lessonId,
                        challenges = lesson.challenges,
                        questionResponses = lesson.questionResponses.associateBy { it.id },
                        viewModel = viewModel,
                        onNavigateBack = onNavigateBack,
                        onNavigateToListening = {
                            listeningExerciseId?.let { exerciseId ->
                                onNavigateToListening(exerciseId)
                            } ?: onNavigateBack()
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            is LessonUiState.Error -> {
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
                            text = "Không thể tải bài học",
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

/**
 * Quiz Pager - Swipeable pages for each quiz question
 */
@Composable
fun QuizPagerFlow(
    lessonId: String,
    challenges: List<ChallengeWithOptions>,
    questionResponses: Map<String, QuestionResponse>,
    viewModel: LessonViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { challenges.size })
    val coroutineScope = rememberCoroutineScope()
    val streakCelebration by viewModel.streakCelebration.collectAsStateWithLifecycle()
    var currentAnswerStatus by remember { mutableStateOf<AnswerStatus>(AnswerStatus.NONE) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var showResultScreen by remember { mutableStateOf(false) }
    var showStreakScreen by remember { mutableStateOf(false) }
    var streakEventToShow by remember { mutableStateOf<StreakCelebrationEvent?>(null) }
    var matchingCompleted by remember { mutableStateOf(false) }
    var lessonStartTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var totalTimeMillis by remember { mutableStateOf(0L) }
    var correctCount by remember { mutableStateOf(0) }
    var experienceGained by remember { mutableStateOf(0) }
    val completedChallengeIds = remember { mutableStateListOf<String>() }
    var hasProgressUpdated by remember { mutableStateOf(false) }
    var isUpdatingProgress by remember { mutableStateOf(false) }
    var pendingNavigation by remember { mutableStateOf(false) }
    var shouldNavigateAfterStreak by remember { mutableStateOf(false) }

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

    LaunchedEffect(pagerState.currentPage) {
        currentAnswerStatus = AnswerStatus.NONE
        selectedOption = null
        matchingCompleted = false
        if (ttsManager.isAvailable()) {
            ttsManager.stop()
        }
    }

    LaunchedEffect(challenges) {
        showResultScreen = false
        showStreakScreen = false
        correctCount = 0
        experienceGained = 0
        completedChallengeIds.clear()
        lessonStartTimestamp = System.currentTimeMillis()
        totalTimeMillis = 0L
        hasProgressUpdated = false
        isUpdatingProgress = false
        pendingNavigation = false
        shouldNavigateAfterStreak = false
    }

    LaunchedEffect(streakCelebration) {
        if (streakCelebration != null) {
            streakEventToShow = streakCelebration
        }
    }

    LaunchedEffect(showStreakScreen) {
        if (showStreakScreen) {
            viewModel.markStreakCelebrationDisplayed()
        }
    }

    fun proceedAfterLessonResult() {
        pendingNavigation = false
        showResultScreen = false
        if (ttsManager.isAvailable()) {
            ttsManager.stop()
        }
        val event = streakEventToShow
        if (event != null) {
            shouldNavigateAfterStreak = true
            showStreakScreen = true
        } else {
            onNavigateToListening()
        }
    }

    fun dismissStreakCelebration() {
        val navigateAfter = shouldNavigateAfterStreak
        showStreakScreen = false
        shouldNavigateAfterStreak = false
        streakEventToShow = null
        viewModel.clearStreakCelebration()
        if (navigateAfter) {
            onNavigateToListening()
        }
    }

    LaunchedEffect(showResultScreen) {
        if (showResultScreen && !hasProgressUpdated && !isUpdatingProgress) {
            isUpdatingProgress = true
            viewModel.updateLessonProgress(lessonId) {
                hasProgressUpdated = true
                isUpdatingProgress = false
                if (pendingNavigation) {
                    proceedAfterLessonResult()
                }
            }
        }
    }

    val streakEvent = streakEventToShow

    // Show celebratory result screen when lesson is completed
    if (showStreakScreen && streakEvent != null) {
        StreakCelebrationScreen(
            streakDays = streakEvent.streakDays,
            onContinueClick = { dismissStreakCelebration() },
            onExitConfirmed = { dismissStreakCelebration() },
            modifier = modifier
        )
    } else if (showResultScreen) {
        LessonResultScreen(
            totalTime = totalTimeMillis,
            experienceGained = experienceGained,
            onRetry = {
                showResultScreen = false
                correctCount = 0
                experienceGained = 0
                completedChallengeIds.clear()
                lessonStartTimestamp = System.currentTimeMillis()
                totalTimeMillis = 0L
                currentAnswerStatus = AnswerStatus.NONE
                selectedOption = null
                matchingCompleted = false
                soundManager.cleanup()
                if (ttsManager.isAvailable()) {
                    ttsManager.stop()
                }
                coroutineScope.launch {
                    pagerState.scrollToPage(0)
                }
                hasProgressUpdated = false
                isUpdatingProgress = false
                pendingNavigation = false
            },
            onContinue = {
                pendingNavigation = true
                when {
                    hasProgressUpdated -> proceedAfterLessonResult()
                    isUpdatingProgress -> Unit
                    else -> {
                        isUpdatingProgress = true
                        viewModel.updateLessonProgress(lessonId) {
                            hasProgressUpdated = true
                            isUpdatingProgress = false
                            proceedAfterLessonResult()
                        }
                    }
                }
            }
        )
    } else {
        val currentPage = pagerState.currentPage
        val currentChallenge = challenges.getOrNull(currentPage)
        val currentQuestionResponse = currentChallenge?.let { questionResponses[it.challenge.id] }
        val isMatchingQuestion = currentChallenge?.challenge?.type == QuestionType.MATCHING && currentQuestionResponse != null

        fun isSelectionCorrect(selection: String?): Boolean {
            if (selection.isNullOrEmpty() || currentChallenge == null) return false
            val correctOption = currentChallenge.options.firstOrNull { it.correct } ?: return false
            val metadataChoices = currentQuestionResponse?.metadata?.choices
            return if (!metadataChoices.isNullOrEmpty()) {
                selection == correctOption.text
            } else {
                selection == correctOption.id
            }
        }

        fun registerCorrectForChallenge(challengeId: String?) {
            val id = challengeId ?: return
            if (!completedChallengeIds.contains(id)) {
                completedChallengeIds.add(id)
                correctCount += 1
            }
        }

        fun submitAnswer(isCorrect: Boolean) {
            if (!isCorrect && selectedOption == null && !isMatchingQuestion) {
                return
            }

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

            if (isCorrect && currentChallenge != null) {
                registerCorrectForChallenge(currentChallenge.challenge.id)
                viewModel.submitPracticeCorrectAnswer(
                    lessonId = lessonId,
                    questionId = currentChallenge.challenge.id,
                    selectedOptionId = selectedOption ?: ""
                )
            }
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
                ProgressIndicator(
                    currentPage = pagerState.currentPage + 1,
                    totalPages = challenges.size
                )

                // Pager with questions
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    pageSpacing = 16.dp,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    userScrollEnabled = false
                ) { page ->
                    val challenge = challenges[page]
                    val questionResponse = questionResponses[challenge.challenge.id]
                    val isBlankQuestion = challenge.challenge.type == QuestionType.BLANK && questionResponse != null
                    val isMatchingQuestion = challenge.challenge.type == QuestionType.MATCHING && questionResponse != null

                    if (isBlankQuestion) {
                        FillInBlankQuestionCard(
                            challenge = challenge,
                            question = questionResponse,
                            questionIndex = page,
                            selectedOption = selectedOption,
                            answerStatus = currentAnswerStatus,
                            onOptionSelected = { optionId ->
                                if (currentAnswerStatus == AnswerStatus.NONE) {
                                    selectedOption = optionId
                                    val option = challenge.options.find { it.id == optionId }
                                    val metadataChoice = questionResponse?.metadata?.choices?.firstOrNull { it == optionId }
                                    val optionText = option?.text
                                        ?.takeIf { it.isNotBlank() }
                                        ?: metadataChoice
                                        ?: optionId

                                    if (optionText.isNotBlank()) {
                                        if (optionText.containsKoreanCharacters()) {
                                            if (ttsManager.isAvailable()) {
                                                ttsManager.stop()
                                                ttsManager.speak(optionText, speed = 0.8f)
                                            }
                                        } else if (ttsManager.isAvailable()) {
                                            ttsManager.stop()
                                        }
                                    }
                                }
                            },
                            onAnswerSubmitted = { submitAnswer(it) },
                            onPlayAudio = {
                                val questionText = challenge.challenge.question
                                if (questionText.isNotBlank()) {
                                    ttsManager.speak(questionText, speed = 0.8f)
                                }
                            }
                        )
                    } else if (isMatchingQuestion) {
                        val metadataPairs = questionResponse?.metadata?.pairs
                            ?.mapNotNull { pair ->
                                val left = pair.left
                                val right = pair.right
                                if (left.isNullOrBlank() && right.isNullOrBlank()) {
                                    null
                                } else {
                                    left.orEmpty() to right.orEmpty()
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
                                    explanation = questionResponse?.explanation.orEmpty()
                                ),
                                onMatchCompleted = { isCompleted ->
                                    matchingCompleted = isCompleted
                                    if (isCompleted) {
                                        if (currentAnswerStatus != AnswerStatus.CORRECT) {
                                            currentAnswerStatus = AnswerStatus.CORRECT
                                            registerCorrectForChallenge(challenge.challenge.id)
                                            viewModel.submitPracticeCorrectAnswer(
                                                lessonId = lessonId,
                                                questionId = challenge.challenge.id,
                                                selectedOptionId = ""
                                            )
                                        }
                                    } else if (currentAnswerStatus != AnswerStatus.NONE) {
                                        currentAnswerStatus = AnswerStatus.NONE
                                    }
                                },
                                onMatchFeedback = { isCorrect ->
                                    if (isCorrect) {
                                        soundManager.playCorrect()
                                    } else {
                                        soundManager.playIncorrect()
                                    }
                                }
                            )
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Text(
                                    text = "Không có dữ liệu ghép cặp cho câu hỏi này.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LessonFlowColors.TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                )
                            }
                        }
                    } else {
                        QuizQuestionCard(
                            challenge = challenge,
                            selectedOption = selectedOption,
                            answerStatus = currentAnswerStatus,
                            onOptionSelected = { optionId ->
                                if (currentAnswerStatus == AnswerStatus.NONE) {
                                    selectedOption = optionId
                                    val option = challenge.options.find { it.id == optionId }
                                    val optionText = option?.text.orEmpty()
                                    if (optionText.containsKoreanCharacters()) {
                                        if (ttsManager.isAvailable()) {
                                            ttsManager.stop()
                                            ttsManager.speak(optionText, speed = 0.8f)
                                        }
                                    } else if (ttsManager.isAvailable()) {
                                        ttsManager.stop()
                                    }
                                }
                            },
                            onAnswerSubmitted = { submitAnswer(it) },
                            questionIndex = page,
                            onPlayAudio = {
                                val questionText = challenge.challenge.question
                                if (questionText.isNotBlank()) {
                                    ttsManager.speak(questionText, speed = 0.8f)
                                }
                            }
                        )
                    }
                }
            }

        val buttonEnabled = when {
            isMatchingQuestion -> matchingCompleted
            currentAnswerStatus == AnswerStatus.NONE -> selectedOption != null
            else -> true
        }

        val buttonText = when {
            isMatchingQuestion && !matchingCompleted -> "Hoàn thành"
            currentAnswerStatus == AnswerStatus.NONE -> "Kiểm tra"
            currentPage == challenges.lastIndex -> "Hoàn thành"
            else -> "Tiếp tục"
        }

        val shouldShowActionButton = when {
            isMatchingQuestion -> true
            currentAnswerStatus != AnswerStatus.NONE -> true
            selectedOption != null -> true
            else -> false
        }

        if (shouldShowActionButton) {
            Button(
                onClick = {
                    if (currentAnswerStatus == AnswerStatus.NONE) {
                        val isCorrect = isSelectionCorrect(selectedOption)
                        submitAnswer(isCorrect)
                    } else {
                        if (currentPage == challenges.lastIndex) {
                            val totalQuestions = challenges.size.coerceAtLeast(1)
                            totalTimeMillis = (System.currentTimeMillis() - lessonStartTimestamp).coerceAtLeast(0L)
                            val calculatedXp = ((correctCount.toFloat() / totalQuestions.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
                            experienceGained = calculatedXp
                            showResultScreen = true
                            pendingNavigation = false
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
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
}

/**
 * Progress indicator showing current question number
 */
@Composable
fun ProgressIndicator(currentPage: Int, totalPages: Int) {
    val safeTotal = max(totalPages, 1)
    val clampedPage = currentPage.coerceIn(0, safeTotal)
    val progress = clampedPage.toFloat() / safeTotal.toFloat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = LessonFlowColors.PrimaryColor,
            trackColor = LessonFlowColors.BackgroundColor
        )

        Text(
            text = "$clampedPage / $safeTotal",
            style = MaterialTheme.typography.labelMedium,
            color = LessonFlowColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private val koreanCharacterRegex = Regex("[\\u1100-\\u11FF\\u3130-\\u318F\\uAC00-\\uD7AF]")

private fun String.containsKoreanCharacters(): Boolean = koreanCharacterRegex.containsMatchIn(this)

@Composable
private fun ConfirmExitDialog(
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
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
                        Icon(
                            painter = painterResource(id = R.drawable.warning_material),
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(48.dp)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Bạn có chắc muốn thoát?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Nếu thoát bây giờ, bạn sẽ không nhận được kinh nghiệm từ bài học này.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280),
                                textAlign = TextAlign.Center
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(2.dp, LessonFlowColors.PrimaryColor)
                            ) {
                                Text(
                                    text = "Tiếp tục học",
                                    color = LessonFlowColors.PrimaryColor,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = onConfirmExit,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
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

