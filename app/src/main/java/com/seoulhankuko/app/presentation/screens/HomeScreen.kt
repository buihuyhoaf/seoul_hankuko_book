package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.R
import com.seoulhankuko.app.domain.model.AuthState
import com.seoulhankuko.app.presentation.components.BottomNavigationBar
import com.seoulhankuko.app.presentation.components.BottomNavigationRoute
import com.seoulhankuko.app.presentation.components.EntryTestReminderDialog
import com.seoulhankuko.app.presentation.viewmodel.AuthViewModel
import com.seoulhankuko.app.presentation.viewmodel.EntryTestFlowViewModel
import com.seoulhankuko.app.presentation.viewmodel.GoogleSignInViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CourseCard(
    courseId: Int,
    onClick: () -> Unit,
    isNew: Boolean = false,
    isFeatured: Boolean = false
) {
    val progressPercent = (courseId * 15 + 25) % 100 // Mock progress data
    val lessonsCount = 5 + courseId * 2 // Mock lessons count
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Course Image/Icon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F5E8)), // Light green background for course image
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📚",
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Course Title
                Text(
                    text = stringResource(R.string.course_title, courseId),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Course Description
                Text(
                    text = stringResource(R.string.course_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress Section
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF58CC02),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.lessons_count, lessonsCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Progress Bar
                    LinearProgressIndicator(
                        progress = progressPercent / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFF58CC02),
                        trackColor = Color(0xFFE0E0E0)
                    )
                }
            }
            
            // Badge for New/Featured courses
            if (isNew || isFeatured) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isFeatured) Color(0xFF58CC02) else Color(0xFFFF6B35)
                    ) {
                        Text(
                            text = if (isFeatured) stringResource(R.string.course_featured) else stringResource(R.string.course_new),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    onCourseSelected: (courseId: Int) -> Unit,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit = {},
    onNavigateToChallenge: () -> Unit = {},
    onNavigateToNotification: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToEntryTest: () -> Unit = {}, // New callback for entry test navigation
    onNavigateToLogin: () -> Unit = {}, // New callback for login navigation
    viewModel: GoogleSignInViewModel = hiltViewModel(),
    entryTestFlowViewModel: EntryTestFlowViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    
    // State for entry test reminder popup
    var showEntryTestReminder by remember { mutableStateOf(false) }
    
    // State for guest login prompt popup
    var showGuestLoginPrompt by remember { mutableStateOf(false) }
    
    // Check if user is logged in and should show entry test popup
    // Use Unit as key to ensure this runs every time the screen is composed
    LaunchedEffect(Unit) {
        val isLoggedIn = userData?.isLoggedIn == true && !userData?.accessToken.isNullOrEmpty()
        if (isLoggedIn) {
            // Add a small delay to ensure MainActivity's reset logic has completed
            delay(500)
            val shouldShowPopup = entryTestFlowViewModel.shouldShowEntryTestPopup()
            showEntryTestReminder = shouldShowPopup
        } else {
            showEntryTestReminder = false
        }
    }
    
    // Check if guest user should be prompted to login (after X lessons)
    LaunchedEffect(authState) {
        if (authState is AuthState.Guest) {
            delay(500)
            val shouldPrompt = authViewModel.shouldPromptGuestToLogin(3)
            showGuestLoginPrompt = shouldPrompt
        } else {
            showGuestLoginPrompt = false
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF81C784)) // Nền xanh lá cây nhạt
    ) {
        // Main content với padding trừ phần bottom navigation
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
                .padding(bottom = 0.dp) // Remove bottom padding để tránh overlap với bottom nav
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Không có nút back nữa - để title ở giữa
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = stringResource(R.string.courses_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20) // Xanh lá đậm cho title
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            TextButton(
                onClick = {
                    viewModel.signOut(context)
                    onLogout()
                }
            ) {
                Text(
                    text = "Logout",
                    color = Color(0xFF1B5E20) // Màu chữ cho nút Logout
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            items(listOf(1, 2, 3, 4, 5, 6)) { courseId ->
                CourseCard(
                    courseId = courseId,
                    onClick = { onCourseSelected(courseId) },
                    isNew = courseId <= 2, // First 2 courses marked as "New"
                    isFeatured = courseId == 1 // First course is "Featured"
                )
            }
        }
        }
        
        // Bottom Navigation Bar
        BottomNavigationBar(
            currentRoute = BottomNavigationRoute.HOME,
            onNavigateToHome = { /* Current screen */ },
            onNavigateToChallenge = onNavigateToChallenge,
            onNavigateToNotification = onNavigateToNotification,
            onNavigateToProfile = onNavigateToProfile
        )
    }
    
    // Entry Test Reminder Dialog
    EntryTestReminderDialog(
        show = showEntryTestReminder,
        onStartEntryTest = {
            showEntryTestReminder = false
            onNavigateToEntryTest()
        },
        onCancel = {
            showEntryTestReminder = false
            coroutineScope.launch {
                entryTestFlowViewModel.dismissEntryTestPopup()
            }
        }
    )
    
    // Guest Login Prompt Dialog
    if (showGuestLoginPrompt) {
        AlertDialog(
            onDismissRequest = { showGuestLoginPrompt = false },
            title = { Text("Save Your Progress") },
            text = { Text("You've completed 3 lessons! Sign in to save your progress and continue learning.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGuestLoginPrompt = false
                        onNavigateToLogin()
                    }
                ) {
                    Text("Sign In")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGuestLoginPrompt = false }
                ) {
                    Text("Continue as Guest")
                }
            }
        )
    }
}
