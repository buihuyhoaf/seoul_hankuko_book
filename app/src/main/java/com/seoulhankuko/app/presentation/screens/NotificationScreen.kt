package com.seoulhankuko.app.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.seoulhankuko.app.presentation.components.TopBarState
import com.seoulhankuko.app.presentation.utils.HomeColors
import com.seoulhankuko.app.presentation.viewmodel.MainUiViewModel

private data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean
)

@Composable
fun NotificationScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToAlphabet: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    mainUiViewModel: MainUiViewModel = hiltViewModel()
) {
    val userData by mainUiViewModel.userData.collectAsStateWithLifecycle()
    val notificationItems = remember {
        listOf(
            NotificationItem(
                id = "1",
                title = "Hoàn thành mục tiêu ngày",
                message = "Bạn đã hoàn thành 3/3 bài học hôm nay. Giữ vững phong độ nhé!",
                timestamp = "Vừa xong",
                isRead = false
            ),
            NotificationItem(
                id = "2",
                title = "Chuỗi học 5 ngày",
                message = "Chúc mừng! Bạn đã duy trì chuỗi học 5 ngày liên tiếp.",
                timestamp = "2 giờ trước",
                isRead = true
            )
        )
    }
    val unreadCount = notificationItems.count { !it.isRead }

    BackHandler { onNavigateBack() }

    MainScaffold(
        topBarState = TopBarState(
            userName = userData.name ?: userData.email ?: "Học viên",
            streakDays = userData.streakDays,
            exp = userData.exp,
            courseTitle = "Thông báo",
            courseThumbnailUrl = null,
            isVisible = true,
            isShown = true,
            notificationCount = unreadCount
        ),
        currentRoute = "notifications",
        onNavigateToHome = onNavigateToHome,
        onNavigateToNotification = {},
        onNavigateToAlphabet = onNavigateToAlphabet,
        onNavigateToProfile = onNavigateToProfile,
        onAvatarClick = onNavigateToProfile,
        containerColor = HomeColors.DuolingoLightGray,
        showBottomBar = true,
        showBackButton = true,
        onBackClick = onNavigateBack,
        modifier = modifier
    ) { innerPadding ->
        if (notificationItems.isEmpty()) {
            EmptyNotificationsState(innerPadding)
        } else {
            NotificationList(
                notifications = notificationItems,
                contentPadding = innerPadding
            )
        }
    }
}

@Composable
private fun NotificationList(
    notifications: List<NotificationItem>,
    contentPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Hộp thông báo của bạn",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = HomeColors.DuolingoDarkGreen,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )
        }

        items(notifications, key = NotificationItem::id) { item ->
            NotificationCard(item)
        }
        item { SpacerBottomPadding() }
    }
}

@Composable
private fun NotificationCard(notification: NotificationItem) {
    val backgroundColor = if (notification.isRead) Color.White else HomeColors.DuolingoLightGreen.copy(alpha = 0.3f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
            Text(
                text = notification.timestamp,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = HomeColors.DuolingoGray
            )
        }
    }
}

@Composable
private fun EmptyNotificationsState(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
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

