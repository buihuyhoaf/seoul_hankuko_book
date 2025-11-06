package com.seoulhankuko.app.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Path representing a stroke as a series of points
 * Used for comparison and animation
 */
data class StrokePath(
    val points: List<Offset>
) {
    /**
     * Convert to Compose Path for drawing
     */
    fun toComposePath(): Path {
        val path = Path()
        if (points.isEmpty()) return path
        
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        return path
    }
    
    /**
     * Get bounding box
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
     * Normalize path to fit in [0, 1] range
     */
    fun normalize(): StrokePath {
        val box = getBoundingBox() ?: return this
        val width = box.maxX - box.minX
        val height = box.maxY - box.minY
        
        if (width == 0f || height == 0f) return this
        
        val scale = 1f / maxOf(width, height)
        val centerX = (box.minX + box.maxX) / 2f
        val centerY = (box.minY + box.maxY) / 2f
        
        val normalizedPoints = points.map { point ->
            Offset(
                x = (point.x - centerX) * scale + 0.5f,
                y = (point.y - centerY) * scale + 0.5f
            )
        }
        
        return StrokePath(normalizedPoints)
    }
    
    /**
     * Resample path to fixed number of points
     */
    fun resample(targetPoints: Int): StrokePath {
        if (points.size <= targetPoints) return this
        if (points.isEmpty()) return this
        
        // Calculate cumulative path length
        val segmentLengths = mutableListOf<Float>()
        var totalLength = 0f
        
        for (i in 1 until points.size) {
            val dx = points[i].x - points[i - 1].x
            val dy = points[i].y - points[i - 1].y
            val length = sqrt(dx * dx + dy * dy)
            segmentLengths.add(length)
            totalLength += length
        }
        
        if (totalLength == 0f) return this
        
        val interval = totalLength / (targetPoints - 1)
        val resampled = mutableListOf<Offset>()
        resampled.add(points[0])
        
        var currentLength = 0f
        var segmentIndex = 0
        
        for (i in 1 until targetPoints - 1) {
            val targetLength = interval * i
            
            while (segmentIndex < segmentLengths.size && currentLength < targetLength) {
                if (currentLength + segmentLengths[segmentIndex] >= targetLength) {
                    // Interpolate within this segment
                    val t = (targetLength - currentLength) / segmentLengths[segmentIndex]
                    val p1 = points[segmentIndex]
                    val p2 = points[segmentIndex + 1]
                    val interpolated = Offset(
                        x = p1.x + t * (p2.x - p1.x),
                        y = p1.y + t * (p2.y - p1.y)
                    )
                    resampled.add(interpolated)
                    break
                } else {
                    currentLength += segmentLengths[segmentIndex]
                    segmentIndex++
                }
            }
        }
        
        resampled.add(points.last())
        return StrokePath(resampled)
    }
}

