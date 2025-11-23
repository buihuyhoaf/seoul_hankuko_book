package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.R
import com.seoulhankuko.app.core.model.ModelDownloadProgress
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
    onCharacterClick: (String) -> Unit = {},
    viewModel: HangulSelectionViewModel = hiltViewModel(),
    mainUiViewModel: MainUiViewModel = hiltViewModel()
) {
    val hangulGroups by viewModel.hangulGroups.collectAsStateWithLifecycle()
    val userData by mainUiViewModel.userData.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()

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
            // Show download progress if downloading
            when (val progress = downloadProgress) {
                is ModelDownloadProgress.Downloading -> {
                    ModelDownloadProgressCard(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                is ModelDownloadProgress.Loading -> {
                    ModelLoadingCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                is ModelDownloadProgress.Error -> {
                    ModelErrorCard(
                        error = progress.message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                else -> {}
            }
            
            Button(
                onClick = onNavigateToPractice,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                enabled = downloadProgress !is ModelDownloadProgress.Downloading
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_writing_practice),
                    contentDescription = "Luyện viết",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HangulSelectionContent(
                groups = hangulGroups,
                onCharacterClick = onCharacterClick,
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
    onCharacterClick: (String) -> Unit,
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
                group = group,
                onCharacterClick = onCharacterClick
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
    group: HangulGroup,
    onCharacterClick: (String) -> Unit
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
            characters = group.items,
            onCharacterClick = onCharacterClick
        )
    }
}

/**
 * Progress Card Component showing download progress
 */
@Composable
private fun ModelDownloadProgressCard(
    progress: ModelDownloadProgress.Downloading,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📥 Đang tải mô hình nhận diện...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = UnitColors.TextPrimary
                )
                Text(
                    text = "${progress.progressPercent}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = UnitColors.SoftIndigo
                )
            }
            
            LinearProgressIndicator(
                progress = { progress.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = UnitColors.SoftIndigo,
                trackColor = UnitColors.SoftIndigo.copy(alpha = 0.2f)
            )
            
            Text(
                text = formatBytes(progress.bytesDownloaded) + " / " + formatBytes(progress.totalBytes),
                style = MaterialTheme.typography.bodySmall,
                color = UnitColors.TextSecondary
            )
        }
    }
}

/**
 * Loading Card Component showing model is being loaded into memory
 */
@Composable
private fun ModelLoadingCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = UnitColors.SoftIndigo,
                strokeWidth = 2.dp
            )
            Text(
                text = "Đang tải mô hình vào bộ nhớ...",
                style = MaterialTheme.typography.bodyMedium,
                color = UnitColors.TextPrimary
            )
        }
    }
}

/**
 * Error Card Component showing download error
 */
@Composable
private fun ModelErrorCard(
    error: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.titleLarge
            )
            Column {
                Text(
                    text = "Lỗi tải mô hình",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD32F2F)
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = UnitColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Helper function to format bytes to human-readable string
 */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

/**
 * Composable for displaying Hangul characters in a grid
 * Each character is displayed in a card with rounded corners
 */
@Composable
private fun HangulCharacterGrid(
    characters: List<HangulChar>,
    onCharacterClick: (String) -> Unit
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
                    onClick = { onCharacterClick(hangulChar.symbol) },
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
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxWidth(),
        onClick = onClick,
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

