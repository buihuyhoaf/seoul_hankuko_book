package com.seoulhankuko.app.domain.model

/**
 * Domain model representing a complete stroke (sequence of points)
 * A stroke is drawn from finger down to finger up
 */
data class Stroke(
    val points: List<StrokePoint>
) {
    /**
     * Check if stroke is empty
     */
    val isEmpty: Boolean
        get() = points.isEmpty()

    /**
     * Get bounding box of the stroke
     */
    fun getBoundingBox(): BoundingBox? {
        if (points.isEmpty()) return null
        
        val xs = points.map { it.x }
        val ys = points.map { it.y }
        
        return BoundingBox(
            minX = xs.minOrNull() ?: 0f,
            maxX = xs.maxOrNull() ?: 0f,
            minY = ys.minOrNull() ?: 0f,
            maxY = ys.maxOrNull() ?: 0f
        )
    }

    /**
     * Calculate total path length
     */
    fun getPathLength(): Float {
        if (points.size < 2) return 0f
        
        var length = 0f
        for (i in 1 until points.size) {
            val dx = points[i].x - points[i - 1].x
            val dy = points[i].y - points[i - 1].y
            length += kotlin.math.sqrt(dx * dx + dy * dy)
        }
        return length
    }
}

/**
 * Bounding box helper class
 */
data class BoundingBox(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f
    
    /**
     * Calculate Intersection over Union (IoU) with another bounding box
     */
    fun iou(other: BoundingBox): Float {
        val intersectionX = maxOf(minX, other.minX) to minOf(maxX, other.maxX)
        val intersectionY = maxOf(minY, other.minY) to minOf(maxY, other.maxY)
        
        if (intersectionX.first >= intersectionX.second || intersectionY.first >= intersectionY.second) {
            return 0f
        }
        
        val intersectionArea = (intersectionX.second - intersectionX.first) * (intersectionY.second - intersectionY.first)
        val unionArea = width * height + other.width * other.height - intersectionArea
        
        return if (unionArea > 0f) intersectionArea / unionArea else 0f
    }
}

