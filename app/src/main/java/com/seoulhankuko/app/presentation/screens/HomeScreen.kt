package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.domain.model.AuthState
import com.seoulhankuko.app.presentation.viewmodel.HomeViewModel
import com.seoulhankuko.app.core.Logger

@Composable
fun HomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToLearn: (courseId: Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    
    // Log auth state changes
    LaunchedEffect(authState) {
        when (val currentAuthState = authState) {
            is AuthState.Loading -> Logger.HomeScreen.authStateChanged("Loading")
            is AuthState.SignedOut -> Logger.HomeScreen.authStateChanged("SignedOut")
            is AuthState.SignedIn -> Logger.HomeScreen.authStateChanged("SignedIn (ID: ${currentAuthState.userId})")
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.width(IntrinsicSize.Max)
        ) {
            // Hero Image placeholder - using a simple colored box for now
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                // Placeholder for hero image - in real app, this would be a drawable or network image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🇰🇷\nSeoul\nHankuko\nBook",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20), // Xanh lá đậm cho logo text
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Main content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Text(
                    text = "Learn, practice, and master Korean with Seoul Hankuko Book.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                // Authentication buttons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.width(IntrinsicSize.Max)
                ) {
                    when (authState) {
                        is AuthState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        is AuthState.SignedOut -> {
                            Button(
                                onClick = {
                                    Logger.HomeScreen.navigateToRegisterClicked()
                                    onNavigateToRegister()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32) // Xanh lá đậm cho button
                                )
                            ) {
                                Text(
                                    "Get Started",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    Logger.HomeScreen.navigateToLoginClicked()
                                    onNavigateToLogin()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "I already have an account",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        is AuthState.SignedIn -> {
                            Button(
                                onClick = { 
                                    Logger.HomeScreen.continueLearningClicked()
                                    onNavigateToLearn(1) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1B5E20) // Xanh lá đậm nhất cho button chính
                                )
                            ) {
                                Text(
                                    "Continue Learning",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            
                            OutlinedButton(
                                onClick = { 
                                    Logger.HomeScreen.signOutClicked()
                                    viewModel.signOut() 
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Sign Out",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
