package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.presentation.viewmodel.EntryTestViewModel
import com.seoulhankuko.app.presentation.viewmodel.GoogleSignInViewModel
import com.seoulhankuko.app.presentation.viewmodel.EntryTestFlowViewModel
import com.seoulhankuko.app.presentation.components.LoginPromptDialog

@Composable
fun EntryTestScreen(
    onNavigateToResult: (score: Float, courseId: Int, courseName: String) -> Unit,
    onNavigateToLogin: () -> Unit = {}, // New callback for login navigation
    onNavigateBack: () -> Unit,
    allowOfflineMode: Boolean = true, // Allow entry test without login
    viewModel: EntryTestViewModel = hiltViewModel(),
    entryTestFlowViewModel: EntryTestFlowViewModel = hiltViewModel(),
    googleSignInViewModel: GoogleSignInViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userData by googleSignInViewModel.userData.collectAsStateWithLifecycle()
    
    // State for login prompt dialog
    var showLoginPrompt by remember { mutableStateOf(false) }
    
    // Check if user is authenticated
    val isAuthenticated = userData?.isLoggedIn == true && !userData?.accessToken.isNullOrEmpty()
    
    LaunchedEffect(Unit) {
        // Load entry test questions regardless of login status if offline mode is allowed
        viewModel.loadEntryTestQuestions(allowOffline = allowOfflineMode)
    }
    
    LaunchedEffect(uiState.isCompleted, uiState.submissionResult, isAuthenticated) {
        if (uiState.isCompleted && uiState.submissionResult != null) {
            val result = uiState.submissionResult!!
            
            if (isAuthenticated) {
                // User is logged in - proceed normally to result screen
                onNavigateToResult(result.score, result.recommendedCourseId, result.recommendedCourseTitle)
            } else {
                // User is not logged in - save offline and show login prompt
                entryTestFlowViewModel.saveEntryTestResultOffline(
                    score = result.score.toInt(),
                    courseId = result.recommendedCourseId,
                    courseName = result.recommendedCourseTitle
                )
                showLoginPrompt = true
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onNavigateBack) {
                Text("← Back")
            }
            
            Text(
                text = "🎯 Entry Test",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(80.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when {
            // Only enforce authentication if offline mode is disabled
            !allowOfflineMode && !isAuthenticated && userData != null -> {
                // User is not authenticated and offline mode is not allowed
                LaunchedEffect(Unit) {
                    onNavigateBack()
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Please login first to take the entry test",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            userData == null -> {
                // Still loading user data
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading entry test",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadEntryTestQuestions() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            
            uiState.questions.isNotEmpty() -> {
                val currentQuestion = uiState.questions.getOrNull(uiState.currentQuestionIndex)
                
                if (currentQuestion != null) {
                    // Progress indicator
                    Text(
                        text = "Question ${uiState.currentQuestionIndex + 1} of ${uiState.questions.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = (uiState.currentQuestionIndex + 1).toFloat() / uiState.questions.size,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Current question
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = currentQuestion.content,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                            
                            // Options
                            currentQuestion.options.forEach { option ->
                                val isSelected = uiState.selectedAnswers[currentQuestion.id] == option.id
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .selectable(
                                            selected = isSelected,
                                            onClick = { viewModel.selectAnswer(currentQuestion.id, option.id) }
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) 
                                            MaterialTheme.colorScheme.primaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Text(
                                        text = option.optionText,
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isSelected) 
                                            MaterialTheme.colorScheme.onPrimaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Navigation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (uiState.currentQuestionIndex > 0) {
                            OutlinedButton(onClick = { viewModel.previousQuestion() }) {
                                Text("Previous")
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        if (uiState.currentQuestionIndex < uiState.questions.size - 1) {
                            Button(
                                onClick = { viewModel.nextQuestion() },
                                enabled = uiState.selectedAnswers[currentQuestion.id] != null
                            ) {
                                Text("Next")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.submitTest() },
                                enabled = !uiState.isLoading && uiState.questions.all { question -> 
                                    uiState.selectedAnswers[question.id] != null 
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Submit Test")
                            }
                        }
                    }
                } else {
                    // Handle invalid question index
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Invalid question index",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            
            else -> {
                // No questions available
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No questions available",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
    
    // Login prompt dialog
    LoginPromptDialog(
        show = showLoginPrompt,
        onLoginClick = {
            showLoginPrompt = false
            onNavigateToLogin()
        },
        onRemindLaterClick = {
            showLoginPrompt = false
            onNavigateBack() // Go back to course screen
        }
    )
}
