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
    viewModel: GoogleSignInViewModel = hiltViewModel()
) {
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    
    // State để track initial destination
    var initialDestination by remember { mutableStateOf<String?>(null) }
    
    // Effect để xác định initial destination
    LaunchedEffect(initialLoginState) {
        if (initialDestination == null) {
            if (initialLoginState) {
                // Nếu SplashActivity đã xác nhận user đã đăng nhập, đi thẳng đến courses
                initialDestination = "courses"
            } else {
                // Đợi userData được load hoặc timeout sau 2 giây
                var attempts = 0
                while (userData == null && attempts < 40) {
                    delay(50)
                    attempts++
                }
                
                initialDestination = if (userData?.isLoggedIn == true && !userData?.accessToken.isNullOrEmpty()) {
                    "courses"
                } else {
                    "home"
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