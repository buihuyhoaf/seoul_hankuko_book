package com.seoulhankuko.app.data.api.model

import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    @SerializedName("is_read")
    val isRead: Boolean,
    @SerializedName("created_at")
    val createdAt: String
)

data class NotificationUnreadCountResponse(
    @SerializedName("unread_count")
    val unreadCount: Int,
    val username: String
)


