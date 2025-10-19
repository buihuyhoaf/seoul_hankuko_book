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
import com.seoulhankuko.app.presentation.viewmodel.LoggedAccountsViewModel
import com.seoulhankuko.app.presentation.viewmodel.AuthViewModel
import com.seoulhankuko.app.domain.model.AuthState
import dagger.hilt.android.AndroidEntryPoint
import com.seoulhankuko.app.core.Logger

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Logger.MainActivity.onCreate()
        
        enableEdgeToEdge()
        
        Logger.MainActivity.setContentView()
        
        setContent {
            SeoulhankukobookTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationWithAutoLogin()
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
    viewModel: GoogleSignInViewModel = hiltViewModel(),
    entryTestViewModel: EntryTestFlowViewModel = hiltViewModel(),
    loggedAccountsViewModel: LoggedAccountsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val loggedAccounts by authViewModel.loggedAccounts.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    
    // State để track initial destination
    var initialDestination by remember { mutableStateOf<String?>(null) }
    
    // Effect để xác định initial destination
    LaunchedEffect(authState, loggedAccounts) {
        if (initialDestination == null) {
            // Đợi một chút để đảm bảo AuthRepository đã được khởi tạo
            delay(100)
            
            // Kiểm tra trạng thái đăng nhập từ AuthRepository thay vì dựa vào SplashActivity
            val isLoggedIn = authViewModel.hasValidToken()
            
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
                // Not logged in - check if we have saved accounts
                if (loggedAccounts.isNotEmpty()) {
                    // User has logged accounts but no valid token - go to LoggedAccountsScreen
                    initialDestination = "logged-accounts"
                } else {
                    // No logged accounts - go to LoginScreen (new user flow)
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