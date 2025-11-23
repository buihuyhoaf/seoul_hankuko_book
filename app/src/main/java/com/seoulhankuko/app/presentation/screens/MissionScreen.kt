package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.presentation.components.BonusIndicatorCard
import com.seoulhankuko.app.presentation.components.MainScaffold
import com.seoulhankuko.app.presentation.components.MissionCard
import com.seoulhankuko.app.presentation.components.TopBarState
import com.seoulhankuko.app.presentation.utils.HomeColors
import com.seoulhankuko.app.presentation.viewmodel.MainUiViewModel
import com.seoulhankuko.app.presentation.viewmodel.MissionViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun MissionScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToAlphabet: () -> Unit,
    onNavigateToRanking: () -> Unit,
    onNavigateToMission: () -> Unit,
    onNavigateToNotification: () -> Unit,
    onNavigateToProfile: () -> Unit,
    mainUiViewModel: MainUiViewModel = hiltViewModel(),
    missionViewModel: MissionViewModel = hiltViewModel()
) {
    val userData by mainUiViewModel.userData.collectAsStateWithLifecycle()
    val missions by missionViewModel.missions.collectAsStateWithLifecycle()
    val isLoading by missionViewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by missionViewModel.isRefreshing.collectAsStateWithLifecycle()
    val bonusRemaining by missionViewModel.bonusRemainingTime.collectAsStateWithLifecycle()
    val isBonusActive by missionViewModel.isBonusActive.collectAsStateWithLifecycle()
    
    // Load missions on init
    LaunchedEffect(Unit) {
        missionViewModel.loadTodayMissions(userData.accessToken)
    }
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { missionViewModel.loadTodayMissions(userData.accessToken, isRefresh = true) }
    )

    MainScaffold(
        topBarState = TopBarState(
            userName = userData.name ?: userData.email ?: "Học viên",
            streakDays = userData.streakDays,
            exp = userData.exp,
            courseTitle = "Nhiệm vụ",
            courseThumbnailUrl = null,
            avatarUrl = userData.avatarUrl,
            isVisible = true,
            isShown = true
        ),
        currentRoute = "missions",
        onNavigateToHome = onNavigateToHome,
        onNavigateToAlphabet = onNavigateToAlphabet,
        onNavigateToRanking = onNavigateToRanking,
        onNavigateToMission = onNavigateToMission,
        onNavigateToNotification = onNavigateToNotification,
        onNavigateToProfile = onNavigateToProfile,
        onAvatarClick = onNavigateToProfile,
        containerColor = HomeColors.DuolingoLightGray
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Bonus indicator
                if (isBonusActive) {
                    BonusIndicatorCard(
                        remainingSeconds = bonusRemaining,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                // Info card about x2 exp benefit
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE3F2FD) // Light blue background
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡",
                            fontSize = 24.sp
                        )
                        Text(
                            text = "Hoàn thành nhiệm vụ để nhận x2 EXP trong 15 phút!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1565C0), // Dark blue text
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Missions list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(missions) { mission ->
                        MissionCard(
                            mission = mission,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = Color.White,
                contentColor = HomeColors.DuolingoDarkGreen
            )
        }
    }
}

