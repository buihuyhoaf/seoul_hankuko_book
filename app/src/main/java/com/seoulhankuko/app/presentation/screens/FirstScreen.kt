package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.R
import com.seoulhankuko.app.domain.model.AuthState
import com.seoulhankuko.app.presentation.viewmodel.HomeViewModel
import com.seoulhankuko.app.core.Logger

@Composable
fun FirstScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToLearn: (courseId: Int) -> Unit,
    onNavigateToEntryTest: () -> Unit = {}, // New callback for entry test
    onNavigateToGuestMode: () -> Unit = {}, // New callback for guest mode
    viewModel: HomeViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    
    // Log auth state changes
    LaunchedEffect(authState) {
        when (val currentAuthState = authState) {
            is AuthState.Loading -> Logger.HomeScreen.authStateChanged("Loading")
            is AuthState.SignedOut -> Logger.HomeScreen.authStateChanged("SignedOut")
            is AuthState.Guest -> Logger.HomeScreen.authStateChanged("Guest")
            is AuthState.SignedIn -> Logger.HomeScreen.authStateChanged("SignedIn (ID: ${currentAuthState.userId})")
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // Force white background
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.width(IntrinsicSize.Max)
        ) {
            // Hero Image - Frame image
            Box(
                modifier = Modifier
                    .size(300.dp) // Made bigger
                    .clip(RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                // Frame image from drawable
                Image(
                    painter = painterResource(id = R.drawable.frame),
                    contentDescription = "Seoul Hankuko Book Logo",
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            
            // Main content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.home_subtitle),
                    style = MaterialTheme.typography.headlineMedium, // Made bigger
                    fontWeight = FontWeight.Bold,
                    color = Color.Black, // Force black color for dark theme compatibility
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
                                    viewModel.enterGuestMode()
                                    onNavigateToGuestMode() // Navigate to guest mode (skip entry test)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp), // Fixed height để button to hơn
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF58CC02), // Màu nền 58CC02
                                    contentColor = Color(0xFFFFFFFF) // Màu chữ FFFFFF
                                )
                            ) {
                                Text(
                                    stringResource(id = R.string.get_started),
                                    color = Color(0xFFFFFFFF),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    Logger.HomeScreen.navigateToLoginClicked()
                                    onNavigateToLogin()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp), // Fixed height để button to hơn
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFFFFFFFF), // Màu nền FFFFFF
                                    contentColor = Color(0xFF1CB0F6) // Màu chữ 1CB0F6
                                ),
                                border = BorderStroke(1.dp, Color(0xFFE5E5E5)) // Border màu E5E5E5
                            ) {
                                Text(
                                    stringResource(id = R.string.already_have_account),
                                    color = Color(0xFF1CB0F6),
                                    modifier = Modifier.padding(vertical = 8.dp)
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
                        is AuthState.Guest -> {
                            Button(
                                onClick = { 
                                    Logger.HomeScreen.continueLearningClicked()
                                    onNavigateToLearn(1) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1B5E20)
                                )
                            ) {
                                Text(
                                    "Continue Learning (Guest)",
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
                                    "Sign In to Save Progress",
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
