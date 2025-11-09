package com.seoulhankuko.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.seoulhankuko.app.R
import com.seoulhankuko.app.presentation.utils.HomeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarSection(
    userName: String,
    onAvatarClick: () -> Unit,
    streakDays: Int,
    exp: Int,
    courseTitle: String?,
    courseThumbnailUrl: String?,
    isVisible: Boolean,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    var avatarScaleTarget by remember { mutableStateOf(1f) }
    val avatarScale by animateFloatAsState(
        targetValue = avatarScaleTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "avatar_click_scale"
    )

    var streakScaleTarget by remember { mutableStateOf(1f) }
    val streakScale by animateFloatAsState(
        targetValue = streakScaleTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "streak_pulse_scale"
    )

    var expScaleTarget by remember { mutableStateOf(1f) }
    val expScale by animateFloatAsState(
        targetValue = expScaleTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "exp_pulse_scale"
    )

    LaunchedEffect(streakDays) {
        streakScaleTarget = 1.1f
        delay(180)
        streakScaleTarget = 1f
    }

    LaunchedEffect(exp) {
        expScaleTarget = 1.1f
        delay(180)
        expScaleTarget = 1f
    }

    val iconPulseTransition = rememberInfiniteTransition(label = "icon_pulse_transition")
    val fireIconScale by iconPulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fire_icon_scale"
    )
    val starIconScale by iconPulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_icon_scale"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it / 2 }
        ) + fadeIn(
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it / 2 }
        ) + fadeOut(animationSpec = androidx.compose.animation.core.tween(durationMillis = 200))
    ) {
        TopAppBar(
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = HomeColors.DuolingoDarkGreen
                        )
                    }
                }
            },
            title = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconSize = 20.dp

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .scale(streakScale)
                                .semantics { contentDescription = "Chuỗi hiện tại: $streakDays ngày" },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = streakDays.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HomeColors.DuolingoDarkGreen
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.fire_svgrepo_com),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(iconSize)
                                    .scale(fireIconScale)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .scale(expScale)
                                .semantics { contentDescription = "Điểm EXP hiện tại: $exp" },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = exp.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HomeColors.DuolingoDarkGreen
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.stars),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(iconSize)
                                    .scale(starIconScale)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    val fallbackCourseTitle = courseTitle ?: "Khóa học hiện tại"
                    val thumbnailDescription = "Khóa học hiện tại: $fallbackCourseTitle"
                    val thumbnailModifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .semantics { contentDescription = thumbnailDescription }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!courseThumbnailUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = courseThumbnailUrl,
                                contentDescription = thumbnailDescription,
                                modifier = thumbnailModifier,
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = thumbnailModifier
                                    .background(HomeColors.DuolingoLightGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = fallbackCourseTitle.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HomeColors.DuolingoDarkGreen
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .scale(avatarScale)
                                .semantics { contentDescription = "Ảnh đại diện người dùng" }
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    coroutineScope.launch {
                                        avatarScaleTarget = 0.92f
                                        delay(90)
                                        avatarScaleTarget = 1.05f
                                        delay(120)
                                        avatarScaleTarget = 1f
                                    }
                                    onAvatarClick()
                                }
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                color = HomeColors.DuolingoGreen,
                                shape = CircleShape,
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = userName.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White,
                titleContentColor = HomeColors.DuolingoDarkGreen
            ),
            modifier = Modifier.shadow(elevation = 2.dp)
        )
    }
}
