package com.seoulhankuko.app.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.data.api.model.ExerciseResponse
import com.seoulhankuko.app.presentation.components.SouthKoreaLoadingIcon
import com.seoulhankuko.app.presentation.utils.AppColors
import com.seoulhankuko.app.presentation.utils.LessonFlowColors
import com.seoulhankuko.app.presentation.viewmodel.LessonUiState
import com.seoulhankuko.app.presentation.viewmodel.LessonViewModel
import com.seoulhankuko.app.presentation.viewmodel.WritingSubmissionMode
import com.seoulhankuko.app.presentation.viewmodel.WritingSubmissionUiState
import com.seoulhankuko.app.presentation.viewmodel.TeacherScoreBreakdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingScreen(
    lessonId: String,
    onNavigateBack: () -> Unit,
    viewModel: LessonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(lessonId) {
        viewModel.resetWritingSubmissionState()
        val currentLessonId = (viewModel.uiState.value as? LessonUiState.Success)
            ?.lessonWithChallenges
            ?.lesson
            ?.id

        if (currentLessonId != lessonId || viewModel.uiState.value !is LessonUiState.Success) {
            viewModel.loadLesson(lessonId)
        }
    }

    val writingExercise = when (val state = uiState) {
        is LessonUiState.Success -> state.lessonWithChallenges
            ?.exercisesResponse
            ?.firstOrNull { exercise ->
                val type = exercise.type.lowercase()
                type == "writing" || type == "writing_practice"
            }
        else -> null
    }

    val screenTitle = writingExercise?.title
        ?.takeIf { it.isNotBlank() }
        ?: writingExercise?.content?.takeIf { it.isNotBlank() }
        ?: "Viết bài luyện tập"

    val submissionState by viewModel.writingSubmissionState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = LessonFlowColors.BackgroundColor,
        topBar = {
            WritingTopBar(
                title = "Viết bài luyện tập",
                onBackClick = onNavigateBack
            )
        }
    ) { innerPadding ->
    when (val state = uiState) {
        is LessonUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                        .padding(innerPadding)
                    .background(LessonFlowColors.BackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                SouthKoreaLoadingIcon(size = 56.dp)
            }
        }
        
        is LessonUiState.Success -> {
                when {
                    writingExercise != null -> {
                WritingContent(
                            exercise = writingExercise,
                            screenTitle = screenTitle,
                            submissionState = submissionState,
                            onSubmit = { userAnswer, submissionType ->
                                viewModel.submitWritingExercise(
                                    exerciseId = writingExercise.id,
                                    lessonId = lessonId,
                                    content = userAnswer,
                                    mode = submissionType
                                )
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(LessonFlowColors.BackgroundColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = "Không tìm thấy bài tập viết trong bài học này.",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = LessonFlowColors.TextPrimary
                                )
                                Button(
                                    onClick = onNavigateBack,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LessonFlowColors.PrimaryColor
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = "Quay lại",
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
        
        is LessonUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                        .padding(innerPadding)
                    .background(LessonFlowColors.BackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
            }
        }
    }
}

private val ActiveButtonGradient = listOf(Color(0xFF38BDF8), Color(0xFF3B82F6))
private val DisabledButtonGradient = ActiveButtonGradient.map { it.copy(alpha = 0.35f) }

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(32.dp)
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        label = "${text}ButtonScale"
    )
    val gradient = if (enabled) ActiveButtonGradient else DisabledButtonGradient

    Button(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = shape,
                ambientColor = Color(0x220EA5E9),
                spotColor = Color(0x330EA5E9)
            ),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(),
        enabled = enabled
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(gradient),
                    shape = shape
                )
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = AppColors.White
            )
        }
    }
}

@Composable
fun WritingContent(
    exercise: ExerciseResponse,
    screenTitle: String,
    submissionState: WritingSubmissionUiState?,
    onSubmit: (String, WritingSubmissionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var userInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val focusColor = Color(0xFF0EA5E9)
    var isTextFieldFocused by remember { mutableStateOf(false) }
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isTextFieldFocused) focusColor else focusColor.copy(alpha = 0.4f),
        label = "writingBorderColor"
    )
    val promptText = exercise.prompt
        ?.takeIf { it.isNotBlank() }
        ?: exercise.content
        ?: exercise.title.orEmpty()
    val isSubmitting = submissionState?.isSubmitting == true
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LessonFlowColors.BackgroundColor)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = screenTitle,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 26.sp
            ),
            color = LessonFlowColors.TextPrimary
        )
        
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .onFocusChanged { focusState -> isTextFieldFocused = focusState.isFocused },
                    placeholder = {
                        Text(
                            text = "Viết câu trả lời của bạn...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                color = LessonFlowColors.TextSecondary.copy(alpha = 0.6f),
                                textAlign = TextAlign.Start
                            )
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Normal,
                        color = LessonFlowColors.TextPrimary,
                        textAlign = TextAlign.Start
                    ),
                    shape = RoundedCornerShape(18.dp),
                    singleLine = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = animatedBorderColor,
                        unfocusedBorderColor = animatedBorderColor.copy(alpha = 0.55f),
                        focusedContainerColor = AppColors.White,
                        unfocusedContainerColor = AppColors.White,
                        cursorColor = focusColor
                    )
                )
            }
        }
        
        submissionState?.let { state ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.isSubmitting) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = LessonFlowColors.PrimaryColor,
                        trackColor = LessonFlowColors.SecondaryColor.copy(alpha = 0.35f)
                    )
                    Text(
                        text = "Đang gửi bài, vui lòng chờ...",
                        style = MaterialTheme.typography.bodySmall,
                        color = LessonFlowColors.TextSecondary
                    )
                }

                state.errorMessage?.let {
                    WritingInfoBanner(
                        text = it,
                        isError = true
                    )
                }

                state.message?.takeIf { it.isNotBlank() }?.let {
                    WritingInfoBanner(text = it)
                }

                if (state.submissionStatus == "ai_graded" || state.aiScore != null || !state.aiFeedback.isNullOrBlank()) {
                    AiEvaluationCard(
                        score = state.aiScore,
                        feedback = state.aiFeedback
                    )
                }

                if (
                    state.teacherFinalScore != null ||
                    (state.teacherScores?.hasAnyScore == true) ||
                    !state.teacherFeedback.isNullOrBlank()
                ) {
                    TeacherEvaluationCard(
                        status = state.submissionStatus,
                        finalScore = state.teacherFinalScore,
                        teacherFeedback = state.teacherFeedback,
                        scores = state.teacherScores
                    )
                } else if (
                    state.mode == WritingSubmissionMode.TEACHER &&
                    state.errorMessage == null &&
                    (state.submissionStatus == "submitted" || state.message != null)
                ) {
                    WritingInfoBanner(
                        text = "Bài viết đang chờ giáo viên chấm.",
                        isError = false
                    )
                }
            }
        }

        val aiInteractionSource = remember { MutableInteractionSource() }
        val teacherInteractionSource = remember { MutableInteractionSource() }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionButton(
                modifier = Modifier.weight(1f),
                text = "Chấm AI",
                enabled = userInput.isNotBlank() && !isSubmitting,
                interactionSource = aiInteractionSource
            ) {
                onSubmit(userInput.trim(), WritingSubmissionMode.AI)
            }

            ActionButton(
                modifier = Modifier.weight(1f),
                text = "Chấm giáo viên",
                enabled = userInput.isNotBlank() && !isSubmitting,
                interactionSource = teacherInteractionSource
            ) {
                onSubmit(userInput.trim(), WritingSubmissionMode.TEACHER)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WritingTopBar(
    title: String,
    onBackClick: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF8FBFF), Color(0xFFEFF5FF))
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                spotColor = Color(0x220EA5E9),
                ambientColor = Color(0x220EA5E9)
            )
            .background(gradient)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    ),
                    color = Color(0xFF1E293B)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = Color(0xFF1E293B)
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent,
                navigationIconContentColor = Color(0xFF1E293B),
                titleContentColor = Color(0xFF1E293B)
            )
        )
    }
}

@Composable
private fun WritingInfoBanner(
    text: String,
    isError: Boolean = false
) {
    val background = if (isError) Color(0xFFFFE5E5) else Color(0xFFE6F4FF)
    val textColor = if (isError) Color(0xFFD32F2F) else LessonFlowColors.TextPrimary
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun AiEvaluationCard(
    score: Float?,
    feedback: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Kết quả AI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = LessonFlowColors.TextPrimary
            )

            score?.let {
                Text(
                    text = "Điểm chính tả & ngữ pháp: ${String.format("%.2f", it)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LessonFlowColors.TextPrimary
                )
            }

            feedback?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LessonFlowColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun TeacherEvaluationCard(
    status: String?,
    finalScore: Float?,
    teacherFeedback: String?,
    scores: TeacherScoreBreakdown?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Đánh giá giáo viên",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = LessonFlowColors.TextPrimary
            )

            status?.let {
                val label = when (it.lowercase()) {
                    "teacher_graded" -> "Trạng thái: đã chấm"
                    else -> "Trạng thái: $it"
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = LessonFlowColors.TextSecondary
                )
            }

            scores?.let { breakdown ->
                TeacherScoreRow(label = "Chính tả", score = breakdown.spelling)
                TeacherScoreRow(label = "Ngữ pháp", score = breakdown.grammar)
                TeacherScoreRow(label = "Cấu trúc", score = breakdown.structure)
                TeacherScoreRow(label = "Từ vựng", score = breakdown.vocabulary)
            }

            finalScore?.let {
                Text(
                    text = "Điểm tổng kết: ${String.format("%.2f", it)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = LessonFlowColors.TextPrimary
            )
        }

            teacherFeedback?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LessonFlowColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun TeacherScoreRow(
    label: String,
    score: Float?
) {
    score?.let {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = LessonFlowColors.TextPrimary
            )
            Text(
                text = String.format("%.2f", it),
                style = MaterialTheme.typography.bodyMedium,
                color = LessonFlowColors.PrimaryColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}