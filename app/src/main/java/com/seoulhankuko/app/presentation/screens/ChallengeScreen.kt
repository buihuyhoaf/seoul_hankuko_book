package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seoulhankuko.app.presentation.components.MainScreenWrapper

@Composable
fun ChallengeScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToChallenge: () -> Unit = {},
    onNavigateToNotification: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    MainScreenWrapper(
        currentRoute = "challenge",
        onNavigateToHome = onNavigateToHome,
        onNavigateToChallenge = onNavigateToChallenge,
        onNavigateToNotification = onNavigateToNotification,
        onNavigateToProfile = onNavigateToProfile
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF81C784)) // Nền xanh lá cây nhạt
                .padding(16.dp)
        ) {
            Text(
                text = "🏆 Challenge",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20), // Xanh lá đậm cho title
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(listOf(
                    Triple("Daily Challenge", "Complete 5 lessons", "🔥 2h left"),
                    Triple("Weekly Challenge", "Learn 20 new words", "⭐ 5d left"),
                    Triple("Monthly Challenge", "Perfect streak for 30 days", "💎 20d left")
                )) { (title, description, time) ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = time,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }
        }
    }
}
