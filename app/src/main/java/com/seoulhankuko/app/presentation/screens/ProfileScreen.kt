package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.seoulhankuko.app.presentation.viewmodel.GoogleSignInViewModel
import com.seoulhankuko.app.presentation.components.BottomNavigationBar
import com.seoulhankuko.app.presentation.components.BottomNavigationRoute

@Composable
private fun UserAvatar(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Int = 60
) {
    Card(
        modifier = modifier.size(size.dp),
        shape = CircleShape
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
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF81C784)) // Nền xanh lá cây nhạt
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
                .padding(bottom = 0.dp)
        ) {
            Text(
                text = "👤 Profile",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20), // Xanh lá đậm cho title
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // User Info Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar
                        UserAvatar(
                            avatarUrl = userData?.avatarUrl,
                            size = 60
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = userData?.name ?: userData?.email ?: "User",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                            Text(
                                text = userData?.email ?: "Korean Learner",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Profile Options
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf(
                    Pair("Settings", "⚙️"),
                    Pair("Statistics", "📊"),
                    Pair("Achievements", "🏆"),
                    Pair("Help", "❓"),
                    Pair("About", "ℹ️")
                )) { (title, icon) ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = icon,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }
                
                // Logout button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.signOut(context)
                                onLogout()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "🚪 Logout",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Red
                            )
                        }
                    }
                }
            }
        }
        
        // Bottom Navigation Bar
        BottomNavigationBar(
            currentRoute = BottomNavigationRoute.PROFILE,
            onNavigateToHome = onNavigateToHome,
            onNavigateToChallenge = onNavigateToChallenge,
            onNavigateToNotification = onNavigateToNotification,
            onNavigateToProfile = { /* Current screen */ }
        )
    }
}
