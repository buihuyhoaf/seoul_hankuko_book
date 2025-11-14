package com.seoulhankuko.app.data.api.model

import com.google.gson.annotations.SerializedName

// Mission Response
data class MissionResponse(
    @SerializedName("mission_id")
    val missionId: String,
    val type: String, // "lesson", "speaking", "listening"
    val target: Int,
    val progress: Int,
    @SerializedName("is_completed")
    val isCompleted: Boolean
)

// Today Missions Response
data class TodayMissionsResponse(
    val missions: List<MissionResponse>
)

// Activity Request
data class ActivityRequest(
    val type: String // "lesson", "speaking", "listening"
)

// Activity Response
data class ActivityResponse(
    @SerializedName("mission_completed")
    val missionCompleted: Boolean,
    @SerializedName("mission_id")
    val missionId: String? = null,
    @SerializedName("expires_at")
    val expiresAt: Long? = null // timestamp in milliseconds
)

// EXP Request
data class ExpRequest(
    val exp: Int // EXP đã được nhân đôi ở client
)

// EXP Response
data class ExpResponse(
    val message: String,
    @SerializedName("total_exp")
    val totalExp: Int
)

