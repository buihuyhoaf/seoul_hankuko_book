package com.seoulhankuko.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.seoulhankuko.app.navigation.AppNavigation
import com.seoulhankuko.app.presentation.ui.theme.SeoulhankukobookTheme
import com.seoulhankuko.app.presentation.viewmodel.GoogleSignInViewModel
import com.seoulhankuko.app.presentation.viewmodel.EntryTestFlowViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.seoulhankuko.app.core.Logger

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Logger.MainActivity.onCreate()
        
        enableEdgeToEdge()
        
        Logger.MainActivity.setContentView()
        
        // Lấy thông tin đăng nhập từ Intent (từ SplashActivity)
        val isLoggedInFromSplash = intent.getBooleanExtra("isLoggedIn", false)
        
        setContent {
            SeoulhankukobookTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationWithAutoLogin(initialLoginState = isLoggedInFromSplash)
                }
            }
        }
    }
}

/**
 * App Navigation với tính năng auto login
 * Kiểm tra Google Sign-In session và điều hướng đến CoursesScreen nếu đã đăng nhập
 */
@Composable
fun AppNavigationWithAutoLogin(
    initialLoginState: Boolean = false,
    viewModel: GoogleSignInViewModel = hiltViewModel(),
    entryTestViewModel: EntryTestFlowViewModel = hiltViewModel()
) {
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    
    // State để track initial destination
    var initialDestination by remember { mutableStateOf<String?>(null) }
    
    // Effect để xác định initial destination
    LaunchedEffect(initialLoginState, userData) {
        if (initialDestination == null) {
            val isLoggedIn = if (initialLoginState) {
                // Nếu SplashActivity đã xác nhận user đã đăng nhập
                true
            } else {
                // Đợi userData được load hoặc timeout sau 2 giây
                var attempts = 0
                while (userData == null && attempts < 40) {
                    delay(50)
                    attempts++
                }
                userData?.isLoggedIn == true && !userData?.accessToken.isNullOrEmpty()
            }
            
            if (isLoggedIn) {
                // Logged-in user flow (Flow B & C from requirements)
                
                // Sync user data from backend first to get latest entry test status
                try {
                    val syncSuccess = entryTestViewModel.syncUserDataFromBackend()
                    // Add small delay to ensure sync completes before checking
                    if (syncSuccess) {
                        delay(100) // Small delay to ensure data is saved locally
                    }
                    
                    // Check if user has completed entry test (now synced with backend)
                    val hasCompletedEntryTest = entryTestViewModel.hasCompletedEntryTest()
                    
                    // Reset popup dismissal flag for logged-in users who haven't completed entry test
                    // This ensures popup will show again after app restart if user dismissed it before
                    if (!hasCompletedEntryTest) {
                        entryTestViewModel.resetEntryTestPopupDismissal()
                    }
                    
                    // Flow B & C: Always go to courses, popup will be handled in HomeScreen
                    initialDestination = "courses"
                } catch (e: Exception) {
                    // Continue with local data if sync fails
                    val hasCompletedEntryTest = entryTestViewModel.hasCompletedEntryTest()
                    
                    // Reset popup dismissal flag for logged-in users who haven't completed entry test
                    if (!hasCompletedEntryTest) {
                        entryTestViewModel.resetEntryTestPopupDismissal()
                    }
                    
                    // Always go to courses, popup will be handled in HomeScreen
                    initialDestination = "courses"
                }
            } else {
                // New user flow (Flow A from requirements)
                // Check if user needs entry test (either online or offline completion)
                try {
                    val needsEntryTest = entryTestViewModel.needsEntryTest()
                    if (needsEntryTest) {
                        // Flow A: New user, go to courses first, then show entry test popup
                        initialDestination = "courses"
                    } else {
                        // User has already completed entry test offline or online
                        initialDestination = "courses"
                    }
                } catch (e: Exception) {
                    // Default to home screen if there's any error
                    initialDestination = "home"
                }
            }
        }
    }
    
    // Render AppNavigation khi đã xác định destination
    initialDestination?.let { destination ->
        AppNavigation(
            initialDestination = destination
        )
    }
}