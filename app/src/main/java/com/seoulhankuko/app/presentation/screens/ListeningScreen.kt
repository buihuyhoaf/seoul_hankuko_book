package com.seoulhankuko.app.presentation.screens

import android.media.MediaPlayer
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seoulhankuko.app.R
import com.seoulhankuko.app.data.api.model.ExerciseResponse
import com.seoulhankuko.app.data.api.model.ExerciseQuestionResponse
import com.seoulhankuko.app.data.api.model.ExerciseQuestionOptionResponse
import com.seoulhankuko.app.presentation.components.SouthKoreaLoadingIcon
import com.seoulhankuko.app.presentation.components.ComboCelebrationOverlay
import com.seoulhankuko.app.presentation.components.rememberSoundManager
import com.seoulhankuko.app.presentation.utils.AppColors
import com.seoulhankuko.app.presentation.viewmodel.LessonUiState
import com.seoulhankuko.app.presentation.viewmodel.LessonViewModel
import com.seoulhankuko.app.presentation.viewmodel.StreakCelebrationEvent
import com.seoulhankuko.app.presentation.viewmodel.TranscriptSegment
import com.seoulhankuko.app.presentation.viewmodel.TranscriptSyncViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

private const val LISTENING_EXP_PER_CORRECT = 20

/**
 * Modern ListeningScreen with Material 3 design
 * Handles listening exercises with audio playback, answer submission, and feedback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningScreen(
    modifier: Modifier = Modifier,
    exerciseId: String,
    lessonId: String? = null,
    onNavigateBack: () -> Unit,
    onNextExercise: (() -> Unit)? = null,
    viewModel: LessonViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // If lessonId is provided and ViewModel doesn't have data, load lesson
    LaunchedEffect(lessonId) {
        if (lessonId != null) {
    when (val state = uiState) {
                is LessonUiState.Success -> {
                    val currentLessonId = state.lessonWithChallenges?.lesson?.id
                    if (currentLessonId != lessonId) {
                        Timber.d("ListeningScreen: Loading lesson $lessonId (current: $currentLessonId)")
                        viewModel.loadLesson(lessonId)
                    }
                }
                else -> {
                    Timber.d("ListeningScreen: Loading lesson $lessonId (state: ${state::class.simpleName})")
                    viewModel.loadLesson(lessonId)
                }
            }
        }
    }
    
    // Get exercise from current ViewModel state
    // Since we're navigating from LessonScreen, ViewModel should already have the lesson data
    val exerciseResponse = remember(exerciseId, uiState) {
        when (val state = uiState) {
        is LessonUiState.Success -> {
                val exercise = state.lessonWithChallenges?.exercisesResponse?.firstOrNull {
                    it.id == exerciseId && it.type.lowercase() == "listening"
                }
                
                if (exercise != null) {
                    Timber.d("Found exercise: id=${exercise.id}, title=${exercise.title}, audioUrl=${exercise.audioUrl}")
                } else {
                    Timber.w("Exercise not found: exerciseId=$exerciseId")
                    Timber.w("Available exercises: ${state.lessonWithChallenges?.exercisesResponse?.map { "id=${it.id}, type=${it.type}" }}")
                }
                exercise
            }
            else -> {
                Timber.d("ListeningScreen: State is ${state::class.simpleName}, exercise not available yet")
                null
            }
        }
    }
    
    // Get current lessonId from state
    val currentLessonId = remember(uiState) {
        when (val state = uiState) {
            is LessonUiState.Success -> state.lessonWithChallenges?.lesson?.id
            else -> null
        }
    }
    
    // Use provided lessonId or get from state
    val effectiveLessonId = lessonId ?: currentLessonId
    
    // Show appropriate UI based on state
    when {
        exerciseResponse != null -> {
            // Use full ExerciseResponse with all data (audioUrl, transcript, etc.)
            ListeningScreenContent(
                exercise = exerciseResponse,
                lessonId = effectiveLessonId,
                viewModel = viewModel,
                onNavigateBack = onNavigateBack,
                onNextExercise = onNextExercise,
                modifier = modifier
            )
        }
        uiState is LessonUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                SouthKoreaLoadingIcon(size = 56.dp)
            }
        }
        uiState is LessonUiState.Error -> {
            val errorState = uiState as LessonUiState.Error
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
            ) {
                Text(
                        text = "Error: ${errorState.message}",
                    style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFE74C3C),
                    textAlign = TextAlign.Center
                )
                    Button(onClick = onNavigateBack) {
                        Text("Go Back")
            }
        }
    }
}
        uiState is LessonUiState.Success && exerciseResponse == null -> {
            // Exercise not found in loaded data
            val successState = uiState as LessonUiState.Success
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Exercise not found",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Looking for exercise ID: $exerciseId",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF999999),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Available exercises: ${successState.lessonWithChallenges?.exercisesResponse?.size ?: 0}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF999999),
                        textAlign = TextAlign.Center
                    )
                    if (successState.lessonWithChallenges?.exercisesResponse != null) {
                        Text(
                            text = successState.lessonWithChallenges!!.exercisesResponse.joinToString("\n") { 
                                "ID: ${it.id}, Type: ${it.type}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF999999),
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(onClick = onNavigateBack) {
                        Text("Go Back")
                    }
                }
            }
        }
        else -> {
            // Initial state - ViewModel might not have data yet
            // This can happen if ViewModel is a new instance (not shared)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    SouthKoreaLoadingIcon(size = 48.dp)
                    Text(
                        text = "Loading exercise...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "If this persists, the ViewModel may not be shared.\nPlease navigate from Lesson screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF999999),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListeningScreenContent(
    exercise: ExerciseResponse,
    lessonId: String?,
    viewModel: LessonViewModel,
    onNavigateBack: () -> Unit,
    onNextExercise: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    
    // TTS Manager - injected via ViewModel
    val ttsManager = viewModel.ttsManager
    val isPlaying by ttsManager.isPlaying.collectAsState()
    val playbackPositionState by ttsManager.playbackPosition.collectAsState()
    val primaryAudioUrl = remember(exercise.audioUrl) {
        exercise.audioUrl?.takeIf { it.isNotBlank() }
    }
    val hasPrimaryAudio = primaryAudioUrl != null
    val mediaPlayer = remember { MediaPlayer() }
    var isMediaPrepared by remember(primaryAudioUrl) { mutableStateOf(false) }
    var isMediaPlaying by remember(primaryAudioUrl) { mutableStateOf(false) }
    
    // Sound Manager for correct/incorrect sounds
    val soundManager = rememberSoundManager()
    
    // Playback state
    var playbackPosition by remember { mutableStateOf(0) }
    var audioDuration by remember { mutableStateOf(0) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    
    // Exercise state
    var hasListened by remember { mutableStateOf(false) }
    var replayCount by remember { mutableStateOf(0) } // Track number of times user has listened (including replays)
    var hasCompletedFirstListen by remember { mutableStateOf(false) } // Track first completion (not replay)
    var showQuestions by remember { mutableStateOf(false) } // Show after first listen completes
    var showResultScreen by remember { mutableStateOf(false) }
    var showTranscript by remember { mutableStateOf(false) }
    var showDetailPanel by remember { mutableStateOf(false) }
    var showStreakScreen by remember { mutableStateOf(false) }

    var streakEventToShow by remember { mutableStateOf<StreakCelebrationEvent?>(null) }
    var pendingNavigationAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingComboAdvance by remember { mutableStateOf<(() -> Unit)?>(null) }

    var listeningExp by remember { mutableStateOf(0) }
    var correctAnswerCount by remember { mutableStateOf(0) }
    var totalTimeMillis by remember { mutableStateOf(0L) }
    var exerciseStartTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var isUpdatingProgress by remember { mutableStateOf(false) }
    val streakCelebrationFlow = remember { viewModel.streakCelebration }
    val comboCelebration by viewModel.comboCelebration.collectAsStateWithLifecycle()

    // Question navigation state - show one question at a time
    var currentQuestionIndex by remember { mutableStateOf(0) }
    val currentQuestion = if (exercise.questions.isNotEmpty() && currentQuestionIndex < exercise.questions.size) {
        exercise.questions[currentQuestionIndex]
    } else null
    
    // Map of questionId to selectedOptionId
    var selectedAnswers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    // Map of questionId to whether answer was checked (for sound feedback)
    var checkedQuestions by remember { mutableStateOf<Set<String>>(emptySet()) }
    // All questions completed
    val allQuestionsAnswered = exercise.questions.isNotEmpty() && 
                               selectedAnswers.keys.size == exercise.questions.size &&
                               checkedQuestions.size == exercise.questions.size
    
    // Animation state
    var appBarVisible by remember { mutableStateOf(false) }
    var audioCardVisible by remember { mutableStateOf(false) }
    
    // Get text to speech - use textToSpeech field, fallback to transcript
    val textToSpeak = remember(exercise.textToSpeech, exercise.transcript) {
        exercise.textToSpeech ?: exercise.transcript ?: ""
    }

    val transcriptViewModel: TranscriptSyncViewModel = viewModel()
    val transcriptUiState by transcriptViewModel.uiState.collectAsStateWithLifecycle()
    
    // Initialize and calculate duration
    LaunchedEffect(textToSpeak, exercise.questions, primaryAudioUrl) {
        delay(100)
        appBarVisible = true
        delay(200)
        audioCardVisible = true

        hasListened = false
        replayCount = 0
        hasCompletedFirstListen = false
        showQuestions = false
        showResultScreen = false
        showDetailPanel = false
        showStreakScreen = false
        showTranscript = false
        currentQuestionIndex = 0
        selectedAnswers = emptyMap()
        checkedQuestions = emptySet()
        listeningExp = 0
        correctAnswerCount = 0
        totalTimeMillis = 0L
        exerciseStartTimestamp = System.currentTimeMillis()
        isUpdatingProgress = false
        streakEventToShow = null
        pendingNavigationAction = null

        viewModel.recordListeningExp(0)
        
        // Estimate duration for TTS when no primary audio is available
        if (!hasPrimaryAudio && textToSpeak.isNotBlank()) {
            val charsPerWord = 4f // Korean average
            val words = textToSpeak.length / charsPerWord
            val minutes = words / 150f // 150 words per minute
            audioDuration = (minutes * 60f * 1000f).toInt()
            Timber.d("Estimated TTS duration: ${audioDuration}ms for ${textToSpeak.length} chars")
        }
        
        // Log questions info
        Timber.d("Exercise questions: ${exercise.questions.size} questions found")
        exercise.questions.forEachIndexed { index, q ->
            Timber.d("  Q${index + 1}: ${q.questionText}, options: ${q.options.size}")
        }
    }
    
    LaunchedEffect(exercise.transcript) {
        transcriptViewModel.updateTranscript(exercise.transcript)
    }
    
    // Update playback position from TTS manager
    if (!hasPrimaryAudio) {
        LaunchedEffect(isPlaying) {
            while (isPlaying) {
                ttsManager.updatePosition()
                playbackPosition = playbackPositionState
                delay(100)
            }
        }
        
        // Sync playback position
        LaunchedEffect(playbackPositionState) {
            playbackPosition = playbackPositionState
        }
        
        LaunchedEffect(playbackPosition, audioDuration) {
            transcriptViewModel.onPlaybackProgress(
                positionMs = playbackPosition.toLong(),
                totalDurationMs = audioDuration
            )
        }
        
        // Handle first time listening completion - show questions with animation
        // TTSManager sets playbackPosition to estimatedDuration when TTS completes (in onDone callback)
        LaunchedEffect(isPlaying, playbackPositionState, hasListened) {
            val isCompleted = !isPlaying && 
                hasListened && 
                audioDuration > 0 &&
                playbackPositionState >= audioDuration * 0.95 &&
                !hasCompletedFirstListen &&
                exercise.questions.isNotEmpty()
            
            if (isCompleted) {
                hasCompletedFirstListen = true
                Timber.d("TTS completed! Position: $playbackPositionState/$audioDuration, showing questions (count: ${exercise.questions.size})")
                delay(300)
                showQuestions = true
            }
        }
        
        // Debug logging - log all state changes
        LaunchedEffect(isPlaying, hasCompletedFirstListen, showQuestions, exercise.questions.size) {
            Timber.d("TTS state: isPlaying=$isPlaying, hasListened=$hasListened, hasCompletedFirstListen=$hasCompletedFirstListen, showQuestions=$showQuestions, questionsCount=${exercise.questions.size}, currentQuestionIndex=$currentQuestionIndex")
        }
    }
    
    if (hasPrimaryAudio) {
        LaunchedEffect(primaryAudioUrl) {
            try {
                mediaPlayer.reset()
                isMediaPrepared = false
                isMediaPlaying = false
                playbackPosition = 0
                audioDuration = 0

                mediaPlayer.setDataSource(primaryAudioUrl)
                mediaPlayer.setOnPreparedListener { player ->
                    Timber.d("ListeningScreen: Audio prepared (duration=${player.duration})")
                    audioDuration = player.duration
                    isMediaPrepared = true
                }
                mediaPlayer.setOnCompletionListener { player ->
                    Timber.d("ListeningScreen: Audio playback completed")
                    isMediaPlaying = false
                    playbackPosition = player.duration
                    if (!hasCompletedFirstListen && hasListened && exercise.questions.isNotEmpty()) {
                        hasCompletedFirstListen = true
                        showQuestions = true
                    }
                }
                mediaPlayer.setOnErrorListener { _, what, extra ->
                    Timber.e("ListeningScreen: MediaPlayer error what=$what extra=$extra")
                    isMediaPrepared = false
                    isMediaPlaying = false
                    playbackPosition = 0
                    audioDuration = 0
                    true
                }
                mediaPlayer.prepareAsync()
            } catch (e: Exception) {
                Timber.e(e, "ListeningScreen: Failed to prepare audio: $primaryAudioUrl")
                isMediaPrepared = false
            }
        }

        LaunchedEffect(isMediaPlaying) {
            while (isMediaPlaying) {
                try {
                    playbackPosition = mediaPlayer.currentPosition
                } catch (e: IllegalStateException) {
                    Timber.e(e, "ListeningScreen: Unable to read current position")
                    break
                }
                delay(100)
            }
        }
        
        LaunchedEffect(playbackPosition, audioDuration) {
            transcriptViewModel.onPlaybackProgress(
                positionMs = playbackPosition.toLong(),
                totalDurationMs = audioDuration
            )
        }

        LaunchedEffect(isMediaPrepared) {
            if (isMediaPrepared) {
                try {
                    audioDuration = mediaPlayer.duration
                } catch (e: IllegalStateException) {
                    Timber.e(e, "ListeningScreen: Unable to read duration after prepared")
                }
            }
        }
    }

    // Cleanup Sound Manager and media/tts resources on dispose
    DisposableEffect(Unit) {
        onDispose {
            soundManager.cleanup()
            if (ttsManager.isAvailable()) {
                ttsManager.stop()
            }
            try {
                mediaPlayer.reset()
                mediaPlayer.release()
            } catch (e: Exception) {
                Timber.e(e, "ListeningScreen: Error releasing media player")
            }
        }
    }
    
    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = appBarVisible,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(300)
                )
            ) {
                ListeningTopAppBar(
                    title = exercise.title ?: "Listening Practice",
                    subtitle = "Listening Practice",
                    onNavigateBack = onNavigateBack
                )
            }
        },
        containerColor = Color(0xFFF9FAFB)
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            Spacer(modifier = Modifier.height(8.dp))

            fun finalizeExercise() {
                if (showResultScreen) return

                val totalQuestions = exercise.questions.size
                val correct = if (totalQuestions > 0) {
                    exercise.questions.count { question ->
                        val selectedOptionId = selectedAnswers[question.id]
                        val correctOptionId = question.options.firstOrNull { it.isCorrect }?.id
                        selectedOptionId == correctOptionId
                    }
                } else {
                    0
                }

                correctAnswerCount = correct
                val calculatedExp = (correct * LISTENING_EXP_PER_CORRECT).coerceAtLeast(0)
                listeningExp = calculatedExp
                totalTimeMillis = (System.currentTimeMillis() - exerciseStartTimestamp).coerceAtLeast(0L)
                showQuestions = false
                showResultScreen = true
                showDetailPanel = false

                viewModel.recordListeningExp(calculatedExp)

                if (ttsManager.isAvailable()) {
                    ttsManager.stop()
                }
                if (hasPrimaryAudio && isMediaPrepared) {
                    try {
                        if (mediaPlayer.isPlaying) {
                            mediaPlayer.pause()
                        }
                    } catch (e: IllegalStateException) {
                        Timber.e(e, "ListeningScreen: Unable to pause media when finalizing")
                    }
                }
                isMediaPlaying = false
            }

            fun dismissStreakCelebration(navigateAfter: Boolean) {
                showStreakScreen = false
                val action = pendingNavigationAction
                pendingNavigationAction = null
                streakEventToShow = null
                viewModel.clearStreakCelebration()
                if (navigateAfter) {
                    action?.invoke()
                }
            }

            fun handleContinue() {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                if (isUpdatingProgress) return

                val continueAction: () -> Unit = {
                    pendingNavigationAction = null
                    showResultScreen = false
                    showDetailPanel = false
                    onNextExercise?.invoke() ?: onNavigateBack()
                }

                val id = lessonId
                if (id != null) {
                    isUpdatingProgress = true
                    showDetailPanel = false
                    viewModel.updateLessonProgress(id) { celebrationScheduled ->
                        isUpdatingProgress = false
                        if (celebrationScheduled) {
                            pendingNavigationAction = continueAction
                            coroutineScope.launch {
                                val event = streakEventToShow ?: streakCelebrationFlow.filterNotNull().first()
                                streakEventToShow = event
                                viewModel.markStreakCelebrationDisplayed()
                                showResultScreen = false
                                showStreakScreen = true
                            }
                        } else {
                            continueAction()
                        }
                    }
                } else {
                    continueAction()
                }
            }

            val streakEvent = streakEventToShow
            if (showStreakScreen && streakEvent != null) {
                StreakCelebrationScreen(
                    streakDays = streakEvent.streakDays,
                    onContinueClick = { dismissStreakCelebration(true) },
                    onExitConfirmed = { dismissStreakCelebration(true) },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (showResultScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    LessonResultScreen(
                        totalTime = totalTimeMillis,
                        experienceGained = listeningExp,
                        onContinue = { handleContinue() }
                    )

                    if (exercise.questions.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                showDetailPanel = true
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            Text("Xem chi tiết")
                        }
                    }
                }

                if (showDetailPanel) {
                    Dialog(onDismissRequest = { showDetailPanel = false }) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            tonalElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            FeedbackCard(
                                questions = exercise.questions,
                                selectedAnswers = selectedAnswers,
                                transcript = exercise.transcript,
                                onListenAgain = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    if (hasPrimaryAudio && isMediaPrepared) {
                                        try {
                                            mediaPlayer.seekTo(0)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(playbackSpeed)
                                            }
                                            mediaPlayer.start()
                                            isMediaPlaying = true
                                            playbackPosition = 0
                                            replayCount++
                                        } catch (e: Exception) {
                                            Timber.e(e, "ListeningScreen: Unable to replay audio from detail panel")
                                        }
                                    } else if (textToSpeak.isNotBlank()) {
                                        ttsManager.stop()
                                        ttsManager.speak(textToSpeak, playbackSpeed)
                                    }
                                },
                                onNextExercise = {
                                    showDetailPanel = false
                                    handleContinue()
                                },
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            } else {
                val handleSeek: (Int) -> Unit = { position ->
                    if (hasPrimaryAudio && isMediaPrepared) {
                        try {
                            val boundedPosition = position.coerceIn(0, audioDuration.takeIf { it > 0 } ?: position)
                            mediaPlayer.seekTo(boundedPosition)
                            playbackPosition = mediaPlayer.currentPosition
                            transcriptViewModel.onPlaybackProgress(
                                positionMs = mediaPlayer.currentPosition.toLong(),
                                totalDurationMs = audioDuration
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "ListeningScreen: Unable to seek audio")
                        }
                    } else if (textToSpeak.isNotBlank()) {
                        val boundedPosition = position.coerceIn(0, audioDuration.takeIf { it > 0 } ?: position)
                        ttsManager.stop()
                        ttsManager.speak(textToSpeak, playbackSpeed)
                        playbackPosition = boundedPosition
                        transcriptViewModel.onPlaybackProgress(
                            positionMs = boundedPosition.toLong(),
                            totalDurationMs = audioDuration
                        )
                    }
                }

                // Audio Player Card
                AnimatedVisibility(
                    visible = audioCardVisible,
                    enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(400)
                    )
                ) {
                    AudioPlayerCard(
                        isPlaying = if (hasPrimaryAudio) isMediaPlaying else isPlaying,
                        playbackPosition = playbackPosition,
                        duration = audioDuration,
                        playbackSpeed = playbackSpeed,
                        showTranscriptButton = false,
                        transcript = exercise.transcript,
                        onToggleTranscript = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            showTranscript = !showTranscript
                        },
                        onPlayPauseClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            if (hasPrimaryAudio) {
                                if (!isMediaPrepared) {
                                    Timber.w("ListeningScreen: Audio not prepared yet")
                                    return@AudioPlayerCard
                                }
                                if (isMediaPlaying) {
                                    mediaPlayer.pause()
                                    isMediaPlaying = false
                                } else {
                                    if (ttsManager.isAvailable()) {
                                        ttsManager.stop()
                                    }
                                    try {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(playbackSpeed)
                                        }
                                    } catch (e: Exception) {
                                        Timber.e(e, "ListeningScreen: Unable to set playback speed for MediaPlayer")
                                    }
                                    mediaPlayer.start()
                                    isMediaPlaying = true
                                    if (!hasListened) {
                                        hasListened = true
                                    }
                                    replayCount++
                                    Timber.d("ListeningScreen: Starting audio playback, replayCount=$replayCount")
                                }
                            } else if (isPlaying) {
                                ttsManager.pause()
                            } else {
                                if (textToSpeak.isNotBlank()) {
                                    ttsManager.speak(textToSpeak, playbackSpeed)
                                    if (!hasListened) {
                                        hasListened = true
                                    }
                                    replayCount++
                                    Timber.d("Starting TTS playback, replayCount=$replayCount")
                                } else {
                                    Timber.w("No text to speak")
                                }
                            }
                        },
                        onReplayClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            if (hasPrimaryAudio) {
                                if (!isMediaPrepared) {
                                    Timber.w("ListeningScreen: Audio not prepared for replay")
                                    return@AudioPlayerCard
                                }
                                try {
                                    mediaPlayer.seekTo(0)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(playbackSpeed)
                                    }
                                    mediaPlayer.start()
                                    isMediaPlaying = true
                                    playbackPosition = 0
                                    replayCount++
                                    Timber.d("ListeningScreen: Replaying audio (replayCount=$replayCount, hasCompletedFirstListen=$hasCompletedFirstListen)")
                                } catch (e: Exception) {
                                    Timber.e(e, "ListeningScreen: Unable to replay audio")
                                }
                            } else if (textToSpeak.isNotBlank()) {
                                ttsManager.stop()
                                ttsManager.speak(textToSpeak, playbackSpeed)
                                playbackPosition = 0
                                replayCount++
                                Timber.d("Replaying TTS (replayCount=$replayCount, hasCompletedFirstListen=$hasCompletedFirstListen)")
                            }
                        },
                        onSpeedToggle = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            playbackSpeed = when (playbackSpeed) {
                                0.5f -> 1.0f
                                1.0f -> 1.5f
                                else -> 0.5f
                            }
                            if (hasPrimaryAudio) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isMediaPrepared) {
                                    try {
                                        mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(playbackSpeed)
                                    } catch (e: Exception) {
                                        Timber.e(e, "ListeningScreen: Unable to update MediaPlayer speed")
                                    }
                                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                    Timber.w("ListeningScreen: Playback speed control not supported on this device")
                                }
                            } else {
                                ttsManager.setSpeed(playbackSpeed)
                                if (isPlaying && textToSpeak.isNotBlank()) {
                                    ttsManager.stop()
                                    ttsManager.speak(textToSpeak, playbackSpeed)
                                }
                            }
                        },
                        onSeekTo = handleSeek,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (!exercise.transcript.isNullOrBlank()) {
                    val transcriptToggleColor = Color(0xFF42A5F5)
                    Text(
                        text = if (showTranscript) "Ẩn hội thoại" else "Xem hội thoại",
                        style = MaterialTheme.typography.labelLarge,
                        color = transcriptToggleColor,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(transcriptToggleColor.copy(alpha = 0.1f))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                showTranscript = !showTranscript
                                if (showTranscript) {
                                    transcriptViewModel.onPlaybackProgress(
                                        positionMs = playbackPosition.toLong(),
                                        totalDurationMs = audioDuration
                                    )
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    AnimatedVisibility(
                        visible = showTranscript,
                        enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ),
                        exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(
                            animationSpec = tween(300)
                        )
                    ) {
                        TranscriptSection(
                            segments = transcriptUiState.segments,
                            highlightedIndex = transcriptUiState.highlightedIndex,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            onSegmentClick = { segmentIndex ->
                                transcriptViewModel.onSeekToSegment(segmentIndex)?.let { target ->
                                    handleSeek(target.toInt())
                                }
                            }
                        )
                    }
                }

                // Instruction Section
                exercise.content
                    ?.takeIf { it.isNotBlank() }
                    ?.let { content ->
                        InstructionCard(
                            content = content,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                // Questions Section (appears after first listen completion)
                if (exercise.questions.isEmpty()) {
                    Timber.w("No questions found in exercise!")
                } else {
                    Timber.d("Questions available: ${exercise.questions.size}, currentQuestionIndex: $currentQuestionIndex, showQuestions: $showQuestions, hasCompletedFirstListen: $hasCompletedFirstListen")
                }

                if (exercise.questions.isNotEmpty() && currentQuestion != null) {
                    AnimatedVisibility(
                        visible = showQuestions && !showResultScreen,
                        enter = fadeIn(animationSpec = tween(500)) + expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    ) {
                        SingleQuestionCard(
                            question = currentQuestion,
                            questionNumber = currentQuestionIndex + 1,
                            totalQuestions = exercise.questions.size,
                            selectedOptionId = selectedAnswers[currentQuestion.id],
                            isChecked = checkedQuestions.contains(currentQuestion.id),
                            onOptionSelected = { optionId ->
                                if (!checkedQuestions.contains(currentQuestion.id)) {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    selectedAnswers = selectedAnswers + (currentQuestion.id to optionId)

                                    val selectedOption = currentQuestion.options.find { it.id == optionId }
                                    val isCorrect = selectedOption?.isCorrect == true

                                    checkedQuestions = checkedQuestions + currentQuestion.id

                                    if (isCorrect) {
                                        soundManager.playCorrect()
                                    } else {
                                        soundManager.playIncorrect()
                                    }

                                    val comboResult = viewModel.onAnswerEvaluated(isCorrect)

                                    coroutineScope.launch {
                                        delay(500)

                                        val proceed: suspend () -> Unit = {
                                            if (currentQuestionIndex < exercise.questions.size - 1) {
                                                currentQuestionIndex++
                                            } else {
                                                delay(500)
                                                if (lessonId != null) {
                                                    viewModel.submitExercise(
                                                        exerciseId = exercise.id,
                                                        lessonId = lessonId,
                                                        selectedAnswers = selectedAnswers
                                                    )
                                                }
                                                finalizeExercise()
                                            }
                                        }

                                        if (isCorrect && comboResult != null) {
                                            pendingComboAdvance = {
                                                coroutineScope.launch {
                                                    pendingComboAdvance = null
                                                    proceed()
                                                }
                                            }
                                        } else {
                                            pendingComboAdvance = null
                                            proceed()
                                        }
                                    }
                                }
                            },
                            onCheckAnswer = {
                                // Handled in onOptionSelected
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else if (exercise.questions.isEmpty()) {
                    var fallbackAnswerText by remember { mutableStateOf("") }
                    AnimatedVisibility(
                        visible = showQuestions && !showResultScreen,
                        enter = fadeIn(animationSpec = tween(500)) + expandVertically(
                            animationSpec = tween(500)
                        )
                    ) {
                        AnswerCard(
                            sampleAnswer = exercise.sampleAnswer,
                            answerText = fallbackAnswerText,
                            onAnswerTextChange = { fallbackAnswerText = it },
                            onSubmitAnswer = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                if (lessonId != null) {
                                    viewModel.submitExercise(
                                        exerciseId = exercise.id,
                                        lessonId = lessonId,
                                        response = fallbackAnswerText
                                    )
                                }
                                finalizeExercise()
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        comboCelebration?.let { celebration ->
            ComboCelebrationOverlay(
                state = celebration,
                modifier = Modifier.align(Alignment.Center),
                onAnimationFinished = {
                    val action = pendingComboAdvance
                    pendingComboAdvance = null
                    action?.invoke()
                    viewModel.clearComboCelebration()
                }
            )
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningTopAppBar(
    title: String,
    subtitle: String,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
        Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
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
                    colors = listOf(
                        Color(0xFF42A5F5),
                        Color(0xFF64B5F6)
                    )
                )
            )
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(0.dp))
    )
}

@Composable
private fun AudioPlayerCard(
    isPlaying: Boolean,
    playbackPosition: Int,
    duration: Int,
    playbackSpeed: Float,
    showTranscriptButton: Boolean = false,
    transcript: String? = null,
    showTranscript: Boolean = false,
    onToggleTranscript: () -> Unit = {},
    onPlayPauseClick: () -> Unit,
    onReplayClick: () -> Unit,
    onSpeedToggle: () -> Unit,
    onSeekTo: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(0xFF42A5F5)
    val buttonSize = 48.dp

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onReplayClick,
                        modifier = Modifier
                            .size(buttonSize)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.12f)),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = primaryColor
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.replay),
                            contentDescription = "Replay"
                        )
                    }

                    val scale by animateFloatAsState(
                        targetValue = if (isPlaying) 1.08f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "playButtonScale"
                    )

                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier
                            .size(buttonSize)
                            .clip(CircleShape)
                            .background(primaryColor),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        if (isPlaying) {
                            Icon(
                                painter = painterResource(id = R.drawable.pause),
                                contentDescription = "Pause",
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(scale)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(scale)
                            )
                        }
                    }

                    IconButton(
                        onClick = onSpeedToggle,
                        modifier = Modifier
                            .size(buttonSize)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.12f)),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = primaryColor
                        )
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Slider(
                            value = if (duration > 0) playbackPosition.toFloat() / duration else 0f,
                            onValueChange = { newValue ->
                                val newPosition = (newValue * duration).toInt()
                                onSeekTo(newPosition)
                            },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = primaryColor,
                                activeTrackColor = primaryColor,
                                inactiveTrackColor = Color(0xFFE0E0E0)
                            )
                        )

                        AnimatedWaveform(
                            modifier = Modifier
                                .height(20.dp)
                                .width(32.dp)
                                .alpha(if (isPlaying) 1f else 0.3f),
                            barColor = primaryColor
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(playbackPosition),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666)
                        )
                        Text(
                            text = formatTime(duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showTranscript && !transcript.isNullOrBlank(),
                    enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(
                        animationSpec = tween(300)
                    )
                ) {
                    val transcriptScrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(primaryColor.copy(alpha = 0.08f))
                            .verticalScroll(transcriptScrollState)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = transcript.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666),
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedWaveform(
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF42A5F5)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.75f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 520,
                        delayMillis = index * 80,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "waveformScale$index"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(16.dp * scale)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun TranscriptSection(
    segments: List<TranscriptSegment>,
    highlightedIndex: Int,
    modifier: Modifier = Modifier,
    onSegmentClick: (Int) -> Unit = {}
) {
    val lazyListState = rememberLazyListState()

    LaunchedEffect(highlightedIndex, segments.size) {
        val targetPosition = segments.indexOfFirst { it.index == highlightedIndex }
        if (targetPosition >= 0) {
            val viewportHeight = lazyListState.layoutInfo.viewportSize.height
            val offset = if (viewportHeight == 0) 0 else -(viewportHeight / 4)
            lazyListState.animateScrollToItem(
                index = targetPosition,
                scrollOffset = offset
            )
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        )
    ) {
        if (segments.isEmpty()) {
            Box(
                modifier = Modifier
                    .height(100.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Không có hội thoại",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .height(100.dp)
                    .padding(vertical = 8.dp),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(segments, key = { _, segment -> segment.index }) { _, segment ->
                    val isActive = segment.index == highlightedIndex
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
                        animationSpec = tween(250),
                        label = "transcriptBackground"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF333333),
                        animationSpec = tween(250),
                        label = "transcriptTextColor"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(backgroundColor)
                                .clickable { onSegmentClick(segment.index) },
                            color = Color.Transparent
                        ) {
                            Text(
                                text = segment.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionCard(
    content: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.headphone),
                contentDescription = "Listening",
                tint = Color(0xFF42A5F5),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = content,
                    style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SingleQuestionCard(
    question: ExerciseQuestionResponse,
    questionNumber: Int,
    totalQuestions: Int,
    selectedOptionId: String?,
    isChecked: Boolean,
    onOptionSelected: (String) -> Unit,
    onCheckAnswer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val correctOptionId = question.options.find { it.isCorrect }?.id
    val selectedOption = question.options.find { it.id == selectedOptionId }
    val isCorrect = selectedOption?.isCorrect == true

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Question header with progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question $questionNumber of $totalQuestions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF999999)
                    )
                }

                // Question text
                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Options
                question.options.sortedBy { it.orderIndex }.forEachIndexed { optionIndex, option ->
                    val isSelected = selectedOptionId == option.id
                    val showFeedback = isChecked && isSelected

                    SingleQuestionOptionCard(
                        option = option,
                        optionLabel = ('A' + optionIndex).toString(),
                        isSelected = isSelected,
                        isCorrect = option.isCorrect,
                        showFeedback = showFeedback,
                        isDisabled = isChecked,
                        onClick = {
                            if (!isChecked) {
                                // onOptionSelected will handle checking the answer
                                onOptionSelected(option.id)
                            }
                        }
                    )
                }

                // Explanation (shown after checking)
                if (isChecked && !question.explanation.isNullOrBlank()) {
                    AnimatedVisibility(
                        visible = isChecked,
                        enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                            animationSpec = tween(300)
                        )
                    ) {
                        Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCorrect) {
                                    Color(0xFFE8F5E9)
                                } else {
                                    Color(0xFFFFEBEE)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                                        contentDescription = if (isCorrect) "Correct" else "Incorrect",
                                        tint = if (isCorrect) Color(0xFF4CAF50) else Color(
                                            0xFFE74C3C
                                        ),
                                        modifier = Modifier.size(20.dp)
                                    )
                    Text(
                                        text = if (isCorrect) "정답입니다!" else "틀렸습니다",
                                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                                        color = if (isCorrect) Color(0xFF4CAF50) else Color(
                                            0xFFE74C3C
                                        )
                                    )
                                }
                                if (question.explanation.isNotBlank()) {
                                    Text(
                                        text = question.explanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF666666)
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
private fun SingleQuestionOptionCard(
        option: ExerciseQuestionOptionResponse,
        optionLabel: String,
        isSelected: Boolean,
        isCorrect: Boolean,
        showFeedback: Boolean,
        isDisabled: Boolean = false,
        onClick: () -> Unit
    ) {
        val backgroundColor by animateColorAsState(
            targetValue = when {
                showFeedback && isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                showFeedback && isSelected && !isCorrect -> Color(0xFFE74C3C).copy(alpha = 0.15f)
                isSelected -> Color(0xFF42A5F5).copy(alpha = 0.1f)
                else -> Color(0xFFF5F5F5)
            },
            animationSpec = tween(300),
            label = "option_background"
        )

        val borderColor by animateColorAsState(
            targetValue = when {
                showFeedback && isCorrect -> Color(0xFF4CAF50)
                showFeedback && isSelected && !isCorrect -> Color(0xFFE74C3C)
                isSelected -> Color(0xFF42A5F5)
                else -> Color(0xFFE0E0E0)
            },
            animationSpec = tween(300),
            label = "option_border"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isDisabled, onClick = onClick)
                .alpha(if (isDisabled && !isSelected && !showFeedback) 0.6f else 1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            border = BorderStroke(
                width = 2.dp,
                color = borderColor
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Option label (A, B, C, D)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = borderColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = optionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected || showFeedback) Color.White else Color(0xFF666666)
                    )
                }
                
                // Option text
                Text(
                    text = option.optionText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF333333),
                    modifier = Modifier.weight(1f)
                )

                // Feedback icon
                if (showFeedback) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = if (isCorrect) "Correct" else "Incorrect",
                        tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFE74C3C),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }


@Composable
private fun QuestionsCard(
        questions: List<ExerciseQuestionResponse>,
        selectedAnswers: Map<String, String>,
        submittedQuestions: Set<String>,
        onOptionSelected: (questionId: String, optionId: String) -> Unit,
        onSubmitAnswers: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Answer the questions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Display each question
                questions.forEachIndexed { index, question ->
                    QuestionItem(
                        question = question,
                        questionNumber = index + 1,
                        selectedOptionId = selectedAnswers[question.id],
                        isSubmitted = submittedQuestions.contains(question.id),
                        onOptionSelected = { optionId ->
                            onOptionSelected(question.id, optionId)
                        }
                    )

                    if (index < questions.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color(0xFFE0E0E0)
                        )
                    }
                }

                // Submit button - only enabled when all questions answered
                Button(
                    onClick = onSubmitAnswers,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF42A5F5),
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    enabled = selectedAnswers.keys.size == questions.size && submittedQuestions.isEmpty()
                ) {
                    Text(
                        text = "Submit Answers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.White
                    )
                }
                
                // Hint text if not all questions answered
                if (selectedAnswers.keys.size < questions.size && submittedQuestions.isEmpty()) {
                    Text(
                        text = "Please answer all ${questions.size} question${if (questions.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF999999),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }


@Composable
private fun QuestionItem(
        question: ExerciseQuestionResponse,
        questionNumber: Int,
        selectedOptionId: String?,
        isSubmitted: Boolean,
        onOptionSelected: (String) -> Unit
    ) {
        val correctOptionId = question.options.find { it.isCorrect }?.id

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Question text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                    text = "Q$questionNumber:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF42A5F5)
                )
                Text(
                    text = question.questionText,
                        style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF333333),
                    modifier = Modifier.weight(1f)
                    )
                }
                
            // Options
            question.options.sortedBy { it.orderIndex }.forEachIndexed { optionIndex, option ->
                val isSelected = selectedOptionId == option.id
                val isCorrect = option.isCorrect
                val showFeedback = isSubmitted

                QuestionOptionCard(
                    option = option,
                    optionLabel = ('A' + optionIndex).toString(),
                    isSelected = isSelected,
                    isCorrect = isCorrect,
                    showFeedback = showFeedback,
                    onClick = {
                        if (!showFeedback) {
                            onOptionSelected(option.id)
                        }
                    }
                )
            }

            // Explanation (shown after submission)
            if (isSubmitted && !question.explanation.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF0F7FF)
                        )
                    ) {
                        Text(
                        text = question.explanation,
                        modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF42A5F5)
                        )
                    }
            }
        }
    }

@Composable
private fun QuestionOptionCard(
        option: ExerciseQuestionOptionResponse,
        optionLabel: String,
        isSelected: Boolean,
        isCorrect: Boolean,
        showFeedback: Boolean,
        onClick: () -> Unit
    ) {
        val backgroundColor by animateColorAsState(
            targetValue = when {
                showFeedback && isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                showFeedback && isSelected && !isCorrect -> Color(0xFFE74C3C).copy(alpha = 0.15f)
                isSelected -> Color(0xFF42A5F5).copy(alpha = 0.1f)
                else -> Color(0xFFF5F5F5)
            },
            animationSpec = tween(300),
            label = "option_background"
        )

        val borderColor by animateColorAsState(
            targetValue = when {
                showFeedback && isCorrect -> Color(0xFF4CAF50)
                showFeedback && isSelected && !isCorrect -> Color(0xFFE74C3C)
                isSelected -> Color(0xFF42A5F5)
                else -> Color.Transparent
            },
            animationSpec = tween(300),
            label = "option_border"
        )

        val textColor by animateColorAsState(
            targetValue = when {
                showFeedback && isCorrect -> Color(0xFF4CAF50)
                showFeedback && isSelected && !isCorrect -> Color(0xFFE74C3C)
                isSelected -> Color(0xFF42A5F5)
                else -> Color(0xFF333333)
            },
            animationSpec = tween(300),
            label = "option_text"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            border = if (borderColor != Color.Transparent) {
                BorderStroke(2.dp, borderColor)
            } else null,
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isSelected) 4.dp else 0.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Option label (A, B, C, D)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            color = if (isSelected) Color(0xFF42A5F5) else Color(0xFFE0E0E0)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = optionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color(0xFF666666)
                    )
                }

                // Option text
                Text(
                    text = option.optionText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
        
                // Feedback icon
                if (showFeedback && (isSelected || isCorrect)) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFE74C3C),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

@Composable
private fun AnswerCard(
        sampleAnswer: String?,
        answerText: String,
        onAnswerTextChange: (String) -> Unit,
        onSubmitAnswer: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (sampleAnswer != null) {
                    // Fill-in-the-blank style input
                    OutlinedTextField(
                        value = answerText,
                        onValueChange = onAnswerTextChange,
                        label = { Text("Your answer") },
                        placeholder = { Text("Type your answer here...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { onSubmitAnswer() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF42A5F5),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                }

                Button(
                    onClick = onSubmitAnswer,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF42A5F5),
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    enabled = sampleAnswer == null || answerText.isNotBlank()
                ) {
                    Text(
                        text = if (sampleAnswer != null) "Submit Answer" else "Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.White
                    )
                }
            }
        }
    }

@Composable
fun FeedbackCard(
    questions: List<ExerciseQuestionResponse>,
    selectedAnswers: Map<String, String>,
    transcript: String?,
    onListenAgain: () -> Unit,
    onNextExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate score
    val totalQuestions = questions.size
    val correctAnswers = questions.count { question ->
        val selectedOptionId = selectedAnswers[question.id]
        val correctOptionId = question.options.find { it.isCorrect }?.id
        selectedOptionId == correctOptionId
    }
    val score = if (totalQuestions > 0) (correctAnswers * 100) / totalQuestions else 0
    val isPerfectScore = correctAnswers == totalQuestions && totalQuestions > 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Score header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedFeedbackIcon(isCorrect = isPerfectScore)

                Text(
                    text = if (isPerfectScore) "Perfect! 🎉" else "Results",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isPerfectScore) Color(0xFF4CAF50) else Color(0xFF42A5F5)
                )

                Text(
                    text = "You got $correctAnswers out of $totalQuestions correct",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF666666)
                )

                // Score percentage
                Text(
                    text = "$score%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (score >= 80) Color(0xFF4CAF50) else if (score >= 60) Color(
                        0xFFFF9800
                    ) else Color(0xFFE74C3C)
                )
            }
            
            // Questions review
            if (questions.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Review",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )

                    questions.forEachIndexed { index, question ->
                        val selectedOptionId = selectedAnswers[question.id]
                        val correctOptionId = question.options.find { it.isCorrect }?.id
                        val isCorrect = selectedOptionId == correctOptionId

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCorrect)
                                    Color(0xFF4CAF50).copy(alpha = 0.1f)
                                else
                                    Color(0xFFE74C3C).copy(alpha = 0.1f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isCorrect) Color(0xFF4CAF50) else Color(0xFFE74C3C)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (isCorrect) Color(0xFF4CAF50) else Color(
                                            0xFFE74C3C
                                        ),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Q${index + 1}: ${question.questionText}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF333333),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Show explanation if available
                                if (!question.explanation.isNullOrBlank()) {
                                    Text(
                                        text = question.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF666666)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Transcript
            if (!transcript.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Text(
                        text = transcript,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF333333)
                    )
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onListenAgain,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF42A5F5)
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.volume_up_awesome),
                        contentDescription = "Listen again",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Listen again")
                }

                Button(
                    onClick = onNextExercise,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF42A5F5)
                    )
                ) {
                    Text("Next Exercise")
                }
            }
        }
    }
}

@Composable
private fun AnimatedFeedbackIcon(isCorrect: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "feedback")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "feedbackScale"
    )

    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(500),
        label = "feedbackAlpha"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        if (isCorrect) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Correct",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(80.dp)
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.cancel),
                contentDescription = "Incorrect",
                tint = Color(0xFFE74C3C),
                modifier = Modifier.size(80.dp)
            )
        }
    }
}

/**
 * Format time in milliseconds to MM:SS or H:MM:SS format
 * @param timeInMillis Time in milliseconds
 * @return Formatted time string (e.g., "1:23" or "1:02:34")
 */
private fun formatTime(timeInMillis: Int): String {
    val totalSeconds = timeInMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

