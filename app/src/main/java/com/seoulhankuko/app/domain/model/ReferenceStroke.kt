package com.seoulhankuko.app.domain.model

import androidx.compose.ui.geometry.Offset

/**
 * Reference stroke path for a Hangul character
 * Contains the ideal stroke path as a list of points
 */
data class ReferenceStroke(
    val character: String,
    val strokeIndex: Int,
    val points: List<Offset>
) {
    /**
     * Check if reference stroke is valid
     */
    val isValid: Boolean
        get() = points.isNotEmpty() && points.size >= 2
}

/**
 * Complete reference pattern for a character
 * Contains all strokes for the character in drawing order
 */
data class ReferencePattern(
    val character: String,
    val strokes: List<ReferenceStroke>
) {
    /**
     * Get stroke count
     */
    val strokeCount: Int
        get() = strokes.size
    
    /**
     * Check if pattern is valid
     */
    val isValid: Boolean
        get() = strokes.isNotEmpty() && strokes.all { it.isValid }
}

