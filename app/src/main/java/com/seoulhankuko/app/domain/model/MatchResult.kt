package com.seoulhankuko.app.domain.model

/**
 * Result of comparing user strokes with template pattern
 * @param score Similarity score from 0.0 to 1.0
 * @param label Match quality label
 */
data class MatchResult(
    val score: Float,
    val label: MatchLabel
) {
    init {
        require(score in 0f..1f) { "Score must be between 0 and 1" }
    }
}

/**
 * Match quality label based on score thresholds
 */
enum class MatchLabel {
    /** Score >= 0.8: Excellent match */
    PERFECT,
    
    /** Score 0.5-0.79: Good match but needs improvement */
    ALMOST,
    
    /** Score < 0.5: Needs more practice */
    TRY_AGAIN
}

