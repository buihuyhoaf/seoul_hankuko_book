package com.seoulhankuko.app.presentation.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.seoulhankuko.app.MainActivity
import com.seoulhankuko.app.R
import com.seoulhankuko.app.presentation.ui.theme.SeoulhankukobookTheme
import com.seoulhankuko.app.presentation.viewmodel.GoogleSignInViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SplashActivity - Màn hình splash screen với tính năng auto login
 * Kiểm tra Google Sign-In session và điều hướng phù hợp
 */
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Cài đặt SplashScreen API (Android 12+) - phải gọi trước setContent
        val splashScreen = installSplashScreen()
        
        // Set content với Jetpack Compose
        setContent {
            SeoulhankukobookTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SplashScreenWithAutoLogin(
                        onNavigateToMain = { isLoggedIn ->
                            navigateToMainActivity(isLoggedIn)
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Điều hướng đến MainActivity
     */
    private fun navigateToMainActivity(isLoggedIn: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("isLoggedIn", isLoggedIn)
        startActivity(intent)
        finish() // Đóng SplashActivity để không thể quay lại
    }
}

/**
 * Composable cho Splash Screen với tính năng auto login
 * Kiểm tra Google Sign-In session và điều hướng phù hợp
 */
@Composable
fun SplashScreenWithAutoLogin(
    onNavigateToMain: (Boolean) -> Unit,
    viewModel: GoogleSignInViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // Kiểm tra JWT token trong DataStore
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    
    // Animation states cho fade in effect
    val logoAlpha = remember { Animatable(0f) }
    val appNameAlpha = remember { Animatable(0f) }
    val sloganAlpha = remember { Animatable(0f) }
    
    // Bắt đầu animations khi Composable được tạo
    LaunchedEffect(Unit) {
        // Logo fade in trong 1 giây
        logoAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
        
        // Tên app fade in sau logo 300ms
        delay(300)
        appNameAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
        
        // Slogan fade in sau tên app 200ms
        delay(200)
        sloganAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
        
        // Delay tổng cộng 1.5 giây cho hiệu ứng
        delay(500) // Tổng cộng đã có 1.5 giây
    }
    
    // Xử lý navigation sau khi animations hoàn thành và userData đã sẵn sàng
    LaunchedEffect(userData) {
        // Đợi animations hoàn thành (1.5 giây)
        delay(1500)
        
        // Kiểm tra trạng thái đăng nhập và truyền thông tin qua Intent
        val isLoggedIn = userData?.isLoggedIn == true && !userData?.accessToken.isNullOrEmpty()
        
        onNavigateToMain(isLoggedIn)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E7D32)), // Xanh lá đậm cho SplashScreen
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo với fade in animation
            Image(
                painter = painterResource(id = R.drawable.duolingo_icon_logo),
                contentDescription = stringResource(R.string.duolingo_logo_description),
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .alpha(logoAlpha.value)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tên app với fade in animation
            Text(
                text = stringResource(R.string.splash_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White, // Màu trắng cho text trên nền xanh lá
                modifier = Modifier.alpha(appNameAlpha.value)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Slogan với fade in animation
            Text(
                text = stringResource(R.string.splash_subtitle),
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f), // Màu trắng nhạt cho slogan
                modifier = Modifier.alpha(sloganAlpha.value)
            )
        }
    }
}
