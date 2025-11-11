package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.presentation.components.MainScaffold
import com.seoulhankuko.app.presentation.components.TopBarState
import com.seoulhankuko.app.presentation.utils.HomeColors
import com.seoulhankuko.app.presentation.viewmodel.MainUiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToAlphabet: () -> Unit,
    onNavigateToRanking: () -> Unit,
    onNavigateToMission: () -> Unit,
    onNavigateToNotification: () -> Unit,
    onNavigateToProfile: () -> Unit,
    mainUiViewModel: MainUiViewModel = hiltViewModel()
) {
    val userData by mainUiViewModel.userData.collectAsStateWithLifecycle()

    MainScaffold(
        topBarState = TopBarState(
            userName = userData.name ?: userData.email ?: "Học viên",
            streakDays = userData.streakDays,
            exp = userData.exp,
            courseTitle = "Xếp hạng",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

