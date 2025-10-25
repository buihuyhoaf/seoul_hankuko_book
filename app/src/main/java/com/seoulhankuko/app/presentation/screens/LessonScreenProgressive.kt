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
import com.seoulhankuko.app.domain.model.LessonTask
import com.seoulhankuko.app.domain.model.TaskType
import com.seoulhankuko.app.domain.model.getColor
import com.seoulhankuko.app.domain.model.getIcon
import com.seoulhankuko.app.presentation.viewmodel.LessonViewModel
import kotlinx.coroutines.delay

// Progressive Learning Color Palette
private val PrimaryCoral = Color(0xFFFF6F61)
private val SecondaryTeal = Color(0xFF4DB6AC)
private val BackgroundCream = Color(0xFFFFFDF6)
private val CompletedGreen = Color(0xFFE8F5E9)
private val LockedGray = Color(0xFFF5F5F5)
private val TextPrimaryProg = Color(0xFF333333)
private val TextSecondaryProg = Color(0xFF757575)
private val PastelYellow = Color(0xFFFFF8E7)

/**
 * LessonScreen with progressive unlock logic
 * Tasks unlock sequentially as previous tasks are completed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreenProgressive(
    lessonId: Int,
    onNavigateBack: () -> Unit,
    onTaskClick: (TaskType) -> Unit,
) {
    // Mock data with progressive unlock logic
    val tasks = remember {
        listOf(
            LessonTask(
                id = 1,
                type = TaskType.QUIZ,
                title = "Basic Greetings",
                description = "Test your knowledge of Korean greetings",
                completed = true,
                unlocked = true,
                orderIndex = 0
            ),
            LessonTask(
                id = 2,
                type = TaskType.LISTENING,
                title = "Greeting Dialogues",
                description = "Listen and understand Korean conversations",
                completed = false,
                unlocked = true, // Unlocked because Quiz is completed
                orderIndex = 1
            ),
            LessonTask(
                id = 3,
                type = TaskType.SPEAKING,
                title = "Practice Greetings",
                description = "Practice pronouncing Korean greetings",
                completed = false,
                unlocked = false, // Locked because Listening is not completed
                orderIndex = 2
            ),
            LessonTask(
                id = 4,
                type = TaskType.WRITING,
                title = "Greeting Sentences",
                description = "Write Korean greeting sentences",
                completed = false,
                unlocked = false, // Locked because Speaking is not completed
                orderIndex = 3
            )
        )
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
    
    Scaffold(
        topBar = {
            LessonTopAppBar(
                lessonTitle = "Lesson $lessonId: 안녕하세요",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = BackgroundCream,
        snackbarHost = {
            SnackbarHost(
                hostState = remember { SnackbarHostState() },
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = PrimaryCoral,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
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
                LessonProgress(
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
                            onTaskClick(task.type)
                        } else {
                            // Show snackbar for locked tasks
                            snackbarMessage = "Complete previous task first!"
                            showSnackbar = true
                        }
                    }
                )
            }
        }
    }
    
    // Handle snackbar
    if (showSnackbar) {
        LaunchedEffect(showSnackbar) {
            delay(2000)
            showSnackbar = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonTopAppBar(
    lessonTitle: String,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = lessonTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
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
                    colors = listOf(PrimaryCoral, PastelYellow)
                )
            )
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(0.dp)
            )
    )
}

@Composable
private fun LessonProgress(
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
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // Progress text
        Text(
            text = "You've completed $completedTasks of $totalTasks tasks",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimaryProg,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress bar
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = PrimaryCoral,
            trackColor = LockedGray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Progress percentage
        Text(
            text = "${(animatedProgress * 100).toInt()}% Complete",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryProg,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LessonTaskList(
    tasks: List<LessonTask>,
    onTaskClick: (LessonTask) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        itemsIndexed(tasks) { index, task ->
            LessonTaskCard(
                task = task,
                index = index,
                onClick = { onTaskClick(task) }
            )
        }
    }
}

@Composable
private fun LessonTaskCard(
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
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = index * 100,
            easing = FastOutSlowInEasing
        ),
        label = "card_alpha"
    )
    
    // Determine card colors based on status
    val backgroundColor = when {
        task.completed -> CompletedGreen
        task.unlocked -> Color.White
        else -> LockedGray
    }
    
    val borderColor = when {
        task.completed -> Color(0xFF4CAF50)
        task.unlocked -> PrimaryCoral
        else -> Color(0xFFE0E0E0)
    }
    
    val textColor = when {
        task.completed -> TextPrimaryProg
        task.unlocked -> TextPrimaryProg
        else -> TextSecondaryProg
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
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        task.unlocked -> {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Unlocked",
                                tint = PrimaryCoral,
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