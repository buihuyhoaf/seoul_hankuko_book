package com.seoulhankuko.app.domain.model

data class LessonLite(
    val id: Int,
    val title: String,
    val unitId: Int,
    val order: Int
)

data class LessonWithChallenges(
    val lesson: LessonLite,
    val challenges: List<ChallengeWithOptions>,
    val progressPercent: Int = 0 // Progress percentage from backend
)

