package com.seoulhankuko.app.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.presentation.components.MainScaffold
import com.seoulhankuko.app.presentation.components.SouthKoreaLoadingIcon
import com.seoulhankuko.app.presentation.components.TopBarState
import com.seoulhankuko.app.presentation.utils.HomeColors
import com.seoulhankuko.app.presentation.viewmodel.MainUiViewModel
import com.seoulhankuko.app.presentation.viewmodel.NotificationItemUi
import com.seoulhankuko.app.presentation.viewmodel.NotificationViewModel

@Composable
fun NotificationScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToAlphabet: () -> Unit,
    onNavigateToRanking: () -> Unit,
    onNavigateToMission: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    mainUiViewModel: MainUiViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val userData by mainUiViewModel.userData.collectAsStateWithLifecycle()
    val uiState by notificationViewModel.uiState.collectAsStateWithLifecycle()
    val username = userData.name?.takeIf { it.isNotBlank() } ?: userData.email ?: ""

    LaunchedEffect(username) {
        if (username.isNotBlank()) {
            notificationViewModel.initialize(username)
        }
    }

    BackHandler { onNavigateBack() }

    MainScaffold(
        topBarState = TopBarState(
            userName = userData.name ?: userData.email ?: "Học viên",
            streakDays = userData.streakDays,
            exp = userData.exp,
            courseTitle = "Thông báo",
            courseThumbnailUrl = null,
            avatarUrl = userData.avatarUrl,
            isVisible = true,
            isShown = true
        ),
        currentRoute = "notifications",
        onNavigateToHome = onNavigateToHome,
        onNavigateToAlphabet = onNavigateToAlphabet,
        onNavigateToRanking = onNavigateToRanking,
        onNavigateToMission = onNavigateToMission,
        onNavigateToNotification = {},
        onNavigateToProfile = onNavigateToProfile,
        onAvatarClick = onNavigateToProfile,
        containerColor = HomeColors.DuolingoLightGray,
        showBottomBar = true,
        modifier = modifier
    ) { innerPadding ->
        if (username.isBlank()) {
            LoginRequiredState(innerPadding)
        } else {
            NotificationScreenContent(
                uiState = uiState,
                contentPadding = innerPadding,
                onRefresh = { notificationViewModel.refresh(force = true) },
                onLoadMore = { notificationViewModel.loadNextPage() },
                onMarkAsRead = { notificationViewModel.markNotificationRead(it) },
                onMarkAllRead = { notificationViewModel.markAllRead() },
                onDismissError = { notificationViewModel.clearError() }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun NotificationScreenContent(
    uiState: com.seoulhankuko.app.presentation.viewmodel.NotificationUiState,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onMarkAsRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onDismissError: () -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = onRefresh
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .pullRefresh(pullRefreshState)
    ) {
        when {
            uiState.isLoading && uiState.items.isEmpty() -> {
                LoadingState()
            }

            uiState.errorMessage != null && uiState.items.isEmpty() -> {
                ErrorState(
                    message = uiState.errorMessage,
                    onRetry = onRefresh
                )
            }

            uiState.items.isEmpty() -> {
                EmptyNotificationsState()
            }

            else -> {
                NotificationList(
                    notifications = uiState.items,
                    unreadCount = uiState.unreadCount,
                    hasMore = uiState.hasMore,
                    isLoadingMore = uiState.isLoadingMore,
                    errorMessage = uiState.errorMessage,
                    onDismissError = onDismissError,
                    onLoadMore = onLoadMore,
                    onMarkAsRead = onMarkAsRead,
                    onMarkAllRead = onMarkAllRead
                )
            }
        }

        PullRefreshIndicator(
            refreshing = uiState.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = Color.White,
            contentColor = HomeColors.DuolingoDarkGreen
        )
    }
}

@Composable
private fun NotificationList(
    notifications: List<NotificationItemUi>,
    unreadCount: Int,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onLoadMore: () -> Unit,
    onMarkAsRead: (String) -> Unit,
    onMarkAllRead: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Column(
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Hộp thông báo của bạn",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = HomeColors.DuolingoDarkGreen
                )
                NotificationActionsRow(
                    unreadCount = unreadCount,
                    onMarkAllRead = onMarkAllRead
                )
                if (errorMessage != null) {
                    NotificationErrorBanner(
                        message = errorMessage,
                        onDismiss = onDismissError
                    )
                }
            }
        }

        itemsIndexed(notifications, key = { _, item -> item.id }) { index, item ->
            NotificationCard(
                notification = item,
                onClick = { onMarkAsRead(item.id) }
            )
            if (index == notifications.lastIndex && hasMore && !isLoadingMore) {
                LaunchedEffect(notifications.size) {
                    onLoadMore()
                }
            }
        }

        if (isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = HomeColors.DuolingoDarkGreen)
                }
            }
        }
        item(key = "bottom_spacer") { SpacerBottomPadding() }
    }
}

@Composable
private fun NotificationActionsRow(
    unreadCount: Int,
    onMarkAllRead: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (unreadCount > 0) "$unreadCount thông báo chưa đọc" else "Bạn đã đọc hết thông báo",
            style = MaterialTheme.typography.bodyMedium,
            color = HomeColors.DuolingoGray
        )
        TextButton(onClick = onMarkAllRead, enabled = unreadCount > 0) {
            Text(text = "Đánh dấu đã đọc")
        }
    }
}

@Composable
private fun NotificationErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFFFFE6E6)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Có lỗi xảy ra",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFB42318)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF842029)
                )
            }
            TextButton(onClick = onDismiss) {
                Text(text = "Ẩn", color = Color(0xFFB42318))
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationItemUi,
    onClick: () -> Unit
) {
    val backgroundColor =
        if (notification.isRead) Color.White else HomeColors.DuolingoLightGreen.copy(alpha = 0.3f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = HomeColors.DuolingoDarkGreen
                )
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = notification.displayTime,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = HomeColors.DuolingoGray
            )
        }
    }
}

@Composable
private fun EmptyNotificationsState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Bạn chưa có thông báo mới",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = HomeColors.DuolingoDarkGreen
            )
            Text(
                text = "Chăm chỉ học tập để nhận được nhiều cập nhật thú vị nhé!",
                style = MaterialTheme.typography.bodyMedium,
                color = HomeColors.DuolingoGray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SpacerBottomPadding() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    )
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        SouthKoreaLoadingIcon(size = 64.dp)
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Không thể tải thông báo",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = HomeColors.DuolingoDarkGreen
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HomeColors.DuolingoGray
                )
                TextButton(onClick = onRetry) {
                    Text(text = "Thử lại")
                }
            }
        }
    }
}

@Composable
private fun LoginRequiredState(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Đăng nhập để xem thông báo",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = HomeColors.DuolingoDarkGreen
                )
                Text(
                    text = "Vui lòng đăng nhập để đồng bộ và nhận thông báo cá nhân hoá.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HomeColors.DuolingoGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

