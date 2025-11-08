package com.seoulhankuko.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.seoulhankuko.app.R
import com.seoulhankuko.app.presentation.utils.HomeColors
import com.seoulhankuko.app.presentation.utils.BottomNavColors

enum class BottomNavigationRoute {
    HOME, NOTIFICATION, PROFILE
}

@Composable
private fun getIconForRoute(route: BottomNavigationRoute): ImageVector {
    return when (route) {
        BottomNavigationRoute.HOME -> Icons.Default.Home
        BottomNavigationRoute.NOTIFICATION -> Icons.Default.Notifications
        BottomNavigationRoute.PROFILE -> Icons.Default.Person
    }
}

@Composable
private fun getContentDescriptionForRoute(route: BottomNavigationRoute): String {
    return when (route) {
        BottomNavigationRoute.HOME -> "Home"
        BottomNavigationRoute.NOTIFICATION -> "Notification"
        BottomNavigationRoute.PROFILE -> "Profile"
    }
}

@Composable
fun BottomNavigationBar(
    currentRoute: BottomNavigationRoute,
    onNavigateToHome: () -> Unit,
    onNavigateToNotification: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BottomNavColors.ContainerGreen
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavigationItem(
                icon = getIconForRoute(BottomNavigationRoute.HOME),
                isSelected = currentRoute == BottomNavigationRoute.HOME,
                onClick = onNavigateToHome,
                contentDescription = getContentDescriptionForRoute(BottomNavigationRoute.HOME)
            )
            
            BottomNavigationItem(
                icon = getIconForRoute(BottomNavigationRoute.NOTIFICATION),
                isSelected = currentRoute == BottomNavigationRoute.NOTIFICATION,
                onClick = onNavigateToNotification,
                contentDescription = getContentDescriptionForRoute(BottomNavigationRoute.NOTIFICATION)
            )
            
            BottomNavigationItem(
                icon = getIconForRoute(BottomNavigationRoute.PROFILE),
                isSelected = currentRoute == BottomNavigationRoute.PROFILE,
                onClick = onNavigateToProfile,
                contentDescription = getContentDescriptionForRoute(BottomNavigationRoute.PROFILE)
            )
        }
    }
}

@Composable
fun ModernBottomNavigationBar(
    currentRoute: String,
    onNavigateToHome: () -> Unit,
    onNavigateToNotification: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.home_tabler_outline),
                    contentDescription = "Home"
                )
            },
            label = { Text("Trang chủ") },
            selected = currentRoute == "home" || currentRoute == "courses",
            onClick = onNavigateToHome,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = HomeColors.DuolingoGreen,
                selectedTextColor = HomeColors.DuolingoGreen,
                unselectedIconColor = HomeColors.DuolingoGray,
                unselectedTextColor = HomeColors.DuolingoGray
            )
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.alphabet_korean),
                    contentDescription = "Alphabet"
                )
            },
            label = { Text("Bảng chữ cái") },
            selected = currentRoute == "alphabet_list",
            onClick = onNavigateToNotification,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = HomeColors.DuolingoGreen,
                selectedTextColor = HomeColors.DuolingoGreen,
                unselectedIconColor = HomeColors.DuolingoGray,
                unselectedTextColor = HomeColors.DuolingoGray
            )
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.profile_icomoon),
                    contentDescription = "Profile"
                )
            },
            label = { Text("Hồ sơ") },
            selected = currentRoute == "profile",
            onClick = onNavigateToProfile,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = HomeColors.DuolingoGreen,
                selectedTextColor = HomeColors.DuolingoGreen,
                unselectedIconColor = HomeColors.DuolingoGray,
                unselectedTextColor = HomeColors.DuolingoGray
            )
        )
    }
}

/**
 * Main Screen Wrapper with Bottom Navigation
 * Wraps main navigation screens (Home, Notification, Profile) with bottom navigation bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWrapper(
    currentRoute: String,
    onNavigateToHome: () -> Unit,
    onNavigateToNotification: () -> Unit,
    onNavigateToProfile: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = {
            ModernBottomNavigationBar(
                currentRoute = currentRoute,
                onNavigateToHome = onNavigateToHome,
                onNavigateToNotification = onNavigateToNotification,
                onNavigateToProfile = onNavigateToProfile
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        content(paddingValues)
    }
}
