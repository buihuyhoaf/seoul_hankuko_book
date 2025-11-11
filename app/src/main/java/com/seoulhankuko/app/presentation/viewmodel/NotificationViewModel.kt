package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.api.model.NotificationResponse
import com.seoulhankuko.app.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private const val PAGE_SIZE = 20

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private var currentUsername: String? = null
    private var isRefreshingUnread: Boolean = false

    fun initialize(username: String) {
        if (username.isBlank()) return
        if (currentUsername == username && _uiState.value.initialized) return
        currentUsername = username
        refresh(force = true)
    }

    fun refresh(force: Boolean = false) {
        val username = currentUsername ?: return
        if (_uiState.value.isLoading || _uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = if (force || it.items.isEmpty()) true else it.isLoading,
                    isRefreshing = it.items.isNotEmpty(),
                    errorMessage = null
                )
            }
            val notificationsResult = notificationRepository.getNotifications(
                username = username,
                page = 1,
                itemsPerPage = PAGE_SIZE
            )
            notificationsResult.onSuccess { response ->
                val mappedItems = response.data.orEmpty().map(::mapNotification)
                val totalPages = if (response.pages <= 0) 1 else response.pages
                _uiState.update {
                    it.copy(
                        items = mappedItems,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = null,
                        currentPage = response.page,
                        totalPages = totalPages,
                        initialized = true
                    )
                }
                updateUnreadCount(username)
            }.onFailure { error ->
                Timber.e(error, "Failed to refresh notifications")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = error.message ?: "Không thể tải danh sách thông báo",
                        initialized = true
                    )
                }
            }
        }
    }

    fun loadNextPage() {
        val username = currentUsername ?: return
        val state = _uiState.value
        if (!state.hasMore || state.isLoadingMore || state.isLoading || state.isRefreshing) return
        val nextPage = state.currentPage + 1
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
            val result = notificationRepository.getNotifications(
                username = username,
                page = nextPage,
                itemsPerPage = PAGE_SIZE
            )
            result.onSuccess { response ->
                val mappedItems = response.data.orEmpty().map(::mapNotification)
                _uiState.update { current ->
                    current.copy(
                        items = (current.items + mappedItems).distinctBy { it.id },
                        isLoadingMore = false,
                        currentPage = response.page,
                        totalPages = if (response.pages <= 0) current.totalPages else response.pages
                    )
                }
            }.onFailure { error ->
                Timber.e(error, "Failed to load more notifications")
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        errorMessage = error.message ?: "Không thể tải thêm thông báo"
                    )
                }
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        val username = currentUsername ?: return
        val index = _uiState.value.items.indexOfFirst { it.id == notificationId }
        if (index == -1) return
        val item = _uiState.value.items[index]
        if (item.isRead) return
        viewModelScope.launch {
            // Optimistic update
            _uiState.update { state ->
                val updatedItems = state.items.toMutableList()
                updatedItems[index] = updatedItems[index].copy(isRead = true)
                state.copy(
                    items = updatedItems,
                    unreadCount = (state.unreadCount - 1).coerceAtLeast(0)
                )
            }
            val result = notificationRepository.markNotificationRead(username, notificationId)
            result.onFailure { error ->
                Timber.e(error, "Failed to mark notification as read")
                // Revert changes
                _uiState.update { state ->
                    val updatedItems = state.items.toMutableList()
                    val itemIndex = updatedItems.indexOfFirst { it.id == notificationId }
                    if (itemIndex != -1) {
                        updatedItems[itemIndex] = updatedItems[itemIndex].copy(isRead = false)
                    }
                    state.copy(
                        items = updatedItems,
                        unreadCount = state.unreadCount + 1,
                        errorMessage = error.message ?: "Không thể đánh dấu đã đọc"
                    )
                }
            }
        }
    }

    fun markAllRead() {
        val username = currentUsername ?: return
        if (_uiState.value.items.none { !it.isRead }) return
        viewModelScope.launch {
            val previousItems = _uiState.value.items
            _uiState.update {
                it.copy(
                    items = it.items.map { item -> item.copy(isRead = true) },
                    unreadCount = 0
                )
            }
            val result = notificationRepository.markAllNotificationsRead(username)
            result.onFailure { error ->
                Timber.e(error, "Failed to mark all notifications as read")
                _uiState.update {
                    it.copy(
                        items = previousItems,
                        unreadCount = previousItems.count { item -> !item.isRead },
                        errorMessage = error.message ?: "Không thể đánh dấu tất cả đã đọc"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun updateUnreadCount(username: String) {
        if (isRefreshingUnread) return
        isRefreshingUnread = true
        val result = notificationRepository.getUnreadCount(username)
        result.onSuccess { response ->
            _uiState.update { it.copy(unreadCount = response.unreadCount) }
        }.onFailure { error ->
            Timber.w(error, "Unable to refresh unread notification count")
        }
        isRefreshingUnread = false
    }

    private fun mapNotification(notification: NotificationResponse): NotificationItemUi {
        return NotificationItemUi(
            id = notification.id,
            title = notification.title,
            message = notification.message,
            type = notification.type,
            createdAt = notification.createdAt,
            displayTime = formatRelativeTime(notification.createdAt),
            isRead = notification.isRead
        )
    }

    private fun formatRelativeTime(timestamp: String): String {
        return try {
            val instant = parseTimestamp(timestamp)
            val now = Instant.now()
            val duration = Duration.between(instant, now)
            if (!duration.isNegative) {
                when {
                    duration.seconds < 60 -> "Vừa xong"
                    duration.toMinutes() < 60 -> "${duration.toMinutes()} phút trước"
                    duration.toHours() < 24 -> "${duration.toHours()} giờ trước"
                    duration.toDays() < 7 -> "${duration.toDays()} ngày trước"
                    else -> DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
                        .withZone(ZoneId.systemDefault())
                        .format(instant)
                }
            } else {
                DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
                    .withZone(ZoneId.systemDefault())
                    .format(instant)
            }
        } catch (e: Exception) {
            Timber.w(e, "Unable to format timestamp: $timestamp")
            timestamp
        }
    }

    private fun parseTimestamp(value: String): Instant {
        return try {
            Instant.parse(value)
        } catch (ignored: DateTimeParseException) {
            OffsetDateTime.parse(value).toInstant()
        }
    }
}

data class NotificationUiState(
    val items: List<NotificationItemUi> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val unreadCount: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val initialized: Boolean = false
) {
    val hasMore: Boolean get() = currentPage < totalPages
}

data class NotificationItemUi(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val createdAt: String,
    val displayTime: String,
    val isRead: Boolean
)


