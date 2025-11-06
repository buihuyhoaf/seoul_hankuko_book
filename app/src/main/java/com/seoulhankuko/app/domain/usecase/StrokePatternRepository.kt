package com.seoulhankuko.app.domain.usecase

import com.seoulhankuko.app.domain.model.StrokePattern

/**
 * Repository interface for stroke patterns
 * TODO: Implement Room persistence for patterns
 * TODO: Add ML Kit integration for pattern recognition
 */
interface StrokePatternRepository {
    suspend fun getPattern(character: String): StrokePattern?
}

