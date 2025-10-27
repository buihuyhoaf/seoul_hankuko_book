package com.seoulhankuko.app.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.data.api.model.ExerciseResponse
import com.seoulhankuko.app.data.api.model.LessonDetailResponse
import com.seoulhankuko.app.domain.model.LessonTask
import com.seoulhankuko.app.domain.model.TaskType
import com.seoulhankuko.app.domain.model.getColor
import com.seoulhankuko.app.domain.model.getIcon
import com.seoulhankuko.app.data.repository.CourseRepository
import com.seoulhankuko.app.data.repository.AuthRepository
import timber.log.Timber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

// Lesson Screen Color Palette
private val PrimaryColor = Color(0xFFFF6F61)
private val SecondaryColor = Color(0xFFFFE0B2)
private val BackgroundColor = Color(0xFFFFF8E7)
private val SuccessColor = Color(0xFF4CAF50)
private val LockedColor = Color(0xFFE0E0E0)
private val LessonTextPrimary = Color(0xFF333333)
private val LessonTextSecondary = Color(0xFF757575)

/**
 * Main LessonScreen composable
 * Shows lesson tasks with progressive unlock mechanism
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    lessonId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToTask: (TaskType) -> Unit,
    courseRepository: CourseRepository,
    authRepository: AuthRepository
) {
    var lessonData by remember { mutableStateOf<LessonDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load lesson data from API
    LaunchedEffect(lessonId) {
        isLoading = true
        try {
            val token = authRepository.getCurrentToken()
            val result = courseRepository.getLesson(lessonId, token)
            result.fold(
                onSuccess = { lesson ->
                    lessonData = lesson
                    isLoading = false
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load lesson $lessonId")
                    isLoading = false
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Exception loading lesson $lessonId")
            isLoading = false
        }
    }
    
    // Convert to tasks based on lesson data
    val tasks = remember(lessonData) {
        lessonData?.let { lesson ->
            buildTasksFromLesson(lesson)
        } ?: emptyList()
    }
    
    val completedTasks = tasks.count { it.completed }
    val totalTasks = tasks.size
    
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(300)
        visible = true
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        topBar = {
            LessonTopAppBar(
                lessonTitle = lessonData?.let { "Lesson ${it.id}: ${it.title}" } 
                    ?: "Lesson $lessonId",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = BackgroundColor,
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Progress Section
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { -30 })
                    ) {
                        LessonProgressCard(
                            completedTasks = completedTasks,
                            totalTasks = totalTasks
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Task List
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { 30 })
                    ) {
                        LessonTaskList(
                            tasks = tasks,
                            onTaskClick = { task ->
                                if (task.unlocked) {
                                    onNavigateToTask(task.type)
                                } else {
                                    snackbarMessage = "Complete previous task first!"
                                    showSnackbar = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Handle snackbar
    if (showSnackbar) {
        LaunchedEffect(showSnackbar) {
            snackbarHostState.showSnackbar(
                message = snackbarMessage,
                duration = SnackbarDuration.Short
            )
            showSnackbar = false
        }
    }
}

/**
 * Build tasks from lesson data
 * Tasks order: Listening, Speaking, Writing, Final Quiz
 */
private fun buildTasksFromLesson(lesson: LessonDetailResponse): List<LessonTask> {
    val tasks = mutableListOf<LessonTask>()
    var orderIndex = 0
    
    // 1. Listening (from exercises)
    val listeningExercises = lesson.exercises.filter { it.type == "listening" }
    if (listeningExercises.isNotEmpty()) {
        tasks.add(
            LessonTask(
                id = -orderIndex,
                type = TaskType.LISTENING,
                title = "Listening",
                description = "Listen to Korean conversations",
                completed = false, // TODO: Get from progress
                unlocked = true,
                orderIndex = orderIndex++
            )
        )
    }
    
    // 2. Speaking (from exercises)
    val speakingExercises = lesson.exercises.filter { it.type == "speaking" }
    if (speakingExercises.isNotEmpty()) {
        tasks.add(
            LessonTask(
                id = -orderIndex,
                type = TaskType.SPEAKING,
                title = "Speaking",
                description = "Practice pronunciation",
                completed = false,
                unlocked = tasks.isEmpty() || tasks.last().completed,
                orderIndex = orderIndex++
            )
        )
    }
    
    // 3. Writing (from exercises)
    val writingExercises = lesson.exercises.filter { it.type == "writing" }
    if (writingExercises.isNotEmpty()) {
        tasks.add(
            LessonTask(
                id = -orderIndex,
                type = TaskType.WRITING,
                title = "Writing",
                description = "Write Korean sentences",
                completed = false,
                unlocked = tasks.isEmpty() || tasks.all { it.completed } || tasks.size == 1,
                orderIndex = orderIndex++
            )
        )
    }
    
    // 4. Final Quiz (from questions)
    if (lesson.questions.isNotEmpty()) {
        tasks.add(
            LessonTask(
                id = -orderIndex,
                type = TaskType.FINAL_QUIZ,
                title = "Final Quiz",
                description = "Test your knowledge (${lesson.questions.size} questions)",
                completed = false,
                unlocked = tasks.isEmpty() || tasks.all { it.completed },
                orderIndex = orderIndex
            )
        )
    }
    
    return tasks
}

/**
 * Top AppBar with gradient background
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonTopAppBar(
    lessonTitle: String,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🇰🇷", fontSize = 20.sp)
                Text(
                    text = lessonTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
                    colors = listOf(Color(0xFFFF8A65), Color(0xFFFFD180))
                )
            )
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(0.dp)
            )
    )
}

/**
 * Progress card showing completion status
 */
@Composable
private fun LessonProgressCard(
    completedTasks: Int,
    totalTasks: Int
) {
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 1500,
            easing = EaseOutCubic
        ),
        label = "progress"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF8E7)
        ),
        border = BorderStroke(1.dp, Color(0xFFFFE0B2))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress text
            Text(
                text = "$completedTasks of $totalTasks tasks completed",
                style = MaterialTheme.typography.titleMedium,
                color = LessonTextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PrimaryColor,
                trackColor = LockedColor
            )
            
            // Progress percentage
            Text(
                text = "${(animatedProgress * 100).toInt()}% Complete",
                style = MaterialTheme.typography.bodyMedium,
                color = LessonTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Task list composable
 */
@Composable
private fun LessonTaskList(
    tasks: List<LessonTask>,
    onTaskClick: (LessonTask) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        itemsIndexed(tasks) { index, task ->
            LessonTaskItem(
                task = task,
                index = index,
                onClick = { onTaskClick(task) }
            )
        }
    }
}

/**
 * Individual task item with animations
 */
@Composable
private fun LessonTaskItem(
    task: LessonTask,
    index: Int,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (task.unlocked) 1f else 0.6f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = index * 100,
            easing = FastOutSlowInEasing
        ),
        label = "card_alpha"
    )
    
    // Determine card colors based on status
    val backgroundColor = when {
        task.completed -> Color(0xFFE8F5E9)
        task.unlocked -> Color.White
        else -> LockedColor
    }
    
    val borderColor = when {
        task.completed -> SuccessColor
        task.unlocked -> PrimaryColor
        else -> Color(0xFFE0E0E0)
    }
    
    val textColor = when {
        task.completed -> LessonTextPrimary
        task.unlocked -> LessonTextPrimary
        else -> LessonTextSecondary
    }
    
    val taskColor = Color(task.type.getColor())
    
    Card(
        onClick = {
            if (task.unlocked) {
                isPressed = true
                onClick()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(alpha)
            .shadow(
                elevation = if (task.unlocked) 8.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (task.unlocked) taskColor.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = BorderStroke(
            width = if (task.completed) 3.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Task Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (task.unlocked) taskColor.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = task.type.getIcon(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 28.sp
                )
            }
            
            // Task Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f),
                    maxLines = 2
                )
            }
            
            // Status Icon
            AnimatedVisibility(
                visible = true,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        task.completed -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = SuccessColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        task.unlocked -> {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Unlocked",
                                tint = PrimaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

