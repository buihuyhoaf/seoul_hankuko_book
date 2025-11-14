package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.R
import com.seoulhankuko.app.data.api.model.LeaderboardEntry
import com.seoulhankuko.app.presentation.components.MainScaffold
import com.seoulhankuko.app.presentation.components.TopBarState
import com.seoulhankuko.app.presentation.utils.HomeColors
import com.seoulhankuko.app.presentation.viewmodel.MainUiViewModel
import com.seoulhankuko.app.presentation.viewmodel.WeeklyLeaderboardViewModel

@Composable
fun LeaderboardScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToAlphabet: () -> Unit,
    onNavigateToRanking: () -> Unit,
    onNavigateToMission: () -> Unit,
    onNavigateToNotification: () -> Unit,
    onNavigateToProfile: () -> Unit,
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
    
    MainScaffold(
        topBarState = TopBarState(
            userName = userData.name ?: userData.email ?: "Học viên",
            streakDays = userData.streakDays,
            exp = userData.exp,
            courseTitle = "🏆 Xếp hạng tuần",
            courseThumbnailUrl = null,
            avatarUrl = userData.avatarUrl,
            isVisible = true,
            isShown = true
        ),
        currentRoute = "ranking",
        onNavigateToHome = onNavigateToHome,
        onNavigateToAlphabet = onNavigateToAlphabet,
        onNavigateToRanking = onNavigateToRanking,
        onNavigateToMission = onNavigateToMission,
        onNavigateToNotification = onNavigateToNotification,
        onNavigateToProfile = onNavigateToProfile,
        onAvatarClick = onNavigateToProfile,
        containerColor = HomeColors.DuolingoLightGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else if (error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Lỗi: $error",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                leaderboard?.let { data ->
                    // Find index of current user's entry
                    val currentUserIndex = remember(data.entries) {
                        data.entries.indexOfFirst { it.isCurrentUser }
                    }
                    
                    // LazyListState for scrolling
                    val listState = rememberLazyListState()
                    
                    // Auto-scroll to current user's entry when data is loaded
                    LaunchedEffect(data.entries) {
                        if (currentUserIndex >= 0) {
                            // Scroll to current user's position with some offset
                            listState.animateScrollToItem(
                                index = currentUserIndex,
                                scrollOffset = -100 // Offset to show more context above
                            )
                        }
                    }
                    
                    // Week information
                    val weekInfo = remember(data.weekStart) {
                        try {
                            val weekStartDate = LocalDate.parse(data.weekStart)
                            val weekEndDate = weekStartDate.plusDays(6)
                            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale("vi", "VN"))
                            val weekStartFormatted = weekStartDate.format(dateFormatter)
                            val weekEndFormatted = weekEndDate.format(dateFormatter)
                            Pair(weekStartFormatted, weekEndFormatted)
                        } catch (e: Exception) {
                            Pair("", "")
                        }
                    }
                    
                    // Encouragement messages
                    val encouragementMessages = listOf(
                        "Cố gắng phát huy! 💪",
                        "Tiếp tục nỗ lực! ⭐",
                        "Bạn đang làm rất tốt! 🌟",
                        "Hãy giữ vững phong độ! 🚀",
                        "Mỗi ngày là một cơ hội mới! ✨"
                    )
                    val randomEncouragement = remember(data.weekStart) {
                        encouragementMessages.random()
                    }
                    
                    // Week range text
                    if (weekInfo.first.isNotEmpty() && weekInfo.second.isNotEmpty()) {
                        Text(
                            text = "Xếp hạng tuần từ ${weekInfo.first} đến ${weekInfo.second}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    // Encouragement text
                    Text(
                        text = randomEncouragement,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = HomeColors.DuolingoDarkGreen,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
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
                        text = "Top 20",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn(
                        state = listState,
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
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        text = "Xếp hạng của bạn:",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF374151)
                    )
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "${xp} XP",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
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
            containerColor = Color.White // Same for all items
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp // Same for all items
        ),
        border = if (isHighlighted) 
            BorderStroke(3.dp, HomeColors.DuolingoGreen) // Only border for user's item
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                text = getRankEmoji(entry.rank) ?: "#${entry.rank}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827), // Same for all items
                modifier = Modifier.width(50.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Streak Days
            entry.streakDays?.let { streak ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.width(60.dp)
                ) {
                    Text(
                        text = "🔥",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$streak",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF374151) // Same for all items
                    )
                }
            } ?: Spacer(modifier = Modifier.width(60.dp))
            
            // Avatar + Name (together with weight = 1) - no spacing before this
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar (circular with hanbok image)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.hanbok),
                        contentDescription = "Ảnh đại diện mặc định",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Name
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, // Same for all items
                    color = Color(0xFF374151) // Same for all items
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // XP (no weight, fixed width)
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.widthIn(min = 80.dp)
            ) {
                Text(
                    text = "${entry.xp} XP",
                    style = MaterialTheme.typography.titleMedium,
                    color = HomeColors.DuolingoDarkGreen, // Same for all items
                    fontWeight = FontWeight.Bold
                )
                if (isHighlighted && entry.rankChange != null && entry.rankChange != 0) {
                    Text(
                        text = if (entry.rankChange > 0) "+${entry.rankChange}" else "${entry.rankChange}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (entry.rankChange > 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Bold
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
