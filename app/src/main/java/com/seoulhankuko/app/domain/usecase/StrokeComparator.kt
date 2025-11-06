package com.seoulhankuko.app.domain.usecase

import androidx.compose.ui.geometry.Offset
import com.seoulhankuko.app.domain.model.StrokePath
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Comparator for comparing user strokes with reference strokes
 * Uses Hausdorff distance and DTW (Dynamic Time Warping) for similarity
 */
class StrokeComparator {
    
    /**
     * Similarity threshold for auto-correction
     * Strokes with similarity >= this threshold will be auto-corrected
     */
    companion object {
        const val SIMILARITY_THRESHOLD = 0.75f
        const val RESAMPLE_POINTS = 64
    }
    
    /**
     * Calculate similarity between user stroke and reference stroke
     * Returns value between 0.0 (completely different) and 1.0 (identical)
     * 
     * @param userStroke User-drawn stroke path
     * @param referenceStroke Reference stroke path
     * @return Similarity score (0-1)
     */
    fun calculateSimilarity(
        userStroke: StrokePath,
        referenceStroke: StrokePath
    ): Float {
        if (userStroke.points.isEmpty() || referenceStroke.points.isEmpty()) {
            return 0f
        }
        
        // Normalize both paths
        val normalizedUser = userStroke.normalize().resample(RESAMPLE_POINTS)
        val normalizedReference = referenceStroke.normalize().resample(RESAMPLE_POINTS)
        
        // Calculate Hausdorff distance
        val hausdorffDistance = calculateHausdorffDistance(
            normalizedUser.points,
            normalizedReference.points
        )
        
        // Calculate DTW distance
        val dtwDistance = calculateDTWDistance(
            normalizedUser.points,
            normalizedReference.points
        )
        
        // Combine distances (weighted average)
        val combinedDistance = 0.6f * hausdorffDistance + 0.4f * dtwDistance
        
        // Convert distance to similarity (inverse relationship)
        // Max expected distance in normalized space is ~sqrt(2) = 1.41
        val normalizedDistance = min(combinedDistance / 1.41f, 1f)
        val similarity = 1f - normalizedDistance
        
        return similarity.coerceIn(0f, 1f)
    }
    
    /**
     * Calculate Hausdorff distance between two point sets
     * Hausdorff distance is the maximum of minimum distances
     */
    private fun calculateHausdorffDistance(
        points1: List<Offset>,
        points2: List<Offset>
    ): Float {
        if (points1.isEmpty() || points2.isEmpty()) return Float.MAX_VALUE
        
        // Forward distance: max of min distances from points1 to points2
        val forwardMax = points1.maxOfOrNull { p1 ->
            points2.minOfOrNull { p2 ->
                distance(p1, p2)
            } ?: Float.MAX_VALUE
        } ?: Float.MAX_VALUE
        
        // Backward distance: max of min distances from points2 to points1
        val backwardMax = points2.maxOfOrNull { p2 ->
            points1.minOfOrNull { p1 ->
                distance(p1, p2)
            } ?: Float.MAX_VALUE
        } ?: Float.MAX_VALUE
        
        // Hausdorff distance is the maximum of forward and backward
        return max(forwardMax, backwardMax)
    }
    
    /**
     * Calculate DTW (Dynamic Time Warping) distance
     * DTW finds the optimal alignment between two sequences
     */
    private fun calculateDTWDistance(
        points1: List<Offset>,
        points2: List<Offset>
    ): Float {
        if (points1.isEmpty() || points2.isEmpty()) return Float.MAX_VALUE
        
        val n = points1.size
        val m = points2.size
        
        // Create DTW matrix
        val dtw = Array(n + 1) { FloatArray(m + 1) { Float.MAX_VALUE } }
        dtw[0][0] = 0f
        
        // Fill DTW matrix
        for (i in 1..n) {
            for (j in 1..m) {
                val cost = distance(points1[i - 1], points2[j - 1])
                dtw[i][j] = cost + min(
                    dtw[i - 1][j],      // Insertion
                    min(
                        dtw[i][j - 1],  // Deletion
                        dtw[i - 1][j - 1] // Match
                    )
                )
            }
        }
        
        return dtw[n][m] / (n + m) // Normalize by path length
    }
    
    /**
     * Calculate Euclidean distance between two points
     */
    private fun distance(p1: Offset, p2: Offset): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Check if user stroke is similar enough to reference for auto-correction
     */
    fun isSimilarEnough(
        userStroke: StrokePath,
        referenceStroke: StrokePath
    ): Boolean {
        val similarity = calculateSimilarity(userStroke, referenceStroke)
        return similarity >= SIMILARITY_THRESHOLD
    }
}

