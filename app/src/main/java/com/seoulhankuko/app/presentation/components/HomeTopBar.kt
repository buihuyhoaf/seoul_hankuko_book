package com.seoulhankuko.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.seoulhankuko.app.R
import com.seoulhankuko.app.presentation.utils.HomeColors
import com.seoulhankuko.app.domain.manager.ExpBonusManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import coil.request.ImageRequest

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ExpBonusManagerEntryPoint {
    fun expBonusManager(): ExpBonusManager
}

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarSection(
    userName: String,
    onAvatarClick: () -> Unit,
    streakDays: Int,
    exp: Int,
    courseTitle: String?,
    courseThumbnailUrl: String?,
    avatarUrl: String?,
    isVisible: Boolean,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Get ExpBonusManager from Hilt
    val expBonusManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ExpBonusManagerEntryPoint::class.java
        ).expBonusManager()
    }
    
    // Track 2x EXP event status
    var hasExpEvent by remember { mutableStateOf(false) }
    
    // Poll ExpBonusManager status periodically
    LaunchedEffect(expBonusManager) {
        while (true) {
            hasExpEvent = expBonusManager.isActive()
            delay(1000) // Check every second
        }
    }

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
    val bottleIconScale by iconPulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bottle_icon_scale"
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
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.fire),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(iconSize)
                                    .scale(fireIconScale),
                                tint = Color.Unspecified
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
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.star),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(iconSize)
                                    .scale(starIconScale),
                                tint = Color.Unspecified
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Show bottle icon only when 2x EXP event is active
                    AnimatedVisibility(
                        visible = hasExpEvent,
                        enter = fadeIn(animationSpec = tween(durationMillis = 300)) + 
                                slideInVertically(
                                    initialOffsetY = { -it / 2 },
                                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                ),
                        exit = fadeOut(animationSpec = tween(durationMillis = 200)) + 
                               slideOutVertically(
                                   targetOffsetY = { -it / 2 },
                                   animationSpec = tween(durationMillis = 200)
                               )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.bottle_with_popping_cork),
                            contentDescription = "Sự kiện x2 EXP đang hoạt động",
                            modifier = Modifier
                                .size(48.dp)
                                .scale(bottleIconScale)
                                .padding(end = 8.dp),
                            tint = Color.Unspecified
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .scale(avatarScale)
                            .semantics { contentDescription = "Ảnh đại diện người dùng" }
                            .clip(CircleShape)
                            .then(
                                if (avatarUrl.isNullOrBlank()) {
                                    Modifier.background(HomeColors.DuolingoGreen)
                                } else {
                                    Modifier
                                }
                            )
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
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Ảnh đại diện người dùng",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.hanbok),
                                contentDescription = "Ảnh đại diện mặc định",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
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
