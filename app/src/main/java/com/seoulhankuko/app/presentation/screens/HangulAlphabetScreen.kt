package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.R
import com.seoulhankuko.app.domain.model.HangulChar
import com.seoulhankuko.app.domain.model.HangulGroup
import com.seoulhankuko.app.presentation.components.MainScaffold
import com.seoulhankuko.app.presentation.components.TopBarState
import com.seoulhankuko.app.presentation.utils.UnitColors
import com.seoulhankuko.app.presentation.viewmodel.HangulSelectionViewModel
import com.seoulhankuko.app.presentation.viewmodel.MainUiViewModel

/**
 * Hangul Alphabet Selection Screen
 * Displays grouped Hangul characters with navigation to canvas screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HangulAlphabetScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToAlphabet: () -> Unit = {},
    onNavigateToRanking: () -> Unit = {},
    onNavigateToMission: () -> Unit = {},
    onNavigateToNotification: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToPractice: () -> Unit = {},
    viewModel: HangulSelectionViewModel = hiltViewModel(),
    mainUiViewModel: MainUiViewModel = hiltViewModel()
) {
    val hangulGroups by viewModel.hangulGroups.collectAsStateWithLifecycle()
    val userData by mainUiViewModel.userData.collectAsStateWithLifecycle()

    MainScaffold(
        topBarState = TopBarState(
            userName = userData.name ?: userData.email ?: "Học viên",
            streakDays = userData.streakDays,
            exp = userData.exp,
            courseTitle = "Bảng chữ cái Hangul",
            courseThumbnailUrl = null,
            avatarUrl = userData.avatarUrl,
            isVisible = true,
            isShown = true
        ),
        currentRoute = "alphabet_list",
        onNavigateToHome = onNavigateToHome,
        onNavigateToAlphabet = onNavigateToAlphabet,
        onNavigateToRanking = onNavigateToRanking,
        onNavigateToMission = onNavigateToMission,
        onNavigateToNotification = onNavigateToNotification,
        onNavigateToProfile = onNavigateToProfile,
        onAvatarClick = onNavigateToProfile,
        containerColor = UnitColors.BackgroundLight,
        showBottomBar = true
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Button(
                onClick = onNavigateToPractice,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(text = "Luyện viết")
            }

            Spacer(modifier = Modifier.height(8.dp))

            HangulSelectionContent(
                groups = hangulGroups,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Content composable for Hangul selection
 * Displays groups of Hangul characters in a scrollable list
 */
@Composable
private fun HangulSelectionContent(
    groups: List<HangulGroup>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(
            items = groups,
            key = { it.title }
        ) { group ->
            HangulGroupSection(
                group = group
            )
        }
    }
}

/**
 * Composable for a single Hangul group section
 * Shows group title and list of characters in cards
 */
@Composable
private fun HangulGroupSection(
    group: HangulGroup
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Group title
        Text(
            text = group.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = UnitColors.TextPrimary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Character grid
        HangulCharacterGrid(
            characters = group.items
        )
    }
}

/**
 * Composable for displaying Hangul characters in a grid
 * Each character is displayed in a card with rounded corners
 */
@Composable
private fun HangulCharacterGrid(
    characters: List<HangulChar>
) {
    val columns = 6 // 6 characters per row

    characters.chunked(columns).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row.forEach { hangulChar ->
                HangulCharacterCard(
                    hangulChar = hangulChar,
                    modifier = Modifier.weight(1f)
                )
            }
            // Fill remaining space if row is incomplete
            repeat(columns - row.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Composable for a single Hangul character card
 * Clickable card with rounded corners showing symbol and romanization
 */
@Composable
private fun HangulCharacterCard(
    hangulChar: HangulChar,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Hangul symbol
            Text(
                text = hangulChar.symbol,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = UnitColors.SoftIndigo,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Romanization
            Text(
                text = hangulChar.romanization,
                style = MaterialTheme.typography.bodySmall,
                color = UnitColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

