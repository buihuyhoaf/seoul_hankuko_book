package com.seoulhankuko.app.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.seoulhankuko.app.presentation.viewmodel.UnitViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.presentation.components.MainScaffold
import com.seoulhankuko.app.presentation.components.TopBarState
import com.seoulhankuko.app.presentation.viewmodel.MainUiViewModel
import com.seoulhankuko.app.presentation.utils.UnitColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitScreen(
    unitId: String,
    onNavigateToLesson: (lessonId: String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToNotification: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateBack: () -> Unit,
    onAvatarClick: (() -> Unit)? = null,
    viewModel: UnitViewModel = hiltViewModel()
) {
    // Collect unit data from ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(unitId) {
        viewModel.loadUnit(unitId)
    }
 
    val mainUiViewModel: MainUiViewModel = hiltViewModel()
    val userData by mainUiViewModel.userData.collectAsStateWithLifecycle()
    val currentLesson by mainUiViewModel.currentLesson.collectAsStateWithLifecycle()

    MainScaffold(
        topBarState = TopBarState(
            userName = userData.name ?: userData.email ?: "Học viên",
            streakDays = userData.streakDays,
            exp = userData.exp,
            courseTitle = uiState.unitTitle,
            courseThumbnailUrl = null,
            isVisible = true,
            isShown = true
        ),
        currentRoute = "courses",
        onNavigateToHome = onNavigateToHome,
        onNavigateToNotification = onNavigateToNotification,
        onNavigateToProfile = onNavigateToProfile,
        onAvatarClick = onAvatarClick ?: onNavigateToProfile,
        containerColor = UnitColors.BackgroundLight,
        showBackButton = true,
        onBackClick = onNavigateBack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(UnitColors.BackgroundLight)
        ) {
            when {
                uiState.isLoading -> {
                    // Loading state - Vietnamese
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = UnitColors.SoftIndigo
                            )
                            Text(
                                text = "Đang tải bài học...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = UnitColors.TextSecondary
                            )
                        }
                    }
                }
                
                uiState.error != null -> {
                    // Error state - Vietnamese
                    val errorMessage = uiState.error
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Không thể tải danh sách bài học",
                                style = MaterialTheme.typography.titleMedium,
                                color = UnitColors.TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = errorMessage ?: "Lỗi không xác định",
                                style = MaterialTheme.typography.bodyMedium,
                                color = UnitColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.loadUnit(unitId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = UnitColors.SoftIndigo
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Thử lại",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                uiState.lessons.isEmpty() -> {
                    // Empty state - Vietnamese
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có bài học nào",
                            style = MaterialTheme.typography.bodyLarge,
                            color = UnitColors.TextSecondary
                        )
                    }
                }
                
                else -> {
                    // Success state - Vietnamese
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Section header - Vietnamese
                        Text(
                            text = "Chọn bài học để bắt đầu nhé 🎯",
                            style = MaterialTheme.typography.titleMedium,
                            color = UnitColors.TextSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 16.dp)
                        )

                        // Lesson Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(uiState.lessons) { index, lesson ->
                                LessonCard(
                                    lesson = lesson,
                                    index = index,
                                    currentLessonId = currentLesson?.lessonId,
                                    onClick = { onNavigateToLesson(lesson.id) }
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
fun LessonCard(
    lesson: com.seoulhankuko.app.data.api.model.LessonResponse,
    index: Int,
    currentLessonId: String?,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    
    // Scale animation - press feedback
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale_animation"
    )
    
    // Fade-in animation with staggered delay
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 80L)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "alpha_animation"
    )
    
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isPressed = true
                }
                is PressInteraction.Release -> {
                    isPressed = false
                }
                is PressInteraction.Cancel -> {
                    isPressed = false
                }
                else -> {}
            }
        }
    }

    // Determine CTA button text based on progress
    val isCurrentLesson = currentLessonId == lesson.id
    val progressPercent = lesson.progress?.progressPercent?.toDouble() ?: 0.0
    val ctaText = when {
        lesson.progress?.isCompleted == true -> "Xem lại"
        progressPercent <= 0.0 -> "Bắt đầu học"
        isCurrentLesson -> "Tiếp tục"
        else -> "Tiếp tục"
    }
    val ctaContainerColor = when {
        lesson.progress?.isCompleted == true -> UnitColors.SoftIndigo
        progressPercent <= 0.0 -> UnitColors.LightGray
        isCurrentLesson -> UnitColors.WarmOrange
        else -> UnitColors.WarmOrange
    }
    val ctaContentColor = when {
        lesson.progress?.isCompleted == true -> Color.White
        progressPercent <= 0.0 -> UnitColors.TextPrimary
        else -> Color.White
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .alpha(alpha)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 2.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = interactionSource
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Order number badge - top left, circular, lavender background
            Surface(
                shape = CircleShape,
                color = UnitColors.Lavender,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.TopStart)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${lesson.orderIndex}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = UnitColors.SoftIndigo,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp)) // Space for badge
                    
                    // Lesson title - bold, max 2 lines
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = UnitColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = MaterialTheme.typography.titleSmall.lineHeight
                    )

                    // Lesson description - 1 line max
                    Text(
                        text = lesson.description?.takeIf { it.isNotBlank() } 
                            ?: "Bài học về từ vựng và ngữ pháp",
                        style = MaterialTheme.typography.bodySmall,
                        color = UnitColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // CTA button - anchored at bottom, WarmOrange
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ctaContainerColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = ctaText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ctaContentColor
                    )
                }
            }
        }
    }
}
