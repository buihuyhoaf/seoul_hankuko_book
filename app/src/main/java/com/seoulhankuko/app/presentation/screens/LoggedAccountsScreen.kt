package com.seoulhankuko.app.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.seoulhankuko.app.R
import com.seoulhankuko.app.domain.model.LoggedAccount
import com.seoulhankuko.app.presentation.components.SouthKoreaLoadingIcon
import com.seoulhankuko.app.presentation.ui.theme.WhiteBackground
import com.seoulhankuko.app.presentation.utils.LoginColors
import com.seoulhankuko.app.presentation.viewmodel.AuthViewModel
import com.seoulhankuko.app.presentation.viewmodel.AutoLoginState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen displaying list of previously logged-in accounts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggedAccountsScreen(
    onAccountSelected: (LoggedAccount) -> Unit,
    onSuccessfulAutoLogin: () -> Unit,
    onAddAccountClick: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val loggedAccounts by viewModel.loggedAccounts.collectAsStateWithLifecycle()
    val autoLoginState by viewModel.autoLoginState.collectAsStateWithLifecycle()
    var showErrorDialog by remember { mutableStateOf<LoggedAccount?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<LoggedAccount?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var currentAutoLoginAccount by remember { mutableStateOf<LoggedAccount?>(null) }
    var isNavigatingToLogin by remember { mutableStateOf(false) }

    // Animation states
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
        // Reset navigation flag when screen is first displayed
        isNavigatingToLogin = false
    }

    // Cancel any pending auto-login when navigating to login screen
    LaunchedEffect(isNavigatingToLogin) {
        if (isNavigatingToLogin) {
            // Immediately cancel any pending auto-login operations
            currentAutoLoginAccount = null
            isLoading = false
            viewModel.clearAutoLoginState()
        }
    }

    // Handle auto-login state changes
    LaunchedEffect(autoLoginState) {
        // Don't handle auto-login if we're navigating away
        if (isNavigatingToLogin) {
            // If navigating to login, clear any pending state and return
            isLoading = false
            viewModel.clearAutoLoginState()
            return@LaunchedEffect
        }
        
        when (autoLoginState) {
            is AutoLoginState.Loading -> {
                if (!isNavigatingToLogin) {
                    isLoading = true
                }
            }
            is AutoLoginState.Success -> {
                isLoading = false
                viewModel.clearAutoLoginState()
                // Only navigate if not already navigating to login
                if (!isNavigatingToLogin) {
                    onSuccessfulAutoLogin()
                }
            }
            is AutoLoginState.Error -> {
                isLoading = false
                // Only show error dialog if not navigating away
                if (!isNavigatingToLogin) {
                    showErrorDialog = currentAutoLoginAccount
                    currentAutoLoginAccount = null
                }
                viewModel.clearAutoLoginState()
            }
            is AutoLoginState.Idle -> {
                isLoading = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = WhiteBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.login_screen_padding)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.login_screen_large_spacing)))

            // Header with optional app logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
                modifier = Modifier.fillMaxWidth()
            ) {
                // App icon/logo (you can replace with your app's icon)
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(72.dp),
                    tint = LoginColors.PrimaryButton
                )

                // Title
                Text(
                    text = stringResource(R.string.logged_accounts_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = LoginColors.TitleGreen,
                    textAlign = TextAlign.Center
                )

                // Subtitle
                Text(
                    text = stringResource(R.string.logged_accounts_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LoginColors.SubtitleGreen,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.login_screen_large_spacing)))

            // Accounts list
            if (loggedAccounts.isEmpty()) {
                // Empty state with animation
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(800, delayMillis = 400))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBox,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = LoginColors.SubtitleGreen
                            )
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                            Text(
                                text = stringResource(R.string.logged_accounts_empty_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = LoginColors.TitleGreen,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
                            Text(
                                text = stringResource(R.string.logged_accounts_empty_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = LoginColors.SubtitleGreen,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(
                        items = loggedAccounts,
                        key = { _, account -> account.userId }
                    ) { index, account ->
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(animationSpec = tween(600)) +
                                    slideInVertically(
                                        initialOffsetY = { it },
                                        animationSpec = tween(600, delayMillis = (index * 100))
                                    )
                        ) {
                            LoggedAccountItem(
                                account = account,
                                onClick = {
                                    isNavigatingToLogin = false
                                    currentAutoLoginAccount = account
                                    viewModel.tryAutoLogin(account)
                                },
                                onLongClick = {
                                    showDeleteConfirmation = account
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.login_screen_spacing)))

            // Add Account Button with animation
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 600)) +
                        scaleIn(
                            initialScale = 0.9f,
                            animationSpec = tween(800, delayMillis = 600)
                        )
            ) {
                Button(
                    onClick = {
                        // CRITICAL: Set flag FIRST to prevent any auto-login from completing
                        isNavigatingToLogin = true
                        // Clear any pending auto-login state and reset current account
                        currentAutoLoginAccount = null
                        isLoading = false
                        viewModel.clearAutoLoginState()
                        // Navigate to login screen immediately - this must happen synchronously
                        onAddAccountClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.button_height)),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoginColors.PrimaryButton,
                        contentColor = Color.White,
                        disabledContainerColor = LoginColors.Disabled,
                        disabledContentColor = LoginColors.DisabledText
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.logged_accounts_add_account),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xlarge)))
        }

        // Note: No back button since LoggedAccountsScreen is an entry point
        // when user has saved accounts but no valid session

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SouthKoreaLoadingIcon(size = 32.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Signing in...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        }
    }

    // Error dialog with improved styling
    showErrorDialog?.let { failedAccount ->
        AlertDialog(
            onDismissRequest = { showErrorDialog = null },
            title = {
                Text(
                    text = "Session Expired",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Your session has expired. Would you like to:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Sign in manually with your password\n• Remove this account from the list",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showErrorDialog = null
                        onAccountSelected(failedAccount)
                    }
                ) {
                    Text("Sign In")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showErrorDialog = null
                            viewModel.deleteAccount(failedAccount.email)
                        }
                    ) {
                        Text("Remove Account")
                    }
                    TextButton(onClick = { showErrorDialog = null }) {
                        Text("Cancel")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Delete confirmation dialog
    showDeleteConfirmation?.let { accountToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = {
                Text(
                    text = "Remove Account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to remove this account?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${accountToDelete.displayName}\n${accountToDelete.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = null
                        viewModel.deleteAccount(accountToDelete.email)
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoggedAccountItem(
    account: LoggedAccount,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with enhanced styling
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = account.photoUrl,
                    contentDescription = "Account avatar",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            SouthKoreaLoadingIcon(size = 24.dp)
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default avatar",
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Account info with enhanced typography
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Display name or email as primary text
                val displayName = account.displayName.ifBlank { account.email }
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Show email if display name is available, otherwise show "Last active" info
                if (account.displayName.isNotBlank()) {
                    Text(
                        text = account.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "Last active: ${formatLastLoginTime(account.lastLogin)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            // Chevron indicator
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Select account",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun formatLastLoginTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} minutes ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
        diff < 48 * 60 * 60 * 1000 -> "yesterday"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} days ago"
        else -> {
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
    }
}
