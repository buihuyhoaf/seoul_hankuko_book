package com.seoulhankuko.app.domain.model

data class ExpBonusLog(
    val missionId: String,
    val startTime: Long, // timestamp
    val duration: Int, // minutes
    val expiresAt: Long // timestamp
)

