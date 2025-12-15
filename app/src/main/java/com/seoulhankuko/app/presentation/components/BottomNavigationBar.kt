package com.seoulhankuko.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.seoulhankuko.app.R
import com.seoulhankuko.app.presentation.utils.HomeColors

@Composable
fun ModernBottomNavigationBar(
    currentRoute: String,
    onNavigateToHome: () -> Unit,
    onNavigateToAlphabet: () -> Unit,
    onNavigateToRanking: () -> Unit,
    onNavigateToMission: () -> Unit,
    onNavigateToNotification: () -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.alphabet_korean),
                    contentDescription = "Alphabet"
                )
            },
            label = { Text("Chữ cái") },
            selected = currentRoute == "alphabet_list",
            onClick = onNavigateToAlphabet,
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
                    painter = painterResource(id = R.drawable.regular_ranking_star),
                    contentDescription = "Ranking",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = { Text("Xếp hạng") },
            selected = currentRoute == "ranking",
            onClick = onNavigateToRanking,
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
                    painter = painterResource(id = R.drawable.scroll_text),
                    contentDescription = "Missions",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = { Text("Nhiệm vụ") },
            selected = currentRoute == "missions",
            onClick = onNavigateToMission,
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
                    painter = painterResource(id = R.drawable.notifications_material),
                    contentDescription = "Notifications",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = { Text("Thông báo") },
            selected = currentRoute == "notifications",
            onClick = onNavigateToNotification,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = HomeColors.DuolingoGreen,
                selectedTextColor = HomeColors.DuolingoGreen,
                unselectedIconColor = HomeColors.DuolingoGray,
                unselectedTextColor = HomeColors.DuolingoGray
            )
        )
    }
}
