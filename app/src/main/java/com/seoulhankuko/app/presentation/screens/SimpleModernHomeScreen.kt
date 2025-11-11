package com.seoulhankuko.app.presentation.screens

import android.text.Html
import android.text.TextUtils
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.seoulhankuko.app.R
import com.seoulhankuko.app.data.api.model.CourseResponse
import com.seoulhankuko.app.presentation.components.MainScaffold
import com.seoulhankuko.app.presentation.components.TopBarState
import com.seoulhankuko.app.presentation.utils.HomeColors
import com.seoulhankuko.app.presentation.components.SouthKoreaLoadingIcon
import com.seoulhankuko.app.presentation.viewmodel.HomeViewModel
import com.seoulhankuko.app.presentation.viewmodel.MainUiViewModel
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernHomeScreen(
    onCourseSelected: (courseId: String) -> Unit,
    onNavigateToNotification: () -> Unit = {},
    onNavigateToAlphabet: () -> Unit = {},
    onNavigateToRanking: () -> Unit = {},
    onNavigateToMission: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    var isVisible by remember { mutableStateOf(false) }
    
    // Collect data from HomeViewModel
    val courses by homeViewModel.courses.collectAsStateWithLifecycle()
    val isLoading by homeViewModel.isLoading.collectAsStateWithLifecycle()
    val userName by homeViewModel.currentUserName.collectAsStateWithLifecycle()
    val popupCourseId by homeViewModel.popupCourseId.collectAsStateWithLifecycle()
    val userData by homeViewModel.userData.collectAsStateWithLifecycle()
    val mainUiViewModel: MainUiViewModel = hiltViewModel()
    val currentLesson by mainUiViewModel.currentLesson.collectAsStateWithLifecycle()

    val streakDays = userData.streakDays
    val expPoints = userData.exp
    val featuredCourse = courses.firstOrNull()
    val courseThumbnailUrl = featuredCourse?.imageUrl
    val courseTitle = featuredCourse?.title

    LaunchedEffect(Unit) {
        delay(300) // Delay for smooth entrance
        isVisible = true
    }
    
    MainScaffold(
        topBarState = TopBarState(
            userName = userName,
            streakDays = streakDays,
            exp = expPoints,
            courseTitle = courseTitle,
            courseThumbnailUrl = courseThumbnailUrl,
            avatarUrl = userData.avatarUrl,
            isVisible = isVisible,
            isShown = true
        ),
        currentRoute = "courses",
        onNavigateToHome = {},
        onNavigateToAlphabet = onNavigateToAlphabet,
        onNavigateToRanking = onNavigateToRanking,
        onNavigateToMission = onNavigateToMission,
        onNavigateToNotification = onNavigateToNotification,
        onNavigateToProfile = onNavigateToProfile,
        onAvatarClick = onNavigateToProfile,
        containerColor = HomeColors.DuolingoLightGray
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                        SouthKoreaLoadingIcon(size = 48.dp)
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
                        isVisible = isVisible,
                        currentCourseId = currentLesson?.courseId,
                        popupCourseId = popupCourseId,
                        onShowPopup = { homeViewModel.showCoursePopup(it) },
                        onHidePopup = { homeViewModel.hideCoursePopup() }
                    )
                }
            }
            
            // Full-screen overlay to catch outside taps when popup is visible
            if (popupCourseId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { homeViewModel.hideCoursePopup() }
                )
            }
        }
    }
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
    onCourseSelected: (courseId: String) -> Unit,
    isVisible: Boolean,
    currentCourseId: String?,
    popupCourseId: String?,
    onShowPopup: (String) -> Unit,
    onHidePopup: () -> Unit
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
                index = courses.indexOf(course),
                isCurrentCourse = currentCourseId == course.id,
                isPopupVisible = popupCourseId == course.id,
                onShowPopup = { onShowPopup(course.id) },
                onHidePopup = onHidePopup
            )
        }
    }
}

@Composable
fun CourseCard(
    course: CourseResponse,
    onClick: () -> Unit,
    isVisible: Boolean,
    index: Int,
    isCurrentCourse: Boolean,
    isPopupVisible: Boolean,
    onShowPopup: () -> Unit,
    onHidePopup: () -> Unit
) {
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
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

    
    // Get gradient colors based on course ID
    
    val gradient = remember {
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0x99000000))
        )
    }

    val descriptionPlain = remember(course.description) {
        HtmlCompat.fromHtml(course.description ?: "", HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    }

    val ctaText = if (isCurrentCourse) "Tiếp tục" else "Bắt đầu học"
    val ctaBackgroundColor = if (isCurrentCourse) HomeColors.DuolingoGreen else HomeColors.DuolingoLightGreen
    val ctaContentColor = if (isCurrentCourse) Color.White else HomeColors.DuolingoDarkGreen

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
    ) {
        Box {
            var cardRect by remember { mutableStateOf<Rect?>(null) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .shadow(
                        elevation = elevation,
                        shape = RoundedCornerShape(16.dp),
                        clip = false
                    )
                    .onGloballyPositioned { coordinates ->
                        cardRect = coordinates.boundsInWindow()
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isPressed = true
                        onShowPopup()
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Image section (top 2/3)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(2f)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(course.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = course.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Gradient overlay for readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(gradient)
                        )

                        // Progress chip (top-right) from API
                        val progressText = if (course.progress == null) "0%" else course.progress.progressPercent.let { "$it%" }
                        if (progressText.isNotBlank()) {
                            Surface(
                                color = Color.White.copy(alpha = 0.9f),
                                shadowElevation = 2.dp,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = progressText,
                                    color = HomeColors.DuolingoDarkGreen,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Text section (bottom 1/3) - title only
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = course.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HomeColors.DuolingoDarkGreen,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Popup positioned to the right of the card
            if (isPopupVisible) {
                AnimatedVisibility(
                    visible = isPopupVisible,
                    enter = fadeIn() + scaleIn(initialScale = 0.9f),
                    exit = fadeOut() + scaleOut()
                ) {
                    val density = LocalDensity.current
                    val popupWidth = 180.dp
                    val popupHeightGuess = 100.dp
                    val offset = cardRect?.let { rect ->
                        val offsetX = (rect.left + with(density) { 8.dp.toPx() }).toInt()
                        val offsetY = (rect.top - with(density) { popupHeightGuess.toPx() } + with(density) { 8.dp.toPx() }).toInt()
                        IntOffset(offsetX, offsetY)
                    } ?: IntOffset(0, 0)

                    Popup(
                        alignment = Alignment.TopStart,
                        offset = offset,
                        properties = PopupProperties(focusable = false)
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(popupWidth)
                                .shadow(8.dp, RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { /* Consume clicks to prevent propagation */ },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.95f)
                        ) {
                            var expanded by remember { mutableStateOf(false) }
                            Column(modifier = Modifier.padding(12.dp)) {
                                // HTML description only
                                if (descriptionPlain.isNotBlank()) {
                                    AndroidView(
                                        factory = { ctx ->
                                            TextView(ctx).apply {
                                                text = Html.fromHtml(descriptionPlain, Html.FROM_HTML_MODE_LEGACY)
                                                ellipsize = TextUtils.TruncateAt.END
                                                maxLines = if (expanded) Int.MAX_VALUE else 2
                                            }
                                        },
                                        update = { tv ->
                                            tv.text = Html.fromHtml(descriptionPlain, Html.FROM_HTML_MODE_LEGACY)
                                            tv.maxLines = if (expanded) Int.MAX_VALUE else 2
                                            tv.ellipsize = TextUtils.TruncateAt.END
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (!expanded) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "xem thêm",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = HomeColors.DuolingoGreen,
                                            modifier = Modifier.clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { expanded = true }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    onClick = {
                                        onHidePopup()
                                        onClick()
                                    },
                                    color = ctaBackgroundColor,
                                    shape = RoundedCornerShape(10.dp),
                                    shadowElevation = 1.dp
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(ctaText, color = ctaContentColor, style = MaterialTheme.typography.labelLarge)
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ctaContentColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

