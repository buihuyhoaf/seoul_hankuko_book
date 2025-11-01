package com.seoulhankuko.app.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.seoulhankuko.app.data.api.model.CourseResponse
import com.seoulhankuko.app.presentation.viewmodel.HomeViewModel
import com.seoulhankuko.app.presentation.components.ModernBottomNavigationBar
import com.seoulhankuko.app.presentation.utils.HomeColors
import com.seoulhankuko.app.presentation.utils.MiscColors
import kotlinx.coroutines.delay
import kotlin.math.floor


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernHomeScreen(
    onCourseSelected: (courseId: Int) -> Unit,
    onNavigateToNotification: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    var isVisible by remember { mutableStateOf(false) }
    
    // Collect data from HomeViewModel
    val courses by homeViewModel.courses.collectAsStateWithLifecycle()
    val isLoading by homeViewModel.isLoading.collectAsStateWithLifecycle()
    val authState by homeViewModel.authState.collectAsStateWithLifecycle()
    val userName by homeViewModel.currentUserName.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        delay(300) // Delay for smooth entrance
        isVisible = true
    }
    
    Scaffold(
        topBar = {
            AppBarSection(
                userName = userName,
                onAvatarClick = { onNavigateToProfile() }
            )
        },
        bottomBar = {
            ModernBottomNavigationBar(
                currentRoute = "courses",
                onNavigateToHome = {},
                onNavigateToNotification = onNavigateToNotification,
                onNavigateToProfile = onNavigateToProfile
            )
        },
        containerColor = HomeColors.DuolingoLightGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Welcome Section
            WelcomeSection(
                userName = userName,
                isVisible = isVisible
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Courses Grid
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = HomeColors.DuolingoGreen,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else if (courses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không có khóa học nào",
                        style = MaterialTheme.typography.bodyLarge,
                        color = HomeColors.DuolingoGray
                    )
                }
            } else {
                CourseGrid(
                    courses = courses,
                    onCourseSelected = onCourseSelected,
                    isVisible = isVisible
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarSection(
    userName: String,
    onAvatarClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Korean Learning",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = HomeColors.DuolingoDarkGreen
            )
        },
        actions = {
            // Avatar with animation
            var isPressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.95f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "avatar_scale"
            )
            
            Box(
                modifier = Modifier
                    .scale(scale)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isPressed = true
                        onAvatarClick()
                    }
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    color = HomeColors.DuolingoGreen,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.White,
            titleContentColor = HomeColors.DuolingoDarkGreen
        ),
        modifier = Modifier.shadow(elevation = 2.dp)
    )
}

@Composable
fun WelcomeSection(
    userName: String,
    isVisible: Boolean
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(
            animationSpec = tween(800)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "Xin chào, $userName! 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = HomeColors.DuolingoDarkGreen
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Hãy tiếp tục hành trình học tiếng Hàn của bạn",
                style = MaterialTheme.typography.bodyMedium,
                color = HomeColors.DuolingoGray
            )
        }
    }
}

@Composable
fun CourseGrid(
    courses: List<CourseResponse>,
    onCourseSelected: (courseId: Int) -> Unit,
    isVisible: Boolean
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(
            items = courses,
            key = { it.id }
        ) { course ->
            CourseCard(
                course = course,
                onClick = { onCourseSelected(course.id) },
                isVisible = isVisible,
                index = courses.indexOf(course)
            )
        }
    }
}

@Composable
fun CourseCard(
    course: CourseResponse,
    onClick: () -> Unit,
    isVisible: Boolean,
    index: Int
) {
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "card_scale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 8.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "card_elevation"
    )
    
    // Get course icon based on course title
    val courseIcon = when {
        course.title.contains("Fundamentals", ignoreCase = true) -> "alphabet_korean"
        course.title.contains("Intermediate", ignoreCase = true) -> "book"
        course.title.contains("Advanced", ignoreCase = true) -> "graduation_cap"
        course.title.contains("Culture", ignoreCase = true) || course.title.contains("Literature", ignoreCase = true) -> "kr"
        course.title.contains("Business", ignoreCase = true) -> "business"
        course.title.contains("Hangul", ignoreCase = true) -> "alphabet_korean"
        course.title.contains("Giao Tiếp", ignoreCase = true) -> "book"
        course.title.contains("Ngữ Pháp", ignoreCase = true) -> "graduation_cap"
        course.title.contains("Văn Hóa", ignoreCase = true) -> "kr"
        course.title.contains("Nghe", ignoreCase = true) -> "book"
        course.title.contains("Từ Vựng", ignoreCase = true) -> "book"
        else -> "book"
    }
    
    // Get gradient colors based on course ID
    val gradientColors = when (course.id % 6) {
        1 -> MiscColors.Gradient1
        2 -> MiscColors.Gradient2
        3 -> MiscColors.Gradient3
        4 -> MiscColors.Gradient4
        5 -> MiscColors.Gradient5
        else -> MiscColors.Gradient6
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(
            animationSpec = tween(600)
        ),
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .shadow(
                    elevation = elevation,
                    shape = RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Background Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = gradientColors
                            )
                        )
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Section - Icon and Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Course Icon
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.9f),
                            shadowElevation = 2.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = getDrawableResourceId(context, courseIcon)),
                                    contentDescription = null,
                                    tint = HomeColors.DuolingoGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    
                    // Bottom Section - Content
                    Column {
                        // Course Title
                        Text(
                            text = course.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HomeColors.DuolingoDarkGreen,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Course Description
                        Text(
                            text = course.description ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = HomeColors.DuolingoGray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Progress Section
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${(course.progress?.progressPercent ?: 0.0).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = HomeColors.DuolingoGreen
                                )
                                Text(
                                    text = run {
                                        val percentVal = (course.progress?.progressPercent ?: 0)
                                        val unitsInt: Int = course.unitsCount
                                        val completedUnits = (percentVal * unitsInt) / 100
                                        "$completedUnits/$unitsInt bài"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HomeColors.DuolingoGray
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Progress Bar
                            LinearProgressIndicator(
                                progress = {
                                    val fraction = ((course.progress?.progressPercent ?: 0) / 100).toFloat()
                                    fraction.coerceIn(0f, 1f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = HomeColors.DuolingoGreen,
                                trackColor = HomeColors.DuolingoLightGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper function to get drawable resource ID
private fun getDrawableResourceId(context: android.content.Context, drawableName: String): Int {
    return context.resources.getIdentifier(drawableName, "drawable", context.packageName)
}

