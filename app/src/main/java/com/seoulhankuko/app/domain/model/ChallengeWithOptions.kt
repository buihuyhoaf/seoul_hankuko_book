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

data class QuestionMetadata(
    val choices: List<String>? = null,
    val matchingPairs: List<QuestionMetadataPair>? = null
)

data class QuestionMetadataPair(
    val left: String?,
    val right: String?
)

data class ChallengeWithOptions(
    val challenge: ChallengeLite,
    val options: List<ChallengeOptionLite>,
    val completed: Boolean,
    val metadata: QuestionMetadata? = null,
    val matchingPairs: List<QuestionMatchingPair>? = null,
    val sentenceOrder: QuestionSentenceOrder? = null,
    val audioComprehension: QuestionAudioComprehension? = null,
    val pronunciation: QuestionPronunciation? = null,
    val blank: QuestionBlank? = null
)

data class QuestionMatchingPair(
    val id: String,
    val leftText: String?,
    val rightText: String?
)

data class QuestionSentenceOrder(
    val id: String,
    val correctSequence: List<String>
)

data class QuestionAudioComprehension(
    val id: String,
    val transcript: String?
)

data class QuestionPronunciation(
    val id: String,
    val targetPhrase: String,
    val referenceAudioUrl: String?
)

data class QuestionBlank(
    val id: String,
    val caseSensitive: Boolean
)

