package com.seoulhankuko.app.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.seoulhankuko.app.presentation.viewmodel.GoogleSignInViewModel
import com.seoulhankuko.app.presentation.components.ModernBottomNavigationBar
import com.seoulhankuko.app.presentation.utils.ProfileColors
import com.seoulhankuko.app.presentation.utils.MiscColors
import kotlinx.coroutines.launch

@Composable
private fun UserAvatar(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Int = 80,
    onClick: (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(200),
        label = "avatarScale"
    )
    
    Box(
        modifier = modifier.size(size.dp)
    ) {
        if (!avatarUrl.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "User Avatar",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .then(if (onClick != null) Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isPressed = true }
                        .scale(scale)
                    else Modifier),
                contentScale = ContentScale.Crop
            )
        } else {
            DefaultAvatarIcon()
        }
    }
}

@Composable
private fun DefaultAvatarIcon() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileColors.TextPrimary)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "User",
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToNotification: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    viewModel: GoogleSignInViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showReviewMistakes by remember { mutableStateOf(false) }
    
    Scaffold(
        bottomBar = {
            ModernBottomNavigationBar(
                currentRoute = "profile",
                onNavigateToHome = onNavigateToHome,
                onNavigateToNotification = onNavigateToNotification,
                onNavigateToProfile = { /* Current screen */ }
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Info Card
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    UserInfoCard(
                        avatarUrl = userData?.avatarUrl,
                        username = userData?.name ?: userData?.email ?: "User",
                        koreanLevel = "",
                        streak = userData?.streakDays ?: 0,
                        exp = userData?.exp ?: 0
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            // Mistakes Section
            item {
                MistakesReviewSection(
                    onReviewClick = { showReviewMistakes = true }
                )
            }
            
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            // Settings Section
            item {
                SettingsSection(
                    onEditProfile = { /* TODO: Navigate to edit profile */ },
                    onChangeLanguage = { /* TODO: Navigate to language settings */ },
                    onLogout = { showLogoutDialog = true }
                )
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
    
    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.signOut(context)
                        onLogout()
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopAppBar(
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Profile",
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
        actions = {
            IconButton(onClick = { /* TODO: Navigate to settings */ }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ProfileColors.GradientStart,
                        ProfileColors.GradientEnd
                    )
                )
            )
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(0.dp))
    )
}

@Composable
fun UserInfoCard(
    avatarUrl: String?,
    username: String,
    koreanLevel: String,
    streak: Int,
    exp: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar
                UserAvatar(
                    avatarUrl = avatarUrl,
                    size = 80
                )
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = username,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ProfileColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = koreanLevel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ProfileColors.TextSecondary
                    )
                }
            }
            
            HorizontalDivider(color = Color.LightGray)
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Streak
                StatItem(
                    icon = Icons.Default.AccountCircle,
                    value = "$streak",
                    label = "days",
                    iconTint = MiscColors.DeepOrange
                )
                
                // EXP
                StatItem(
                    icon = Icons.Default.Star,
                    value = "$exp",
                    label = "EXP",
                    iconTint = MiscColors.Amber
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    iconTint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = ProfileColors.TextPrimary
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
fun MistakesReviewSection(
    onReviewClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Mistakes to Review",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ProfileColors.TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Button(
            onClick = onReviewClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ProfileColors.AccentGreen
            )
        ) {
            Text(
                text = "Review Mistakes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
fun SettingsSection(
    onEditProfile: () -> Unit,
    onChangeLanguage: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ProfileColors.TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Edit Profile
        SettingsOptionItem(
            icon = Icons.Default.Person,
            title = "Edit Profile",
            onClick = onEditProfile
        )
        
        HorizontalDivider(color = Color.LightGray, modifier = Modifier.padding(vertical = 4.dp))
        
        // Change Language
        SettingsOptionItem(
            icon = Icons.Default.AccountBox,
            title = "Change Language",
            onClick = onChangeLanguage
        )
        
        HorizontalDivider(color = Color.LightGray, modifier = Modifier.padding(vertical = 4.dp))
        
        // Logout
        SettingsOptionItem(
            icon = Icons.Default.Close,
            title = "Logout",
            onClick = onLogout,
            textColor = MaterialTheme.colorScheme.error,
            iconTint = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun SettingsOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    textColor: Color = ProfileColors.TextPrimary,
    iconTint: Color = ProfileColors.TextPrimary
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}
