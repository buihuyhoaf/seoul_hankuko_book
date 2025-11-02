package com.seoulhankuko.app.domain.model

data class ChallengeLite(
    val id: Int,
    val lessonId: Int,
    val type: QuestionType,
    val question: String,
    val order: Int
)

data class ChallengeOptionLite(
    val id: Int,
    val challengeId: Int,
    val text: String,
    val correct: Boolean,
    val imageSrc: String? = null,
    val audioSrc: String? = null
)

data class ChallengeWithOptions(
    val challenge: ChallengeLite,
    val options: List<ChallengeOptionLite>,
    val completed: Boolean
)

