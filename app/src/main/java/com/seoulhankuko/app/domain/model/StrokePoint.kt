package com.seoulhankuko.app.domain.model

/**
 * Domain model representing a single point in a stroke
 * @param x X coordinate (normalized or pixel)
 * @param y Y coordinate (normalized or pixel)
 * @param t Timestamp in milliseconds
 */
data class StrokePoint(
    val x: Float,
    val y: Float,
    val t: Long = System.currentTimeMillis()
)

