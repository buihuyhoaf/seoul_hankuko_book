package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.seoulhankuko.app.presentation.viewmodel.GoogleSignInViewModel
import com.seoulhankuko.app.presentation.viewmodel.EntryTestFlowViewModel
import com.seoulhankuko.app.presentation.components.BottomNavigationBar
import com.seoulhankuko.app.presentation.components.BottomNavigationRoute
import com.seoulhankuko.app.presentation.components.EntryTestReminderDialog

@Composable
fun HomeScreen(
    onCourseSelected: (courseId: Int) -> Unit,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit = {},
    onNavigateToChallenge: () -> Unit = {},
    onNavigateToNotification: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToEntryTest: () -> Unit = {}, // New callback for entry test navigation
    viewModel: GoogleSignInViewModel = hiltViewModel(),
    entryTestFlowViewModel: EntryTestFlowViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    
    // State for entry test reminder popup
    var showEntryTestReminder by remember { mutableStateOf(false) }
    
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
                text = "📚 Courses",
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
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(listOf(1, 2, 3, 4, 5)) { courseId ->
                Card(
                    onClick = { onCourseSelected(courseId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Course $courseId",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Korean Language Course $courseId",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Click to start learning!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32) // Xanh lá đậm cho text
                        )
                    }
                }
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
}
