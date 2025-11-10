package com.seoulhankuko.app.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import com.seoulhankuko.app.R
import com.seoulhankuko.app.data.api.model.QuestionResponse
import com.seoulhankuko.app.domain.model.ChallengeWithOptions
import com.seoulhankuko.app.domain.model.QuestionType
import com.seoulhankuko.app.domain.model.QuestionPronunciation
import com.seoulhankuko.app.presentation.components.MatchingQuestion
import com.seoulhankuko.app.presentation.components.MatchingQuestionCard
import com.seoulhankuko.app.presentation.components.ComboCelebrationOverlay
import com.seoulhankuko.app.presentation.components.SouthKoreaLoadingIcon
import com.seoulhankuko.app.presentation.components.PronunciationEvaluationUiState
import com.seoulhankuko.app.presentation.components.PronunciationQuestionCard
import com.seoulhankuko.app.presentation.components.rememberSoundManager
import com.seoulhankuko.app.presentation.utils.AppColors
import com.seoulhankuko.app.presentation.utils.LessonColors
import com.seoulhankuko.app.presentation.utils.LessonFlowColors
import com.seoulhankuko.app.presentation.viewmodel.LessonUiState
import com.seoulhankuko.app.presentation.viewmodel.LessonViewModel

import com.seoulhankuko.app.presentation.viewmodel.StreakCelebrationEvent
import com.seoulhankuko.app.presentation.viewmodel.AdditionalChallengesResult
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.math.max
import kotlin.math.roundToInt
import timber.log.Timber

private data class InsufficientXpDialogState(
    val title: String,
    val message: String,
    val confirmText: String = "Đồng ý"
)

@Composable
private fun InsufficientXpDialog(
    state: InsufficientXpDialogState,
    onDismiss: () -> Unit
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
                .background(Color.Black.copy(alpha = 0.35f)),
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
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(
                                            LessonFlowColors.PrimaryColor.copy(alpha = 0.15f),
                                            LessonColors.AccentSoft
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.warning_material),
                                contentDescription = null,
                                tint = LessonFlowColors.PrimaryColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                textAlign = TextAlign.Center
                            )
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = LessonColors.AccentSoft.copy(alpha = 0.6f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF4B5563),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                        .fillMaxWidth()
                                )
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LessonFlowColors.PrimaryColor,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = state.confirmText,
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
    val snackbarHostState = remember { SnackbarHostState() }

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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                    val fallbackTargetExp = lesson.lesson.expPerQuestion
                        ?.takeIf { it > 0f }
                        ?.times(max(lesson.challenges.size, 1))
                        ?.roundToInt()
                    val targetExp = lesson.lesson.targetExp
                        ?: fallbackTargetExp
                        ?: (lesson.challenges.size * 10)

                    QuizPagerFlow(
                        lessonId = lessonId,
                        initialChallenges = lesson.challenges,
                        initialQuestionResponses = lesson.questionResponses.associateBy { it.id },
                        targetExp = targetExp,
                        baseExpPerQuestion = lesson.lesson.expPerQuestion,
                        hasMoreQuestionsInitial = lesson.hasMoreQuestions,
                        fetchAdditionalChallenges = { offset ->
                            viewModel.fetchAdditionalChallenges(
                                lessonId = lessonId,
                                offset = offset
                            )
                        },
                        viewModel = viewModel,
                        onNavigateBack = onNavigateBack,
                        onNavigateToListening = {
                            listeningExerciseId?.let { exerciseId ->
                                onNavigateToListening(exerciseId)
                            } ?: onNavigateBack()
                        },
                        snackbarHostState = snackbarHostState,
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
                viewModel.clearCurrentLesson()
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
    initialChallenges: List<ChallengeWithOptions>,
    initialQuestionResponses: Map<String, QuestionResponse>,
    targetExp: Int,
    baseExpPerQuestion: Float?,
    hasMoreQuestionsInitial: Boolean,
    fetchAdditionalChallenges: suspend (offset: Int) -> Result<AdditionalChallengesResult>,
    viewModel: LessonViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToListening: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val streakCelebration by viewModel.streakCelebration.collectAsStateWithLifecycle()
    val comboCelebration by viewModel.comboCelebration.collectAsStateWithLifecycle()
    val challengeItems = remember { mutableStateListOf<ChallengeWithOptions>() }
    val questionResponseMap = remember { mutableStateMapOf<String, QuestionResponse>() }
    val challengeMap = remember { mutableStateMapOf<String, ChallengeWithOptions>() }
    val pagerState = rememberPagerState(pageCount = { max(challengeItems.size, 1) })
    var hasMoreQuestions by remember { mutableStateOf(hasMoreQuestionsInitial) }
    var isFetchingMoreChallenges by remember { mutableStateOf(false) }
    var currentAnswerStatus by remember { mutableStateOf<AnswerStatus>(AnswerStatus.NONE) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var showResultScreen by remember { mutableStateOf(false) }
    var showStreakScreen by remember { mutableStateOf(false) }
    var streakEventToShow by remember { mutableStateOf<StreakCelebrationEvent?>(null) }
    var matchingCompleted by remember { mutableStateOf(false) }
    var lessonStartTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var totalTimeMillis by remember { mutableStateOf(0L) }
    var expAccumulated by remember { mutableStateOf(0f) }
    val completedChallengeIds = remember { mutableStateListOf<String>() }
    val incorrectChallengeIds = remember { mutableStateListOf<String>() }
    val reviewAttempts = remember { mutableStateMapOf<String, Int>() }
    var hasProgressUpdated by remember { mutableStateOf(false) }
    var isUpdatingProgress by remember { mutableStateOf(false) }
    var pendingNavigation by remember { mutableStateOf(false) }
    var shouldNavigateAfterStreak by remember { mutableStateOf(false) }
    var streakCelebrationScheduled by remember { mutableStateOf(false) }
    var insufficientXpDialogState by remember { mutableStateOf<InsufficientXpDialogState?>(null) }
    val pronunciationEvaluations by viewModel.pronunciationEvaluations.collectAsStateWithLifecycle()
    val pronunciationProcessing by viewModel.pronunciationProcessing.collectAsStateWithLifecycle()
    val totalTargetExp = remember(targetExp) { targetExp.coerceAtLeast(0) }
    val expPerQuestionValue = remember(targetExp, baseExpPerQuestion, initialChallenges.size) {
        when {
            baseExpPerQuestion != null && baseExpPerQuestion > 0f -> baseExpPerQuestion
            totalTargetExp > 0 && initialChallenges.isNotEmpty() -> totalTargetExp.toFloat() / initialChallenges.size.toFloat()
            else -> 0f
        }
    }
    val maxReviewAttempts = 2

    // Sound manager for playing correct/incorrect sounds
    val soundManager = rememberSoundManager()

    // TTS Manager for reading question content - injected via ViewModel
    val ttsManager = viewModel.ttsManager

    val context = LocalContext.current
    var audioPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        audioPermissionGranted = granted
    }

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
        challengeItems.getOrNull(pagerState.currentPage)?.challenge?.id?.let { currentId ->
            // pronunciation processing state managed in ViewModel
        }
        if (ttsManager.isAvailable()) {
            ttsManager.stop()
        }
    }

    LaunchedEffect(initialChallenges) {
        if (showResultScreen || showStreakScreen) {
            return@LaunchedEffect
        }
        challengeItems.clear()
        challengeItems.addAll(initialChallenges)
        questionResponseMap.clear()
        questionResponseMap.putAll(initialQuestionResponses)
        challengeMap.clear()
        challengeMap.putAll(initialChallenges.associateBy { it.challenge.id })
        hasMoreQuestions = hasMoreQuestionsInitial
        showResultScreen = false
        showStreakScreen = false
        expAccumulated = 0f
        completedChallengeIds.clear()
        incorrectChallengeIds.clear()
        reviewAttempts.clear()
        lessonStartTimestamp = System.currentTimeMillis()
        totalTimeMillis = 0L
        hasProgressUpdated = false
        isUpdatingProgress = false
        pendingNavigation = false
        shouldNavigateAfterStreak = false
        streakCelebrationScheduled = false
        streakEventToShow = null
    }

    LaunchedEffect(streakCelebration) {
        streakEventToShow = streakCelebration
        if (streakCelebration != null) {
            streakCelebrationScheduled = true
        }
    }

    LaunchedEffect(showStreakScreen) {
        if (showStreakScreen) {
            viewModel.markStreakCelebrationDisplayed()
        }
    }

    fun dismissStreakCelebration() {
        val navigateAfter = shouldNavigateAfterStreak
        showStreakScreen = false
        shouldNavigateAfterStreak = false
        streakEventToShow = null
        viewModel.clearStreakCelebration()
        streakCelebrationScheduled = false
        if (navigateAfter) {
            pendingNavigation = false
            onNavigateBack()
        }
    }

    LaunchedEffect(showResultScreen) {
        if (showResultScreen && !hasProgressUpdated && !isUpdatingProgress) {
            isUpdatingProgress = true
            streakCelebrationScheduled = false
            viewModel.updateLessonProgress(lessonId) { celebrationScheduled ->
                hasProgressUpdated = true
                isUpdatingProgress = false
                streakCelebrationScheduled = celebrationScheduled
            }
        }
    }

    val streakCelebrationFlow = remember { viewModel.streakCelebration }

    LaunchedEffect(pendingNavigation, hasProgressUpdated, streakCelebrationScheduled, streakEventToShow) {
        if (!pendingNavigation || !hasProgressUpdated) return@LaunchedEffect

        if (streakCelebrationScheduled) {
            val event = streakEventToShow ?: streakCelebrationFlow.filterNotNull().first().also {
                streakEventToShow = it
            }
            if (ttsManager.isAvailable()) {
                ttsManager.stop()
            }
            showResultScreen = false
            shouldNavigateAfterStreak = true
            showStreakScreen = true
            pendingNavigation = false
        } else {
            if (ttsManager.isAvailable()) {
                ttsManager.stop()
            }
            showResultScreen = false
            pendingNavigation = false
            onNavigateBack()
        }
    }

    val streakEvent = streakEventToShow
    
    insufficientXpDialogState?.let { dialogState ->
        InsufficientXpDialog(
            state = dialogState,
            onDismiss = { insufficientXpDialogState = null }
        )
    }

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
            experienceGained = expAccumulated.roundToInt(),
            onContinue = {
                pendingNavigation = true
                when {
                    hasProgressUpdated -> Unit
                    isUpdatingProgress -> Unit
                    else -> {
                        isUpdatingProgress = true
                        streakCelebrationScheduled = false
                        viewModel.updateLessonProgress(lessonId) { celebrationScheduled ->
                            hasProgressUpdated = true
                            isUpdatingProgress = false
                            streakCelebrationScheduled = celebrationScheduled
                        }
                    }
                }
            }
        )
    } else {
        val currentPage = pagerState.currentPage
        val currentChallenge = challengeItems.getOrNull(currentPage)
        val currentQuestionResponse = currentChallenge?.let { questionResponseMap[it.challenge.id] }
        val isMatchingQuestion = currentChallenge?.challenge?.type == QuestionType.MATCHING && currentQuestionResponse != null
        val isPronunciationQuestion = currentChallenge?.challenge?.type == QuestionType.PRONUNCIATION && currentQuestionResponse != null

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

        fun registerCorrectForChallenge(challengeId: String?) {
            val id = challengeId ?: return
            incorrectChallengeIds.remove(id)
            reviewAttempts.remove(id)
            if (!completedChallengeIds.contains(id)) {
                completedChallengeIds.add(id)
                val gained = expPerQuestionValue.coerceAtLeast(0f)
                expAccumulated = (expAccumulated + gained).coerceAtMost(totalTargetExp.toFloat())
                viewModel.recordQuestionCompletion(id, gained)
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

            viewModel.onAnswerEvaluated(isCorrect)

            val currentId = currentChallenge?.challenge?.id
            if (isCorrect && currentId != null) {
                registerCorrectForChallenge(currentId)
                viewModel.submitPracticeCorrectAnswer(
                    lessonId = lessonId,
                    questionId = currentId,
                    selectedOptionId = selectedOption ?: ""
                )
            } else if (!isCorrect && currentId != null) {
                if (!incorrectChallengeIds.contains(currentId)) {
                    incorrectChallengeIds.add(currentId)
                }
            }
        }

        fun handleIncompleteXp() {
            val reviewCandidates = incorrectChallengeIds.filter { id ->
                reviewAttempts.getOrDefault(id, 0) < maxReviewAttempts
            }
            if (reviewCandidates.isNotEmpty()) {
                val reviewChallenges = reviewCandidates.mapNotNull { id -> challengeMap[id] }
                if (reviewChallenges.isNotEmpty()) {
                    reviewCandidates.forEach { id ->
                        val attemptCount = reviewAttempts.getOrDefault(id, 0) + 1
                        reviewAttempts[id] = attemptCount
                    }
                    incorrectChallengeIds.removeAll(reviewCandidates.toSet())
                    val insertionIndex = challengeItems.size
                    challengeItems.addAll(reviewChallenges)
                    insufficientXpDialogState = InsufficientXpDialogState(
                        title = "Chưa đủ XP",
                        message = "Bạn chưa đủ XP, ôn lại các câu vừa sai để tích lũy thêm XP."
                    )
                    coroutineScope.launch {
                        pagerState.scrollToPage(insertionIndex.coerceAtMost(challengeItems.lastIndex))
                    }
                    currentAnswerStatus = AnswerStatus.NONE
                    selectedOption = null
                    matchingCompleted = false
                    return
                }
            }

            val hasRetryExhausted = incorrectChallengeIds.isNotEmpty() && reviewCandidates.isEmpty()
            if (!hasMoreQuestions) {
                val dialogMessage = if (hasRetryExhausted) {
                    "Bạn đã ôn lại tất cả câu sai nhưng vẫn chưa đủ XP. Vui lòng thử lại sau."
                } else {
                    "Bạn chưa đạt đủ XP và hiện không còn câu hỏi mới. Vui lòng thử lại sau."
                }
                insufficientXpDialogState = InsufficientXpDialogState(
                    title = "Chưa đủ XP",
                    message = dialogMessage
                )
                return
            }
            if (isFetchingMoreChallenges) return

            coroutineScope.launch {
                isFetchingMoreChallenges = true
                val currentIndex = pagerState.currentPage
                val result = fetchAdditionalChallenges(challengeItems.size)
                result.fold(
                    onSuccess = { additional ->
                        if (additional.challenges.isNotEmpty()) {
                            challengeItems.addAll(additional.challenges)
                            challengeMap.putAll(additional.challenges.associateBy { it.challenge.id })
                            questionResponseMap.putAll(additional.questionResponses)
                            hasMoreQuestions = additional.hasMore
                            val nextIndex = (currentIndex + 1).coerceAtMost(challengeItems.lastIndex)
                            pagerState.scrollToPage(nextIndex)
                        } else {
                            hasMoreQuestions = additional.hasMore
                            insufficientXpDialogState = InsufficientXpDialogState(
                                title = "Chưa đủ XP",
                                message = "Không còn câu hỏi mới, vui lòng thử lại sau."
                            )
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to fetch additional challenges for lesson $lessonId")
                        insufficientXpDialogState = InsufficientXpDialogState(
                            title = "Chưa đủ XP",
                            message = "Không thể tải thêm câu hỏi. Vui lòng thử lại."
                        )
                    }
                )
                currentAnswerStatus = AnswerStatus.NONE
                selectedOption = null
                matchingCompleted = false
                isFetchingMoreChallenges = false
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
                ExpProgressIndicator(
                    earnedExp = expAccumulated.roundToInt(),
                    targetExp = totalTargetExp,
                    answeredCount = completedChallengeIds.size,
                    totalQuestions = challengeItems.size,
                    perQuestionExp = expPerQuestionValue
                )

                // Pager with questions
                if (challengeItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có câu hỏi nào cho bài học này",
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
                        userScrollEnabled = false
                    ) { page ->
                        val challenge = challengeItems[page]
                        val questionResponse = questionResponseMap[challenge.challenge.id]
                        val isBlankQuestion = challenge.challenge.type == QuestionType.BLANK && questionResponse != null
                        val isMatchingQuestion = challenge.challenge.type == QuestionType.MATCHING && questionResponse != null
                        val isPronunciationQuestion = challenge.challenge.type == QuestionType.PRONUNCIATION && questionResponse != null
                        val isCurrentPage = page == pagerState.currentPage
                        val pageAnswerStatus = if (isCurrentPage) currentAnswerStatus else AnswerStatus.NONE
                        val pageSelectedOption = if (isCurrentPage) selectedOption else null

                        if (isBlankQuestion) {
                            FillInBlankQuestionCard(
                                challenge = challenge,
                                question = questionResponse,
                                questionIndex = page,
                                selectedOption = pageSelectedOption,
                                answerStatus = pageAnswerStatus,
                                onOptionSelected = { optionId ->
                                    if (!isCurrentPage || currentAnswerStatus != AnswerStatus.NONE) return@FillInBlankQuestionCard
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
                                },
                                onAnswerSubmitted = {
                                    if (!isCurrentPage) return@FillInBlankQuestionCard
                                    submitAnswer(it)
                                },
                                onPlayAudio = {
                                    val questionText = challenge.challenge.question
                                    if (questionText.isNotBlank()) {
                                        ttsManager.speak(questionText, speed = 0.8f)
                                    }
                                }
                            )
                        } else if (isPronunciationQuestion) {
                            val challengeId = challenge.challenge.id
                            val pronunciationData = challenge.pronunciation ?: questionResponse?.pronunciation?.let {
                                QuestionPronunciation(
                                    id = it.id,
                                    targetPhrase = it.targetPhrase,
                                    referenceAudioUrl = it.referenceAudioUrl
                                )
                            }
                            val evaluationState = pronunciationEvaluations[challengeId] ?: PronunciationEvaluationUiState()
                            val isProcessingPronunciation = pronunciationProcessing.contains(challengeId)

                            PronunciationQuestionCard(
                                prompt = pronunciationData?.targetPhrase ?: challenge.challenge.question,
                                explanation = questionResponse.explanation,
                                referenceAudioUrl = pronunciationData?.referenceAudioUrl,
                                evaluationState = evaluationState,
                                isProcessing = isProcessingPronunciation,
                                onRecordStart = {
                                    if (!isCurrentPage) return@PronunciationQuestionCard false
                                    if (!audioPermissionGranted) {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        false
                                    } else {
                                        viewModel.beginPronunciationRecording(challengeId)
                                    }
                                },
                                onRecordStop = {
                                    if (!isCurrentPage) return@PronunciationQuestionCard
                                    val result = viewModel.completePronunciationRecording(
                                        lessonId = lessonId,
                                        questionId = challengeId,
                                        sentence = pronunciationData?.targetPhrase ?: challenge.challenge.question
                                    )
                                    result?.let { evaluation ->
                                        if (evaluation.passed) {
                                            if (currentAnswerStatus != AnswerStatus.CORRECT) {
                                                currentAnswerStatus = AnswerStatus.CORRECT
                                                registerCorrectForChallenge(challengeId)
                                                soundManager.playCorrect()
                                                viewModel.submitPracticeCorrectAnswer(
                                                    lessonId = lessonId,
                                                    questionId = challengeId,
                                                    selectedOptionId = ""
                                                )
                                                viewModel.onAnswerEvaluated(true)
                                            }
                                        } else {
                                            currentAnswerStatus = AnswerStatus.WRONG
                                            soundManager.playIncorrect()
                                            viewModel.onAnswerEvaluated(false)
                                        }
                                    }
                                },
                                onRetry = {
                                    if (!isCurrentPage) return@PronunciationQuestionCard
                                    currentAnswerStatus = AnswerStatus.NONE
                                    viewModel.resetPronunciationAttempt(challengeId)
                                },
                                onListenNative = {
                                    val nativePhrase = pronunciationData?.targetPhrase ?: challenge.challenge.question
                                    if (nativePhrase.isNotBlank() && ttsManager.isAvailable()) {
                                        ttsManager.stop()
                                        ttsManager.speak(nativePhrase, speed = 0.8f)
                                    }
                                }
                            )
                        } else if (isMatchingQuestion) {
                            val metadataPairs = questionResponse?.metadata?.pairs
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
                                        explanation = questionResponse?.explanation.orEmpty()
                                    ),
                                    onMatchCompleted = { isCompleted ->
                                        if (!isCurrentPage) return@MatchingQuestionCard
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
                                                viewModel.onAnswerEvaluated(true)
                                            }
                                        } else if (currentAnswerStatus != AnswerStatus.NONE) {
                                            currentAnswerStatus = AnswerStatus.NONE
                                        }
                                    },
                                    onMatchFeedback = { isCorrect ->
                                        if (!isCurrentPage) return@MatchingQuestionCard
                                        if (isCorrect) {
                                            soundManager.playCorrect()
                                        } else {
                                            soundManager.playIncorrect()
                                            viewModel.onAnswerEvaluated(false)
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
                                selectedOption = pageSelectedOption,
                                answerStatus = pageAnswerStatus,
                                onOptionSelected = { optionId ->
                                    if (!isCurrentPage || currentAnswerStatus != AnswerStatus.NONE) return@QuizQuestionCard
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
                                },
                                onAnswerSubmitted = {
                                    if (!isCurrentPage) return@QuizQuestionCard
                                    submitAnswer(it)
                                },
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
            }

        val buttonEnabled = when {
            isMatchingQuestion -> matchingCompleted
            isPronunciationQuestion -> currentAnswerStatus == AnswerStatus.CORRECT
            currentAnswerStatus == AnswerStatus.NONE -> selectedOption != null
            else -> true
        }

        val xpRequirementMet = totalTargetExp <= 0 || expAccumulated.roundToInt() >= totalTargetExp

        val buttonText = when {
            isMatchingQuestion && !matchingCompleted -> "Hoàn thành"
            currentAnswerStatus == AnswerStatus.NONE -> "Kiểm tra"
            currentPage == challengeItems.lastIndex && !xpRequirementMet -> "Tiếp tục"
            currentPage == challengeItems.lastIndex -> "Hoàn thành"
            else -> "Tiếp tục"
        }

        val shouldShowActionButton = when {
            isMatchingQuestion -> true
            isPronunciationQuestion -> true
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
                        if (currentPage == challengeItems.lastIndex) {
                            if (xpRequirementMet) {
                                totalTimeMillis = (System.currentTimeMillis() - lessonStartTimestamp).coerceAtLeast(0L)
                                showResultScreen = true
                                pendingNavigation = false
                            } else {
                                handleIncompleteXp()
                            }
                        } else {
                            currentAnswerStatus = AnswerStatus.NONE
                            selectedOption = null
                            matchingCompleted = false
                            if (ttsManager.isAvailable()) {
                                ttsManager.stop()
                            }
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
                        }
                    }
                },
                enabled = buttonEnabled && !isFetchingMoreChallenges,
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

        if (isFetchingMoreChallenges) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                SouthKoreaLoadingIcon(size = 56.dp)
            }
        }

        comboCelebration?.let { celebration ->
            ComboCelebrationOverlay(
                state = celebration,
                modifier = Modifier.align(Alignment.Center),
                onAnimationFinished = { viewModel.clearComboCelebration() }
            )
        }
        }
    }
}

/**
 * Progress indicator showing current question number
 */
@Composable
fun ExpProgressIndicator(
    earnedExp: Int,
    targetExp: Int,
    answeredCount: Int,
    totalQuestions: Int,
    perQuestionExp: Float
) {
    val safeTarget = max(targetExp, 1)
    val safeEarned = earnedExp.coerceAtLeast(0)
    val progress = when {
        targetExp <= 0 -> 1f
        else -> safeEarned.coerceAtMost(targetExp).toFloat() / safeTarget.toFloat()
    }
    val safeTotalQuestions = max(totalQuestions, 1)

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
            text = "${safeEarned.coerceAtMost(targetExp)} / $targetExp XP",
            style = MaterialTheme.typography.labelMedium,
            color = LessonFlowColors.TextSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Text(
            text = "$answeredCount / $safeTotalQuestions câu đã đạt XP",
            style = MaterialTheme.typography.labelSmall,
            color = LessonFlowColors.TextSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        if (perQuestionExp > 0f) {
            Text(
                text = "~${perQuestionExp.roundToInt()} XP mỗi câu đúng",
                style = MaterialTheme.typography.labelSmall,
                color = LessonFlowColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
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
        SouthKoreaLoadingIcon(size = 32.dp)
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

