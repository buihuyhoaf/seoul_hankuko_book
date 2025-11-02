package com.seoulhankuko.app.presentation.screens

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.res.painterResource
import com.seoulhankuko.app.R
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.domain.model.QuestionType
import com.seoulhankuko.app.domain.model.LessonWithChallenges
import com.seoulhankuko.app.presentation.utils.LessonColors
import com.seoulhankuko.app.presentation.viewmodel.LessonUiState
import com.seoulhankuko.app.presentation.viewmodel.LessonViewModel
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Modern LessonScreen with timeline
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    lessonId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToLessonFlow: () -> Unit,
    onNavigateToListening: (exerciseId: Int) -> Unit = {},
    onNavigateToSpeaking: (exerciseId: Int) -> Unit = {},
    onNavigateToWriting: (exerciseId: Int) -> Unit = {},
    viewModel: LessonViewModel = hiltViewModel()
) {
    var isLoading by remember { mutableStateOf(true) }
    var visible by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    // Load lesson data
    LaunchedEffect(lessonId) {
        isLoading = true
        viewModel.loadLesson(lessonId)
    }
    
    // Observe UI state
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Update local state
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
    
    val lessonInfo = (uiState as? LessonUiState.Success)?.lessonWithChallenges
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = lessonInfo?.lesson?.title ?: "Lesson $lessonId",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                            colors = listOf(LessonColors.TopBarGradientStart, LessonColors.TopBarGradientEnd)
                        )
                    )
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(0.dp))
            )
        },
        containerColor = LessonColors.BackgroundWhite
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LessonColors.QuestionColor)
                }
            }
            else -> {
                lessonInfo?.let { lesson ->
                    LessonTimelineContent(
                        lessonInfo = lesson,
                        listState = listState,
                        progressPercent = lesson.progressPercent,
                        onNavigateToLessonFlow = onNavigateToLessonFlow,
                        onNavigateToListening = onNavigateToListening,
                        onNavigateToSpeaking = onNavigateToSpeaking,
                        onNavigateToWriting = onNavigateToWriting,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                } ?: run {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Lesson not found", color = LessonColors.TextPrimary)
                    }
                }
            }
        }
    }
}

/**
 * Main timeline content with header
 */
@Composable
private fun LessonTimelineContent(
    lessonInfo: LessonWithChallenges,
    listState: LazyListState,
    progressPercent: Int,
    onNavigateToLessonFlow: () -> Unit,
    onNavigateToListening: (exerciseId: Int) -> Unit,
    onNavigateToSpeaking: (exerciseId: Int) -> Unit,
    onNavigateToWriting: (exerciseId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Prepare timeline items
    val timelineItems = remember(lessonInfo) {
        buildTimelineItems(lessonInfo)
    }
    
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Lesson Header
        item {
            LessonHeader(
                lessonTitle = lessonInfo.lesson.title,
                lessonDescription = "Let's start learning Korean!",
                progressPercent = progressPercent
            )
        }
        
        // Timeline items
        itemsIndexed(
            items = timelineItems,
            key = { index, item -> item.id }
        ) { index, item ->
            TimelineNode(
                item = item,
                isFirst = index == 0,
                isLast = index == timelineItems.size - 1,
                itemIndex = index,
                onItemClick = {
                    when (item.type) {
                        TimelineItemType.QUESTION -> onNavigateToLessonFlow()
                        TimelineItemType.LISTENING -> {
                            item.exerciseId?.let { onNavigateToListening(it) }
                        }
                        TimelineItemType.SPEAKING -> {
                            item.exerciseId?.let { onNavigateToSpeaking(it) }
                        }
                        TimelineItemType.PRONUNCIATION -> {
                            item.exerciseId?.let { onNavigateToSpeaking(it) }
                        }
                        TimelineItemType.WRITING -> {
                            item.exerciseId?.let { onNavigateToWriting(it) }
                        }
                    }
                }
            )
        }
    }
}

/**
 * Fixed lesson header (no collapse functionality)
 */
@Composable
private fun LessonHeader(
    lessonTitle: String,
    lessonDescription: String,
    progressPercent: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        LessonColors.HeaderGradientStart,
                        LessonColors.HeaderGradientEnd
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                Text(
                    text = lessonTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                color = LessonColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                
                // Description
                Text(
                    text = lessonDescription,
                    style = MaterialTheme.typography.bodyMedium,
                color = LessonColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                
            // Progress Bar
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                    color = LessonColors.QuestionColor,
                    trackColor = LessonColors.ConnectorLineColor
                        )
                        
                        Text(
                            text = "Progress: $progressPercent%",
                            style = MaterialTheme.typography.bodySmall,
                    color = LessonColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
/**
 * Timeline item data structure
 */
private enum class TimelineItemType {
    QUESTION, LISTENING, SPEAKING, PRONUNCIATION, WRITING
}

private data class TimelineItem(
    val id: String,
    val type: TimelineItemType,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val exerciseId: Int? = null // Store exerciseId for navigation
)
        
/**
 * Build timeline items from lesson data
 */
private fun buildTimelineItems(
    lessonInfo: LessonWithChallenges
): List<TimelineItem> {
    val items = mutableListOf<TimelineItem>()
    
    // Add Questions node (if questions exist)
    val questionTypes = setOf(
        QuestionType.MULTIPLE_CHOICE,
        QuestionType.BLANK,
        QuestionType.MATCHING,
        QuestionType.AUDIO_COMPREHENSION,
        QuestionType.SENTENCE_ORDER,
        QuestionType.IMAGE_SELECTION
    )
    val hasQuestions = lessonInfo.challenges.any { it.challenge.type in questionTypes }
    val allQuestionsCompleted = hasQuestions && lessonInfo.challenges
        .filter { it.challenge.type in questionTypes }
        .all { it.completed }
    
    if (hasQuestions) {
        items.add(
            TimelineItem(
                id = "questions",
                type = TimelineItemType.QUESTION,
                title = "Questions",
                description = "Practice questions",
                isCompleted = allQuestionsCompleted
            )
        )
    }
    
    // Add Exercise nodes
    lessonInfo.exercises.forEach { exercise ->
        val type = when (exercise.type.lowercase()) {
            "listening", "audio_comprehension" -> TimelineItemType.LISTENING
            "speaking" -> TimelineItemType.SPEAKING
            "pronunciation" -> TimelineItemType.PRONUNCIATION
            "writing", "writing_practice" -> TimelineItemType.WRITING
            else -> null
        }
        
        if (type != null) {
            items.add(
                TimelineItem(
                    id = "exercise_${exercise.id}",
                    type = type,
                    title = exercise.title ?: exercise.type.replaceFirstChar { it.uppercaseChar() },
                    description = exercise.content ?: "",
                    isCompleted = false, // TODO: Get from progress tracking
                    exerciseId = exercise.id
                )
            )
        }
    }
    
    return items
}

/**
 * Timeline node component with centered vertical line
 */
@Composable
private fun TimelineNode(
    item: TimelineItem,
    isFirst: Boolean,
    isLast: Boolean,
    itemIndex: Int,
    onItemClick: () -> Unit
) {
    val nodeColor = when (item.type) {
        TimelineItemType.QUESTION -> LessonColors.QuestionColor
        TimelineItemType.LISTENING -> LessonColors.ListeningColor
        TimelineItemType.SPEAKING -> LessonColors.SpeakingColor
        TimelineItemType.PRONUNCIATION -> LessonColors.PronunciationColor
        TimelineItemType.WRITING -> LessonColors.SpeakingColor
    }
    
    val iconDrawable = when (item.type) {
        TimelineItemType.QUESTION -> R.drawable.question_awesome
        TimelineItemType.LISTENING -> R.drawable.assistive_listening_systems
        TimelineItemType.SPEAKING -> R.drawable.sharp_solid_microphone_stand
        TimelineItemType.PRONUNCIATION -> R.drawable.sharp_solid_microphone_stand
        TimelineItemType.WRITING -> R.drawable.writing
    }
    
    // Animated entrance - only delay first few items to avoid long delays for later items
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Only apply stagger delay for first 3 items, rest appear immediately when scrolled into view
        if (itemIndex < 3) {
            delay(itemIndex * 100L)
        }
        isVisible = true
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        // Left side - for even indices (0, 2, 4...)
        if (itemIndex % 2 == 0) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = if (itemIndex == 0) Alignment.Center else Alignment.CenterEnd
            ) {
                TimelineCircularNode(
                    item = item,
                    nodeColor = nodeColor,
                    iconDrawable = iconDrawable,
                    onClick = onItemClick,
                    modifier = Modifier
                        .padding(end = if (itemIndex == 0) 0.dp else 24.dp)
                        .alpha(if (isVisible) 1f else 0f)
            )
            }
            
            // Center line and node indicator
            Box(
                modifier = Modifier.width(48.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Vertical connector line
                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(120.dp)
                            .align(Alignment.TopCenter)
                            .background(LessonColors.ConnectorLineColor)
                    )
                }
                
                // Circular indicator on line (at top, center horizontally)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(nodeColor)
                        .align(Alignment.TopCenter)
                )
            }
            
            // Right side - empty for even indices
            Spacer(modifier = Modifier.weight(1f))
        } else {
            // Right side - for odd indices (1, 3, 5...)
            // Left side - empty for odd indices
            Spacer(modifier = Modifier.weight(1f))
            
            // Center line and node indicator
            Box(
                modifier = Modifier.width(48.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Vertical connector line
                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(120.dp)
                            .align(Alignment.TopCenter)
                            .background(LessonColors.ConnectorLineColor)
                    )
                }
                
                // Circular indicator on line (at top, center horizontally)
                Box(modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(nodeColor)
                        .align(Alignment.TopCenter)
                )
            }
            
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                TimelineCircularNode(
                    item = item,
                    nodeColor = nodeColor,
                    iconDrawable = iconDrawable,
                    onClick = onItemClick,
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .alpha(if (isVisible) 1f else 0f)
                )
            }
        }
    }
}

/**
 * Simple circular timeline node - just circle with icon, content below
 */
@Composable
private fun TimelineCircularNode(
    item: TimelineItem,
    nodeColor: Color,
    iconDrawable: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Simple circular icon button - no shadow, just colored circle
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(nodeColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconDrawable),
                contentDescription = item.title,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
            
            // Completed overlay
            if (item.isCompleted) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = LessonColors.CompletedGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        
        // Content below circle
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = LessonColors.TextPrimary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
