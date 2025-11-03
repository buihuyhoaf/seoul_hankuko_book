package com.seoulhankuko.app.domain.model

import com.seoulhankuko.app.data.api.model.ExerciseResponse

data class LessonLite(
    val id: String,
    val title: String,
    val unitId: String,
    val order: Int
)

data class ExerciseLite(
    val id: String,
    val type: String, // "listening", "speaking", "writing", "pronunciation"
    val title: String?,
    val content: String?,
    val orderIndex: Int
)

data class LessonWithChallenges(
    val lesson: LessonLite,
    val challenges: List<ChallengeWithOptions>,
    val exercises: List<ExerciseLite> = emptyList(),
    val exercisesResponse: List<ExerciseResponse> = emptyList(), // Full exercise data from API
    val progressPercent: Int = 0 // Progress percentage from backend
)

