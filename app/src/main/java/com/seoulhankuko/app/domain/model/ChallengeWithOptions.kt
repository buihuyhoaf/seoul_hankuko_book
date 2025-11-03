package com.seoulhankuko.app.domain.model

data class ChallengeLite(
    val id: String,
    val lessonId: String,
    val type: QuestionType,
    val question: String,
    val order: Int
)

data class ChallengeOptionLite(
    val id: String,
    val challengeId: String,
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

