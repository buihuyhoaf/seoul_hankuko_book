package com.seoulhankuko.app.domain.model

import com.seoulhankuko.app.data.database.entities.Lesson

data class LessonWithChallenges(
    val lesson: Lesson,
    val challenges: List<ChallengeWithOptions>
)

