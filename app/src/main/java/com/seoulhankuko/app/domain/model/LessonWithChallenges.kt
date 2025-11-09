package com.seoulhankuko.app.domain.model

import com.seoulhankuko.app.data.api.model.ExerciseResponse
import com.seoulhankuko.app.data.api.model.QuestionResponse

data class LessonLite(
    val id: String,
    val title: String,
    val unitId: String,
    val order: Int,
    val description: String? = null,
    val targetExp: Int? = null,
    val expPerQuestion: Float? = null
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
    val questionResponses: List<QuestionResponse> = emptyList(),
    val exercises: List<ExerciseLite> = emptyList(),
    val exercisesResponse: List<ExerciseResponse> = emptyList(), // Full exercise data from API
    val progressPercent: Int = 0, // Progress percentage from backend
    val hasMoreQuestions: Boolean = false
)

