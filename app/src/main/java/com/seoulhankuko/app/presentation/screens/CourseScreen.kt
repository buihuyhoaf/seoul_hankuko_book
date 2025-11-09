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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.seoulhankuko.app.data.api.model.UnitResponse
import com.seoulhankuko.app.presentation.components.MainScaffold
import com.seoulhankuko.app.presentation.components.TopBarState
import com.seoulhankuko.app.presentation.components.SouthKoreaLoadingIcon
import com.seoulhankuko.app.presentation.viewmodel.CourseUiState
import com.seoulhankuko.app.presentation.viewmodel.CourseViewModel
import com.seoulhankuko.app.presentation.viewmodel.MainUiViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.presentation.utils.UnitColors
import com.seoulhankuko.app.presentation.utils.CourseColors

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(
    courseId: String,
    onNavigateToUnit: (unitId: String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToNotification: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateBack: () -> Unit,
    onAvatarClick: (() -> Unit)? = null,
    viewModel: CourseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    // Pull-to-refresh state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { 
            viewModel.refreshCourse(courseId)
        }
    )
    
    val mainUiViewModel: MainUiViewModel = hiltViewModel()
    val userData by mainUiViewModel.userData.collectAsStateWithLifecycle()
    val currentLesson by mainUiViewModel.currentLesson.collectAsStateWithLifecycle()

    // Load course data when the screen is first displayed or courseId changes
    LaunchedEffect(courseId) {
        mainUiViewModel.setCurrentCourseId(courseId)
        viewModel.loadCourse(courseId)
    }

    val courseTitleForTopBar = when (val currentState = uiState) {
        is CourseUiState.Success -> currentState.course.title
        else -> null
    }
    val courseThumbnailForTopBar = (uiState as? CourseUiState.Success)?.course?.imageUrl

    MainScaffold(
        topBarState = TopBarState(
            userName = userData.name ?: userData.email ?: "Học viên",
            streakDays = userData.streakDays,
            exp = userData.exp,
            courseTitle = courseTitleForTopBar,
            courseThumbnailUrl = courseThumbnailForTopBar,
            isVisible = true,
            isShown = true
        ),
        currentRoute = "courses",
        onNavigateToHome = onNavigateToHome,
        onNavigateToNotification = onNavigateToNotification,
        onNavigateToProfile = onNavigateToProfile,
        onAvatarClick = onAvatarClick ?: onNavigateToProfile,
        containerColor = CourseColors.Background,
        showBackButton = true,
        onBackClick = onNavigateBack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(CourseColors.Background)
                .pullRefresh(pullRefreshState)
        ) {
            when (val currentState = uiState) {
                is CourseUiState.Loading -> {
                    LoadingContent()
                }
                is CourseUiState.Success -> {
                    SuccessContent(
                        units = currentState.units,
                        currentUnitId = currentLesson?.unitId,
                        onNavigateToUnit = onNavigateToUnit
                    )
                }
                is CourseUiState.Error -> {
                    ErrorContent(
                        errorMessage = currentState.message,
                        onRetry = { viewModel.loadCourse(courseId) }
                    )
                }
            }
            
            // Pull-to-refresh indicator
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SouthKoreaLoadingIcon(size = 48.dp)
            Text(
                text = "Đang tải khóa học...",
                style = MaterialTheme.typography.bodyLarge,
                color = CourseColors.TextSecondary
            )
        }
    }
}

@Composable
private fun SuccessContent(
    units: List<UnitResponse>,
    currentUnitId: String?,
    onNavigateToUnit: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Welcome text
        Text(
            text = "Chọn một chủ đề để bắt đầu",
            style = MaterialTheme.typography.titleMedium,
            color = CourseColors.TextSecondary,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 16.dp)
        )
        
        if (units.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có chủ đề nào trong khóa học",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CourseColors.TextSecondary
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(units) { index, unit ->
                    UnitCard(
                        unit = unit,
                        index = index,
                        isCurrentUnit = currentUnitId == unit.id,
                        onClick = { onNavigateToUnit(unit.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    errorMessage: String,
    onRetry: () -> Unit
) {
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
                text = "Không thể tải khóa học. Vui lòng thử lại.",
                style = MaterialTheme.typography.titleMedium,
                color = CourseColors.TextPrimary
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = CourseColors.TextSecondary
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CourseColors.Accent
                )
            ) {
                Text("Thử lại")
            }
        }
    }
}

/**
 * Extract Vietnamese meaning from title if available
 * Format: "감기에 걸렸어요 (Tôi bị cảm lạnh)" -> "Tôi bị cảm lạnh"
 */
private fun extractVietnameseMeaning(title: String): String? {
    val pattern = Regex("\\(([^)]+)\\)")
    return pattern.find(title)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
}

@Composable
private fun UnitCard(
    unit: UnitResponse,
    index: Int,
    isCurrentUnit: Boolean,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    
    // Get Vietnamese meaning
    val vietnameseMeaning = extractVietnameseMeaning(unit.title)
    val unitNumber = index + 1
    
    // Scale animation - 0.97f when pressed
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

    // Gradient background
    val backgroundBrush = Brush.verticalGradient(
        listOf(Color.White, CourseColors.AccentLight.copy(alpha = 0.3f))
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .alpha(alpha)
            .scale(scale)
            .shadow(
                elevation = 6.dp,
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
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Badge "Chủ đề n" - top left corner
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CourseColors.AccentLight,
                modifier = Modifier
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "Chủ đề $unitNumber",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = CourseColors.Accent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section: Content
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Spacer for badge
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    // Korean title - bold
                    Text(
                        text = unit.title.split("(").first().trim(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CourseColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Vietnamese meaning (if available) - italic, gray
                    vietnameseMeaning?.let { meaning ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "($meaning)",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = CourseColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Description - max 2 lines
                    unit.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = CourseColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                        )
                    }
                }

                // Bottom section: Progress indicator
                Spacer(modifier = Modifier.height(8.dp))
                
                val actionText = if (isCurrentUnit) "Tiếp tục" else "Bắt đầu học"
                val actionBackground = if (isCurrentUnit) CourseColors.Accent else CourseColors.AccentLight
                val actionContentColor = if (isCurrentUnit) Color.White else CourseColors.Accent

                unit.progress?.let { progress ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (progress.isCompleted) {
                            // Completed state
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✅ Hoàn thành",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CourseColors.Completed,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = actionBackground,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = actionText,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = actionContentColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .fillMaxWidth()
                                )
                            }
                        } else {
                            // In progress state
                            Column {
                                LinearProgressIndicator(
                                    progress = { (progress.progressPercent / 100.0).toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = CourseColors.Accent,
                                    trackColor = CourseColors.Accent.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "📈 Tiến độ: ${progress.progressPercent.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CourseColors.Accent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = actionBackground,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = actionText,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = actionContentColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }
                } ?: run {
                    // No progress data
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = actionBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = actionText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = actionContentColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
