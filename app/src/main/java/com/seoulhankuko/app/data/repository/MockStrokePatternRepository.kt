package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.domain.model.Stroke
import com.seoulhankuko.app.domain.model.StrokePattern
import com.seoulhankuko.app.domain.model.StrokePoint
import com.seoulhankuko.app.domain.usecase.StrokePatternRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock implementation of StrokePatternRepository
 * Provides hardcoded stroke patterns for testing
 * 
 * TODO: Replace with JSON parsing or Room database
 * TODO: Add more character patterns
 */
@Singleton
class MockStrokePatternRepository @Inject constructor() : StrokePatternRepository {
    
    private val patterns = mapOf(
        "ㅏ" to createPatternA(),
        "가" to createPatternGa(),
        "ㄱ" to createPatternGiyeok()
    )
    
    override suspend fun getPattern(character: String): StrokePattern? {
        return patterns[character]?.copy()
    }
    
    /**
     * Create pattern for "ㅏ" (vowel a)
     * Vertical line down, then horizontal line to the right
     * Coordinates are normalized (0-1) for template, will be scaled to canvas size
     */
    private fun createPatternA(): StrokePattern {
        return StrokePattern(
            character = "ㅏ",
            strokes = listOf(
                // Vertical stroke
                Stroke(
                    points = listOf(
                        StrokePoint(0.5f, 0.2f),
                        StrokePoint(0.5f, 0.8f)
                    )
                ),
                // Horizontal stroke
                Stroke(
                    points = listOf(
                        StrokePoint(0.5f, 0.5f),
                        StrokePoint(0.85f, 0.5f)
                    )
                )
            )
        )
    }
    
    /**
     * Create pattern for "가" (ga)
     * First stroke: ㄱ (giyeok), then ㅏ (a)
     */
    private fun createPatternGa(): StrokePattern {
        return StrokePattern(
            character = "가",
            strokes = listOf(
                // ㄱ stroke: horizontal then diagonal down-left
                Stroke(
                    points = listOf(
                        StrokePoint(0.2f, 0.3f),
                        StrokePoint(0.5f, 0.3f),
                        StrokePoint(0.5f, 0.7f),
                        StrokePoint(0.2f, 0.7f)
                    )
                ),
                // ㅏ vertical stroke
                Stroke(
                    points = listOf(
                        StrokePoint(0.6f, 0.2f),
                        StrokePoint(0.6f, 0.8f)
                    )
                ),
                // ㅏ horizontal stroke
                Stroke(
                    points = listOf(
                        StrokePoint(0.6f, 0.5f),
                        StrokePoint(0.85f, 0.5f)
                    )
                )
            )
        )
    }
    
    /**
     * Create pattern for "ㄱ" (giyeok consonant)
     * Horizontal line then diagonal down-left
     */
    private fun createPatternGiyeok(): StrokePattern {
        return StrokePattern(
            character = "ㄱ",
            strokes = listOf(
                Stroke(
                    points = listOf(
                        StrokePoint(0.2f, 0.3f),
                        StrokePoint(0.6f, 0.3f),
                        StrokePoint(0.6f, 0.7f),
                        StrokePoint(0.2f, 0.7f)
                    )
                )
            )
        )
    }
}

