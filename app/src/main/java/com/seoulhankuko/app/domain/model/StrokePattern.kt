package com.seoulhankuko.app.domain.model

/**
 * Domain model representing the ideal stroke pattern for a Hangul character
 * This is the reference template that user strokes are compared against
 * 
 * @param character The Hangul character symbol (e.g., "ㅏ", "가", "ㄱ")
 * @param strokes List of ideal strokes in drawing order
 */
data class StrokePattern(
    val character: String,
    val strokes: List<Stroke>
)

