package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.R
import com.seoulhankuko.app.domain.model.HangulChar
import com.seoulhankuko.app.domain.model.HangulGroup
import com.seoulhankuko.app.presentation.components.ModernBottomNavigationBar
import com.seoulhankuko.app.presentation.utils.UnitColors
import com.seoulhankuko.app.presentation.viewmodel.HangulSelectionViewModel

/**
 * Hangul Alphabet Selection Screen
 * Displays grouped Hangul characters with navigation to canvas screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HangulAlphabetScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToAlphabet: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onSelectChar: (String) -> Unit = {},
    viewModel: HangulSelectionViewModel = hiltViewModel()
) {
    val hangulGroups by viewModel.hangulGroups.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bảng chữ cái Hangul",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = UnitColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = UnitColors.SoftIndigo
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = UnitColors.TextPrimary,
                    navigationIconContentColor = UnitColors.SoftIndigo
                ),
                modifier = Modifier.shadow(elevation = 4.dp, shape = RoundedCornerShape(0.dp))
            )
        },
        bottomBar = {
            ModernBottomNavigationBar(
                currentRoute = "alphabet_list",
                onNavigateToHome = onNavigateToHome,
                onNavigateToNotification = onNavigateToAlphabet,
                onNavigateToProfile = onNavigateToProfile
            )
        },
        containerColor = UnitColors.BackgroundLight
    ) { paddingValues ->
        HangulSelectionContent(
            groups = hangulGroups,
            onSelectChar = onSelectChar,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

/**
 * Content composable for Hangul selection
 * Displays groups of Hangul characters in a scrollable list
 */
@Composable
private fun HangulSelectionContent(
    groups: List<HangulGroup>,
    onSelectChar: (String) -> Unit,
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
                onSelectChar = onSelectChar
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
    onSelectChar: (String) -> Unit
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
            onSelectChar = onSelectChar
        )
    }
}

/**
 * Composable for displaying Hangul characters in a grid
 * Each character is displayed in a card with rounded corners
 */
@Composable
private fun HangulCharacterGrid(
    characters: List<HangulChar>,
    onSelectChar: (String) -> Unit
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
                    onSelectChar = onSelectChar,
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
    onSelectChar: (String) -> Unit,
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { onSelectChar(hangulChar.symbol) }
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

