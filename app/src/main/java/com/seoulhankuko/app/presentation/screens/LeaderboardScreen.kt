package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.data.api.model.LeaderboardEntry
import com.seoulhankuko.app.presentation.viewmodel.MainUiViewModel
import com.seoulhankuko.app.presentation.viewmodel.WeeklyLeaderboardViewModel

@Composable
fun LeaderboardScreen(
    onNavigateBack: () -> Unit,
    mainUiViewModel: MainUiViewModel = hiltViewModel(),
    weeklyLeaderboardViewModel: WeeklyLeaderboardViewModel = hiltViewModel()
) {
    val userData by mainUiViewModel.userData.collectAsStateWithLifecycle()
    val leaderboard by weeklyLeaderboardViewModel.leaderboard.collectAsStateWithLifecycle()
    val isLoading by weeklyLeaderboardViewModel.isLoading.collectAsStateWithLifecycle()
    val error by weeklyLeaderboardViewModel.error.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        weeklyLeaderboardViewModel.loadLeaderboard(userData.accessToken)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onNavigateBack) {
                Text("← Back")
            }
            
            Text(
                text = "🏆 Weekly Leaderboard",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(80.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(32.dp)
            )
        } else if (error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            leaderboard?.let { data ->
                // Current user rank card
                if (data.currentUserRank != null) {
                    CurrentUserRankCard(
                        rank = data.currentUserRank,
                        xp = data.currentUserXp ?: 0,
                        rankChange = data.rankChange
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Leaderboard list
                Text(
                    text = "Top 20:",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(data.entries) { entry ->
                        LeaderboardEntryCard(
                            entry = entry,
                            isHighlighted = entry.isCurrentUser
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentUserRankCard(
    rank: Int,
    xp: Int,
    rankChange: Int?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your Rank:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${xp} XP",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Rank change indicator
                rankChange?.let { change ->
                    RankChangeIndicator(change = change)
                }
            }
        }
    }
}

@Composable
fun RankChangeIndicator(change: Int) {
    val (icon, color, text) = when {
        change > 0 -> Triple("↑", Color(0xFF4CAF50), "+$change")  // Green
        change < 0 -> Triple("↓", Color(0xFFF44336), "$change")    // Red
        else -> Triple("→", Color(0xFF757575), "0")                // Gray
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleLarge,
            color = color
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LeaderboardEntryCard(
    entry: LeaderboardEntry,
    isHighlighted: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isHighlighted) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                text = getRankEmoji(entry.rank) ?: "#${entry.rank}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // Avatar + Name
            Row(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar (placeholder)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Column {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                    )
                    entry.country?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // XP
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.xp} XP",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (isHighlighted && entry.rankChange != null && entry.rankChange != 0) {
                    Text(
                        text = if (entry.rankChange > 0) "+${entry.rankChange}" else "${entry.rankChange}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (entry.rankChange > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

fun getRankEmoji(rank: Int): String? {
    return when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> null
    }
}
