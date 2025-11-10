package com.seoulhankuko.app.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.PaddingValues

/**
 * Encapsulates the shared top and bottom bars used across the app.
 */
data class TopBarState(
    val userName: String,
    val streakDays: Int,
    val exp: Int,
    val courseTitle: String? = null,
    val courseThumbnailUrl: String? = null,
    val isVisible: Boolean = true,
    val isShown: Boolean = true,
    val notificationCount: Int = 0
)

@Composable
fun MainScaffold(
    topBarState: TopBarState,
    currentRoute: String,
    onNavigateToHome: () -> Unit,
    onNavigateToNotification: () -> Unit,
    onNavigateToAlphabet: () -> Unit = onNavigateToNotification,
    onNavigateToProfile: () -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    showBottomBar: Boolean = true,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = containerColor,
        topBar = {
            if (topBarState.isShown) {
                AppBarSection(
                    userName = topBarState.userName,
                    onAvatarClick = onAvatarClick,
                    streakDays = topBarState.streakDays,
                    exp = topBarState.exp,
                    courseTitle = topBarState.courseTitle,
                    courseThumbnailUrl = topBarState.courseThumbnailUrl,
                    isVisible = topBarState.isVisible,
                    notificationCount = topBarState.notificationCount,
                    showBackButton = showBackButton,
                    onBackClick = onBackClick,
                    onNotificationClick = onNavigateToNotification
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                ModernBottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToNotification = onNavigateToAlphabet,
                    onNavigateToProfile = onNavigateToProfile
                )
            }
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}
