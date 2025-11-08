package com.seoulhankuko.app.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.seoulhankuko.app.R
import com.seoulhankuko.app.presentation.components.MainScaffold
import com.seoulhankuko.app.presentation.components.TopBarState
import com.seoulhankuko.app.presentation.utils.MiscColors
import com.seoulhankuko.app.presentation.utils.ProfileColors
import com.seoulhankuko.app.presentation.viewmodel.GoogleSignInViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToNotification: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigateToSettings: (() -> Unit)? = null,
    onEditProfile: () -> Unit = {},
    onChangeLanguage: () -> Unit = {},
    onReviewMistakes: () -> Unit = {},
    onAvatarClick: (() -> Unit)? = null,
    viewModel: GoogleSignInViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userData by viewModel.userData.collectAsStateWithLifecycle()

    var showLogoutDialog by remember { mutableStateOf(false) }

    MainScaffold(
        topBarState = TopBarState(
            userName = userData?.name ?: userData?.email ?: "Người học",
            streakDays = userData?.streakDays ?: 0,
            exp = userData?.exp ?: 0,
            courseTitle = null,
            courseThumbnailUrl = userData?.avatarUrl,
            isVisible = true,
            isShown = true
        ),
        currentRoute = "profile",
        onNavigateToHome = onNavigateToHome,
        onNavigateToNotification = onNavigateToNotification,
        onNavigateToProfile = onNavigateToProfile,
        onAvatarClick = onAvatarClick ?: onNavigateToProfile,
        containerColor = ProfileColors.BackgroundLight
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ProfileColors.BackgroundLight)
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                UserInfoCard(
                    avatarUrl = userData?.avatarUrl,
                    username = userData?.name ?: userData?.email ?: "Người học",
                    koreanLevel = null,
                    streak = userData?.streakDays ?: 0,
                    exp = userData?.exp ?: 0,
                    onAvatarClick = onAvatarClick
                )
            }

            item {
                MistakesReviewSection(
                    onReviewClick = onReviewMistakes
                )
            }

            item {
                SettingsSection(
                    onEditProfile = onEditProfile,
                    onChangeLanguage = onChangeLanguage
                )
            }

            item {
                LogoutButton(
                    onClick = { showLogoutDialog = true }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                viewModel.signOut(context)
                showLogoutDialog = false
                onLogout()
            }
        )
    }
}

@Composable
private fun UserInfoCard(
    avatarUrl: String?,
    username: String,
    koreanLevel: String?,
    streak: Int,
    exp: Int,
    onAvatarClick: (() -> Unit)?
) {
    val visibilityState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    AnimatedVisibility(
        visibleState = visibilityState,
        enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.92f, animationSpec = tween(250)),
        exit = fadeOut(tween(150)) + scaleOut(tween(150))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    UserAvatar(
                        avatarUrl = avatarUrl,
                        size = 96.dp,
                        onClick = onAvatarClick
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = username,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ProfileColors.TextPrimary
                        )
                        if (!koreanLevel.isNullOrBlank()) {
                            Text(
                                text = koreanLevel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ProfileColors.TextSecondary
                            )
                        }
                    }
                }

                Divider(color = ProfileColors.GradientStart.copy(alpha = 0.15f))

                ProfileStatsRow(
                    streak = streak,
                    exp = exp
                )
            }
        }
    }
}

@Composable
private fun ProfileStatsRow(
    streak: Int,
    exp: Int
) {
    val streakTransition = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    val expTransition = remember { MutableTransitionState(false) }

    LaunchedEffect(Unit) {
        delay(120)
        expTransition.targetState = true
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visibleState = streakTransition,
            modifier = Modifier.weight(1f),
            enter = fadeIn(tween(260)) + scaleIn(initialScale = 0.9f, animationSpec = tween(260)),
            exit = fadeOut(tween(180)) + scaleOut(tween(180))
        ) {
            val streakIcon = painterResource(
                id = if (streak > 0) {
                    R.drawable.fire_svgrepo_com
                } else {
                    R.drawable.fire_2_svgrepo_com
                }
            )
            ProfileStatItem(
                iconPainter = streakIcon,
                value = streak.toString(),
                label = "Streak",
                accentColor = MiscColors.Orange
            )
        }

        AnimatedVisibility(
            visibleState = expTransition,
            modifier = Modifier.weight(1f),
            enter = fadeIn(tween(260)) + scaleIn(initialScale = 0.9f, animationSpec = tween(260)),
            exit = fadeOut(tween(180)) + scaleOut(tween(180))
        ) {
            ProfileStatItem(
                iconImageVector = Icons.Default.Star,
                value = exp.toString(),
                label = "EXP",
                accentColor = MiscColors.Amber
            )
        }
    }
}

@Composable
private fun ProfileStatItem(
    iconPainter: Painter? = null,
    iconImageVector: ImageVector? = null,
    value: String,
    label: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.18f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    iconPainter != null -> {
                        Icon(
                            painter = iconPainter,
                            contentDescription = null,
                            tint = accentColor
                        )
                    }
                    iconImageVector != null -> {
                        Icon(
                            imageVector = iconImageVector,
                            contentDescription = null,
                            tint = accentColor
                        )
                    }
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = ProfileColors.TextSecondary
            )
        }
    }
}

@Composable
private fun MistakesReviewSection(
    onReviewClick: () -> Unit
) {
    val visibilityState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    AnimatedVisibility(
        visibleState = visibilityState,
        enter = fadeIn(tween(280)) + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut(tween(180)) + slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Mistakes Review",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ProfileColors.TextPrimary
                )
                Text(
                    text = "Ôn lại các câu sai để giữ phong độ học tập nhé!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ProfileColors.TextSecondary
                )
                Button(
                    onClick = onReviewClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ProfileColors.AccentGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Review Mistakes",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    onEditProfile: () -> Unit,
    onChangeLanguage: () -> Unit
) {
    val visibilityState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    AnimatedVisibility(
        visibleState = visibilityState,
        enter = fadeIn(tween(300, delayMillis = 80)) + slideInVertically(initialOffsetY = { it / 3 }),
        exit = fadeOut(tween(180)) + slideOutVertically(targetOffsetY = { it / 3 })
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ProfileColors.TextPrimary,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)
                )
                SettingsOptionItem(
                    iconPainter = rememberVectorPainter(Icons.Default.Edit),
                    title = "Edit Profile",
                    subtitle = "Cập nhật thông tin và mục tiêu của bạn",
                    onClick = onEditProfile
                )
                Divider(color = ProfileColors.GradientStart.copy(alpha = 0.1f))
                SettingsOptionItem(
                    iconPainter = painterResource(R.drawable.kr),
                    title = "Change Language",
                    subtitle = "Đổi ngôn ngữ hiển thị ứng dụng",
                    onClick = onChangeLanguage
                )
            }
        }
    }
}

@Composable
private fun SettingsOptionItem(
    iconPainter: Painter,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = ProfileColors.GradientStart.copy(alpha = 0.14f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    tint = ProfileColors.GradientEnd
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = ProfileColors.TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ProfileColors.TextSecondary
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ProfileColors.TextSecondary.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun LogoutButton(
    onClick: () -> Unit
) {
    val visibilityState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    AnimatedVisibility(
        visibleState = visibilityState,
        enter = fadeIn(tween(320, delayMillis = 140)) + scaleIn(initialScale = 0.9f, animationSpec = tween(320, delayMillis = 140)),
        exit = fadeOut(tween(180)) + scaleOut(tween(180))
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Đăng xuất",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LogoutConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        val transitionState = remember {
            MutableTransitionState(false).apply { targetState = true }
        }

        AnimatedVisibility(
            visibleState = transitionState,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.85f, animationSpec = tween(220)),
            exit = fadeOut(tween(160)) + scaleOut(tween(160))
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Bạn có chắc muốn đăng xuất?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ProfileColors.TextPrimary
                    )
                    Text(
                        text = "Nếu thoát sẽ mất tiến độ hiện tại.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ProfileColors.TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = ProfileColors.TextSecondary
                            )
                        ) {
                            Text(
                                text = "Hủy",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Đăng xuất",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserAvatar(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    onClick: (() -> Unit)?
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "avatarScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(ProfileColors.GradientStart.copy(alpha = 0.2f))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl.isNullOrBlank()) {
            DefaultAvatarIcon()
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "User avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DefaultAvatarIcon() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileColors.GradientEnd),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Default avatar",
            tint = Color.White
        )
    }
}

