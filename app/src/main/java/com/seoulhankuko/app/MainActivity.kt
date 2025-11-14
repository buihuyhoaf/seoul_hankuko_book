package com.seoulhankuko.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.core.Logger
import com.seoulhankuko.app.navigation.AppNavigation
import com.seoulhankuko.app.notifications.WritingNotificationCenter
import com.seoulhankuko.app.notifications.WritingNotificationEvent
import com.seoulhankuko.app.presentation.ui.theme.SeoulhankukobookTheme
import com.seoulhankuko.app.presentation.viewmodel.AuthViewModel
import com.seoulhankuko.app.presentation.viewmodel.GoogleSignInViewModel
import com.seoulhankuko.app.presentation.viewmodel.LoggedAccountsViewModel
import com.seoulhankuko.app.presentation.viewmodel.MainUiViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Logger.MainActivity.onCreate()
        
        enableEdgeToEdge()
        
        Logger.MainActivity.setContentView()
        
        // Check if launched from notification
        val lessonIdFromNotification = intent.getStringExtra(WritingNotificationCenter.EXTRA_TARGET_LESSON_ID)

        setContent {
            SeoulhankukobookTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationWithAutoLogin(initialLessonId = lessonIdFromNotification)
                    NotificationPermissionHandler()
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent) // Important: Update the intent để có thể đọc lại
        
        // Check if launched from notification khi app đang chạy
        val lessonIdFromNotification = intent?.getStringExtra(WritingNotificationCenter.EXTRA_TARGET_LESSON_ID)
        
        if (lessonIdFromNotification != null) {
            Timber.d("MainActivity: Received notification click with lesson_id=$lessonIdFromNotification")
            // Publish event để trigger navigation
            WritingNotificationCenter.publish(
                WritingNotificationEvent.WritingGraded(
                    lessonId = lessonIdFromNotification,
                    submissionId = intent?.getStringExtra("submission_id"),
                    title = intent?.getStringExtra("title") ?: "Bài viết đã được chấm",
                    body = intent?.getStringExtra("body") ?: "Giáo viên đã chấm bài viết của bạn"
                )
            )
        }
    }
}

/**
 * App Navigation với tính năng auto login
 * Kiểm tra Google Sign-In session và điều hướng đến CoursesScreen nếu đã đăng nhập
 */
@Composable
fun AppNavigationWithAutoLogin(
    initialLessonId: String? = null,
    viewModel: GoogleSignInViewModel = hiltViewModel(),
    loggedAccountsViewModel: LoggedAccountsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    mainUiViewModel: MainUiViewModel = hiltViewModel()
) {
    val loggedAccounts by authViewModel.loggedAccounts.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val currentLesson by mainUiViewModel.currentLesson.collectAsStateWithLifecycle()

    // State để track initial destination
    var initialDestination by remember { mutableStateOf<String?>(null) }
    var resumeHandled by remember { mutableStateOf(false) }
    var notificationLessonId by remember { mutableStateOf<String?>(initialLessonId) }

    // Effect để xác định initial destination
    LaunchedEffect(authState, loggedAccounts) {
        if (initialDestination == null) {
            // Đợi một chút để đảm bảo AuthRepository đã được khởi tạo
            delay(100)
            
            // Kiểm tra trạng thái đăng nhập từ AuthRepository thay vì dựa vào SplashActivity
            val isLoggedIn = authViewModel.hasValidToken()
            
            if (isLoggedIn) {
                // Logged-in user flow - go directly to courses
                initialDestination = "courses"
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
    
    // Get current activity context để observe intent changes
    val activity = LocalContext.current as? MainActivity
    
    // Listen to writing notification events (from FCM hoặc onNewIntent)
    LaunchedEffect(Unit) {
        WritingNotificationCenter.events
            .onEach { event ->
                when (event) {
                    is WritingNotificationEvent.WritingGraded -> {
                        notificationLessonId = event.lessonId
                        Timber.d("AppNavigationWithAutoLogin: Received WritingGraded event, lessonId=${event.lessonId}")
                    }
                }
            }
            .launchIn(this)
    }
    
    // Also observe initialLessonId changes (khi app được launch từ notification)
    LaunchedEffect(initialLessonId) {
        initialLessonId?.let { lessonId ->
            if (lessonId != notificationLessonId) {
                notificationLessonId = lessonId
                Timber.d("AppNavigationWithAutoLogin: Set notificationLessonId from initialLessonId=$lessonId")
            }
        }
    }

    // Render AppNavigation khi đã xác định destination
    initialDestination?.let { destination ->
        val resumeLessonForNav = if (!resumeHandled && destination == "courses") currentLesson else null
        val navLessonId = notificationLessonId

        AppNavigation(
            initialDestination = destination,
            resumeLesson = resumeLessonForNav,
            onResumeLessonConsumed = { resumeHandled = true },
            notificationLessonId = navLessonId
        )
    }

    LaunchedEffect(initialDestination) {
        val destination = initialDestination
        if (destination != null && destination != "courses") {
            resumeHandled = false
        }
    }
}

@Composable
private fun NotificationPermissionHandler() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val permission = Manifest.permission.POST_NOTIFICATIONS
    var hasRequested by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        Timber.d("Notification permission result: %s", granted)
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission && !hasRequested) {
            hasRequested = true
            launcher.launch(permission)
        }
    }
}