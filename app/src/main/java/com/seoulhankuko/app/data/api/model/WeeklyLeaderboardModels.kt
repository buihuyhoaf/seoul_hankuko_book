package com.seoulhankuko.app.data.api.model

import com.google.gson.annotations.SerializedName

// Leaderboard Entry
data class LeaderboardEntry(
    val id: String,
    val rank: Int,
    val name: String,
    val avatar: String? = null,
    val country: String? = null,
    val xp: Int,
    @SerializedName("is_dummy")
    val isDummy: Boolean,
    @SerializedName("is_current_user")
    val isCurrentUser: Boolean = false,
    @SerializedName("rank_change")
    val rankChange: Int? = null  // Positive = up, Negative = down
)

// Weekly Leaderboard Response
data class WeeklyLeaderboardResponse(
    @SerializedName("week_start")
    val weekStart: String,  // ISO date format
    val entries: List<LeaderboardEntry>,
    @SerializedName("current_user_rank")
    val currentUserRank: Int? = null,
    @SerializedName("current_user_xp")
    val currentUserXp: Int? = null,
    @SerializedName("rank_change")
    val rankChange: Int? = null
)

// Update XP Request
data class WeeklyLeaderboardUpdateRequest(
    val exp: Int
)

// Update XP Response
data class WeeklyLeaderboardUpdateResponse(
    val message: String,
    @SerializedName("current_xp")
    val currentXp: Int,
    @SerializedName("current_rank")
    val currentRank: Int,
    @SerializedName("rank_change")
    val rankChange: Int? = null
)

