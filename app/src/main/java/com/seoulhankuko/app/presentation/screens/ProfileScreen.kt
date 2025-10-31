package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
private fun UserAvatar(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Int = 80
) {
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
                    .clip(CircleShape),
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
            .background(Color(0xFF1B5E20)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "👤",
            style = MaterialTheme.typography.displayMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToChallenge: () -> Unit = {},
    onNavigateToNotification: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: GoogleSignInViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = { /* TODO: Open settings */ }) {
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
                modifier = Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF81C784),
                            Color(0xFF1B5E20)
                        )
                    )
                )
            )
        },
        bottomBar = {
            ModernBottomNavigationBar(
                currentRoute = "profile",
                onNavigateToHome = onNavigateToHome,
                onNavigateToChallenge = onNavigateToChallenge,
                onNavigateToNotification = onNavigateToNotification,
                onNavigateToProfile = { /* Current screen */ }
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Info Card
            item {
                UserInfoCard(
                    avatarUrl = userData?.avatarUrl,
                    name = userData?.name ?: userData?.email ?: "User",
                    email = userData?.email ?: ""
                )
            }
            
            // Feature List
            items(listOf(
                ProfileFeature("Settings", "⚙️"),
                ProfileFeature("Statistics", "📊"),
                ProfileFeature("Achievements", "🏆"),
                ProfileFeature("Help", "❓"),
                ProfileFeature("About", "ℹ️")
            )) { feature ->
                FeatureCard(
                    feature = feature,
                    onClick = {
                        // Show toast or snackbar
                        // TODO: Implement navigation or actions
                    }
                )
            }
            
            // Logout Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                LogoutButton(
                    onClick = {
                        viewModel.signOut(context)
                        onLogout()
                    }
                )
            }
        }
    }
}

data class ProfileFeature(
    val title: String,
    val icon: String
)

@Composable
fun UserInfoCard(
    avatarUrl: String?,
    name: String,
    email: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar
                UserAvatar(
                    avatarUrl = avatarUrl,
                    size = 80
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                
                // Edit Profile Button
                IconButton(onClick = { /* TODO: Open edit profile */ }) {
                    Text(
                        text = "✏️",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureCard(
    feature: ProfileFeature,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = feature.icon,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1B5E20),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun LogoutButton(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Red
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🚪 Logout",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
