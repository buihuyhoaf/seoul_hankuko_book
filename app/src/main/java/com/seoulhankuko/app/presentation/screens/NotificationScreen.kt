package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seoulhankuko.app.presentation.components.MainScreenWrapper
import com.seoulhankuko.app.presentation.utils.NotificationColors

@Composable
fun NotificationScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToNotification: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    MainScreenWrapper(
        currentRoute = "notification",
        onNavigateToHome = onNavigateToHome,
        onNavigateToNotification = onNavigateToNotification,
        onNavigateToProfile = onNavigateToProfile
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(NotificationColors.BackgroundGreen)
                .padding(16.dp)
        ) {
            Text(
                text = "🔔 Notification",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = NotificationColors.TitleGreen,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listOf(
                    Triple("Course Update", "New lessons are available in Course 1", "2h ago"),
                    Triple("Achievement", "You completed 10 lessons in a row!", "1d ago"),
                    Triple("Reminder", "Don't forget to practice today", "2d ago"),
                    Triple("Challenge", "New weekly challenge is starting", "3d ago")
                )) { (title, content, time) ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NotificationColors.TitleGreen
                                    )
                                    Text(
                                        text = content,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text(
                                    text = time,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
