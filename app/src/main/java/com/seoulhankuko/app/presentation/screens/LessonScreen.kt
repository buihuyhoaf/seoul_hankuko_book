package com.seoulhankuko.app.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.R
import com.seoulhankuko.app.domain.model.LessonWithChallenges
import com.seoulhankuko.app.presentation.components.SouthKoreaLoadingIcon
import com.seoulhankuko.app.presentation.utils.LessonColors
import com.seoulhankuko.app.presentation.viewmodel.LessonUiState
import com.seoulhankuko.app.presentation.viewmodel.LessonViewModel
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

private const val ITEM_ANIMATION_DELAY = 80L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    lessonId: String,
    onNavigateBack: () -> Unit,
    onNavigateToLessonFlow: () -> Unit,
    onNavigateToListening: (exerciseId: String) -> Unit = {},
    onNavigateToSpeaking: (exerciseId: String) -> Unit = {},
    onNavigateToWriting: (exerciseId: String) -> Unit = {},
    viewModel: LessonViewModel = hiltViewModel()
) {
    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lessonTitle = (uiState as? LessonUiState.Success)?.lessonWithChallenges?.lesson?.title
    val topBarTitle = lessonTitle?.takeIf { it.isNotBlank() }
        ?.let { "Bài học: $it" }
        ?: "Bài học: $lessonId"
    
    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                    Text(
                            text = topBarTitle,
                            fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                            color = LessonColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                modifier = Modifier.shadow(elevation = 4.dp, shape = RoundedCornerShape(0.dp))
            )
        },
        containerColor = LessonColors.BackgroundWhite
    ) { paddingValues ->
        when (val state = uiState) {
            is LessonUiState.Loading -> {
                LessonLoadingState(modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize())
            }

            is LessonUiState.Error -> {
                LessonErrorState(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    onRetry = { viewModel.loadLesson(lessonId) },
                    onNavigateBack = onNavigateBack
                )
                }

            is LessonUiState.Success -> {
                val lessonInfo = state.lessonWithChallenges
                if (lessonInfo != null) {
                    LessonContent(
                modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize(),
                        lessonInfo = lessonInfo,
                        onNavigateToLessonFlow = onNavigateToLessonFlow,
                        onNavigateToListening = onNavigateToListening,
                        onNavigateToSpeaking = onNavigateToSpeaking,
                        onNavigateToWriting = onNavigateToWriting
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                            .background(LessonColors.BackgroundWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        LessonEmptyState()
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonContent(
    modifier: Modifier = Modifier,
    lessonInfo: LessonWithChallenges,
    onNavigateToLessonFlow: () -> Unit,
    onNavigateToListening: (exerciseId: String) -> Unit,
    onNavigateToSpeaking: (exerciseId: String) -> Unit,
    onNavigateToWriting: (exerciseId: String) -> Unit
) {
    val timelineItems = remember(lessonInfo) {
        buildTimelineItems(lessonInfo)
    }
    
    LazyColumn(
        modifier = modifier.background(LessonColors.BackgroundWhite),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            LessonSummaryCard(
                lessonTitle = lessonInfo.lesson.title,
                lessonMeaning = lessonInfo.lesson.description,
                progressPercent = lessonInfo.progressPercent
            )
        }
        
        if (timelineItems.isEmpty()) {
            item { LessonEmptyState() }
        } else {
            itemsIndexed(timelineItems, key = { _, item -> item.id }) { index, item ->
                LessonTimelineCard(
                item = item,
                    index = index,
                    onClick = {
                    when (item.type) {
                        TimelineItemType.QUESTION -> onNavigateToLessonFlow()
                            TimelineItemType.LISTENING -> item.exerciseId?.let(onNavigateToListening)
                            TimelineItemType.SPEAKING -> item.exerciseId?.let(onNavigateToSpeaking)
                            TimelineItemType.PRONUNCIATION -> item.exerciseId?.let(onNavigateToSpeaking)
                            TimelineItemType.WRITING -> item.exerciseId?.let(onNavigateToWriting)
                    }
                }
            )
        }
    }
}
}

@Composable
private fun LessonSummaryCard(
    lessonTitle: String,
    lessonMeaning: String?,
    progressPercent: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = lessonTitle,
                fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                color = LessonColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                
            lessonMeaning?.takeIf { it.isNotBlank() }?.let { meaning ->
                Text(
                    text = "($meaning)",
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                color = LessonColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
                
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tiến độ học",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = LessonColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "$progressPercent%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LessonColors.Accent
                    )
                }

                        LinearProgressIndicator(
                    progress = { progressPercent.coerceIn(0, 100) / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                    color = LessonColors.QuestionColor,
                    trackColor = LessonColors.ConnectorLineColor.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun LessonTimelineCard(
    item: TimelineItem,
    index: Int,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "cardScale"
    )
    val cardAlpha = if (item.status == LessonItemStatus.LOCKED) 0.6f else 1f

    LaunchedEffect(Unit) {
        delay(index * ITEM_ANIMATION_DELAY)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .alpha(cardAlpha)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LessonColors.CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                onClick = {
                    if (item.status != LessonItemStatus.LOCKED) {
                        onClick()
                    }
                },
                enabled = item.status != LessonItemStatus.LOCKED,
                interactionSource = interactionSource
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(item.iconColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = item.title,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LessonColors.TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = item.description,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = LessonColors.TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (item.status) {
                                LessonItemStatus.COMPLETED -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "✅", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Hoàn thành",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = LessonColors.CompletedGreen
                                        )
                                    }
                                }

                                LessonItemStatus.IN_PROGRESS -> {
                                    Row(
                                        modifier = Modifier
                                            .background(LessonColors.AccentSoft, RoundedCornerShape(20.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "📈", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Đang học",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = LessonColors.Accent
                                        )
                                    }
                                }

                                LessonItemStatus.LOCKED -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "🔒", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Chưa mở khóa",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = LessonColors.TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (item.status == LessonItemStatus.COMPLETED) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        )

                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Hoàn thành",
                            tint = LessonColors.CompletedGreen,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(24.dp)
                        )
                    }

                    if (item.status == LessonItemStatus.LOCKED) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Chưa mở khóa",
                            tint = LessonColors.TextSecondary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonLoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SouthKoreaLoadingIcon(size = 48.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Đang tải bài học…",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = LessonColors.TextSecondary
                        )
                    }
                }
            }

@Composable
private fun LessonErrorState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Không thể tải bài học. Vui lòng thử lại.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = LessonColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LessonActionChip(
                    text = "Thử lại",
                    backgroundColor = LessonColors.Accent,
                    contentColor = Color.White,
                    onClick = onRetry
                )

                LessonActionChip(
                    text = "Quay lại",
                    backgroundColor = LessonColors.AccentSoft,
                    contentColor = LessonColors.Accent,
                    onClick = onNavigateBack
                )
            }
        }
    }
}

@Composable
private fun LessonActionChip(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = contentColor,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Composable
private fun LessonEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.mascot_sad),
            contentDescription = null,
            tint = LessonColors.Accent,
            modifier = Modifier.size(48.dp)
        )

        Text(
            text = "Nội dung bài học đang được cập nhật.",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = LessonColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Hãy quay lại sau nhé!",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = LessonColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

private enum class TimelineItemType {
    QUESTION,
    LISTENING,
    SPEAKING,
    PRONUNCIATION,
    WRITING
}

private enum class LessonItemStatus {
    COMPLETED,
    IN_PROGRESS,
    LOCKED
}

private data class TimelineItem(
    val id: String,
    val type: TimelineItemType,
    val title: String,
    val description: String,
    val status: LessonItemStatus,
    val iconRes: Int,
    val iconColor: Color,
    val exerciseId: String?
)
        
private fun buildTimelineItems(
    lessonInfo: LessonWithChallenges
): List<TimelineItem> {
    val rawItems = mutableListOf<BaseTimelineItem>()
    
    val questionCompleted = lessonInfo.challenges.isNotEmpty() && lessonInfo.challenges.all { it.completed }
    
    if (lessonInfo.challenges.isNotEmpty()) {
        rawItems.add(
            BaseTimelineItem(
                id = "questions",
                type = TimelineItemType.QUESTION,
                exerciseId = null,
                isCompleted = questionCompleted
            )
        )
    }
    
    lessonInfo.exercises
        .sortedBy { it.orderIndex }
        .forEach { exercise ->
        val type = when (exercise.type.lowercase()) {
            "listening", "audio_comprehension" -> TimelineItemType.LISTENING
            "speaking" -> TimelineItemType.SPEAKING
            "pronunciation" -> TimelineItemType.PRONUNCIATION
            "writing", "writing_practice" -> TimelineItemType.WRITING
            else -> null
        }
        
        if (type != null) {
                rawItems.add(
                    BaseTimelineItem(
                    id = "exercise_${exercise.id}",
                    type = type,
                        exerciseId = exercise.id,
                        isCompleted = false
                    )
                )
            }
        }

    if (rawItems.isEmpty()) {
        return emptyList()
    }

    val totalItems = rawItems.size
    val progressCompleted = ((lessonInfo.progressPercent / 100f) * totalItems)
        .roundToInt()
        .coerceIn(0, totalItems)
    val dataCompleted = rawItems.count { it.isCompleted }
    val completedCount = max(progressCompleted, dataCompleted)

    var inProgressAssigned = false

    return rawItems.mapIndexed { index, baseItem ->
        val isCompleted = baseItem.isCompleted || index < completedCount
        val status = when {
            isCompleted -> LessonItemStatus.COMPLETED
            !inProgressAssigned -> {
                inProgressAssigned = true
                LessonItemStatus.IN_PROGRESS
            }
            else -> LessonItemStatus.LOCKED
        }

        val (title, description, iconRes, iconColor) = timelineVisuals(baseItem.type)

        TimelineItem(
            id = baseItem.id,
            type = baseItem.type,
            title = title,
            description = description,
            status = status,
            iconRes = iconRes,
            iconColor = iconColor,
            exerciseId = baseItem.exerciseId
        )
    }
}

private data class BaseTimelineItem(
    val id: String,
    val type: TimelineItemType,
    val exerciseId: String?,
    val isCompleted: Boolean
)

private data class TimelineVisuals(
    val title: String,
    val description: String,
    val iconRes: Int,
    val iconColor: Color
)

private fun timelineVisuals(type: TimelineItemType): TimelineVisuals {
    return when (type) {
        TimelineItemType.QUESTION -> TimelineVisuals(
            title = "Câu hỏi luyện tập",
            description = "Làm quen với dạng câu hỏi, chọn đáp án đúng",
            iconRes = R.drawable.question_awesome,
            iconColor = LessonColors.QuestionColor
        )

        TimelineItemType.LISTENING -> TimelineVisuals(
            title = "Luyện nghe",
            description = "Nghe hội thoại và chọn đáp án đúng",
            iconRes = R.drawable.headphone,
            iconColor = LessonColors.ListeningColor
        )

        TimelineItemType.SPEAKING -> TimelineVisuals(
            title = "Luyện nói",
            description = "Lặp lại câu, luyện phát âm",
            iconRes = R.drawable.sharp_solid_microphone_stand,
            iconColor = LessonColors.SpeakingColor
        )

        TimelineItemType.PRONUNCIATION -> TimelineVisuals(
            title = "Phát âm",
            description = "Ghi âm và so sánh phát âm",
            iconRes = R.drawable.sharp_solid_microphone_stand,
            iconColor = LessonColors.PronunciationColor
        )

        TimelineItemType.WRITING -> TimelineVisuals(
            title = "Luyện viết",
            description = "Hoàn thiện câu tiếng Hàn đúng ngữ pháp",
            iconRes = R.drawable.writing,
            iconColor = LessonColors.WritingColor
        )
    }
}
