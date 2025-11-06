package com.seoulhankuko.app.domain.usecase

import androidx.compose.ui.geometry.Offset
import com.seoulhankuko.app.domain.model.StrokePath
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Auto-corrector for user strokes
 * Interpolates user stroke to reference stroke with smooth animation
 */
class StrokeAutoCorrector {
    
    /**
     * Interpolate between user stroke and reference stroke
     * 
     * @param userStroke User-drawn stroke
     * @param referenceStroke Reference stroke
     * @param progress Animation progress (0.0 to 1.0)
     * @return Interpolated stroke path
     */
    fun interpolate(
        userStroke: StrokePath,
        referenceStroke: StrokePath,
        progress: Float
    ): StrokePath {
        if (progress <= 0f) return userStroke
        if (progress >= 1f) return referenceStroke
        
        // Resample both paths to same number of points for interpolation
        val targetPoints = maxOf(userStroke.points.size, referenceStroke.points.size)
        val resampledUser = userStroke.resample(targetPoints)
        val resampledReference = referenceStroke.resample(targetPoints)
        
        // Interpolate each point
        val interpolatedPoints = resampledUser.points.zip(resampledReference.points) { userPoint, refPoint ->
            interpolatePoint(userPoint, refPoint, progress)
        }
        
        return StrokePath(interpolatedPoints)
    }
    
    /**
     * Interpolate a single point with easing function
     * Uses ease-in-out cubic for smooth animation
     */
    private fun interpolatePoint(
        start: Offset,
        end: Offset,
        progress: Float
    ): Offset {
        // Apply easing function (ease-in-out cubic)
        val easedProgress = easeInOutCubic(progress)
        
        val x = start.x + (end.x - start.x) * easedProgress
        val y = start.y + (end.y - start.y) * easedProgress
        
        return Offset(x, y)
    }
    
    /**
     * Ease-in-out cubic easing function
     * Provides smooth acceleration and deceleration
     */
    private fun easeInOutCubic(t: Float): Float {
        return when {
            t < 0.5f -> 4f * t * t * t
            else -> {
                val f = 2f * t - 2f
                1f + f * f * f / 2f
            }
        }
    }
    
    /**
     * Create smooth correction path from user stroke to reference
     * Returns intermediate steps for animation
     */
    fun createCorrectionSequence(
        userStroke: StrokePath,
        referenceStroke: StrokePath,
        steps: Int = 30
    ): List<StrokePath> {
        val sequence = mutableListOf<StrokePath>()
        
        for (i in 0..steps) {
            val progress = i.toFloat() / steps.toFloat()
            val interpolated = interpolate(userStroke, referenceStroke, progress)
            sequence.add(interpolated)
        }
        
        return sequence
    }
}

