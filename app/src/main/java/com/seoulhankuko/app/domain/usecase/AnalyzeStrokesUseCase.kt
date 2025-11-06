package com.seoulhankuko.app.domain.usecase

import com.seoulhankuko.app.domain.model.*
import javax.inject.Inject

/**
 * Use case for analyzing user-drawn strokes against reference patterns
 * 
 * This use case implements a heuristic matching algorithm that:
 * 1. Normalizes both user and template strokes
 * 2. Calculates similarity metrics (point distance, path length, bounding box IoU)
 * 3. Combines metrics into a final score (0-1)
 * 4. Returns MatchResult with appropriate label
 * 
 * TODO: Replace with ML Kit integration in future for more accurate matching
 */
class AnalyzeStrokesUseCase @Inject constructor(
    private val repository: StrokePatternRepository
) {
    /**
     * Analyze user strokes against the template pattern for a character
     * 
     * @param character The Hangul character to match against
     * @param userStrokes The strokes drawn by the user
     * @return MatchResult with score and label
     */
    suspend operator fun invoke(
        character: String,
        userStrokes: List<Stroke>
    ): MatchResult {
        // Get template pattern
        val template = repository.getPattern(character)
            ?: return MatchResult(0f, MatchLabel.TRY_AGAIN)
        
        if (userStrokes.isEmpty() || template.strokes.isEmpty()) {
            return MatchResult(0f, MatchLabel.TRY_AGAIN)
        }
        
        // Normalize and compare strokes
        val normalizedUser = normalizeStrokes(userStrokes)
        val normalizedTemplate = normalizeStrokes(template.strokes)
        
        // Calculate similarity metrics
        val pointDistanceScore = calculatePointDistanceScore(normalizedUser, normalizedTemplate)
        val pathLengthScore = calculatePathLengthScore(userStrokes, template.strokes)
        val boundingBoxScore = calculateBoundingBoxScore(userStrokes, template.strokes)
        
        // Weighted combination: 60% point distance, 20% path length, 20% bounding box
        val finalScore = (0.6f * pointDistanceScore + 
                         0.2f * pathLengthScore + 
                         0.2f * boundingBoxScore).coerceIn(0f, 1f)
        
        // Convert score to label
        val label = when {
            finalScore >= 0.8f -> MatchLabel.PERFECT
            finalScore >= 0.5f -> MatchLabel.ALMOST
            else -> MatchLabel.TRY_AGAIN
        }
        
        return MatchResult(finalScore, label)
    }
    
    /**
     * Normalize strokes: translate to origin, scale to unit box, resample points
     */
    private fun normalizeStrokes(strokes: List<Stroke>): List<List<StrokePoint>> {
        if (strokes.isEmpty()) return emptyList()
        
        // Flatten all points to find global bounding box
        val allPoints = strokes.flatMap { it.points }
        if (allPoints.isEmpty()) return emptyList()
        
        val xs = allPoints.map { it.x }
        val ys = allPoints.map { it.y }
        val minX = xs.minOrNull() ?: 0f
        val maxX = xs.maxOrNull() ?: 0f
        val minY = ys.minOrNull() ?: 0f
        val maxY = ys.maxOrNull() ?: 0f
        
        val width = maxX - minX
        val height = maxY - minY
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        
        // Avoid division by zero
        val scale = if (width > 0f && height > 0f) {
            val scaleX = 1f / width
            val scaleY = 1f / height
            minOf(scaleX, scaleY) // Keep aspect ratio
        } else {
            1f
        }
        
        // Normalize each stroke
        return strokes.map { stroke ->
            val normalizedPoints = stroke.points.map { point ->
                val translatedX = point.x - centerX
                val translatedY = point.y - centerY
                StrokePoint(
                    x = translatedX * scale,
                    y = translatedY * scale,
                    t = point.t
                )
            }
            
            // Resample to fixed number of points (128) for comparison
            resamplePoints(normalizedPoints, targetPoints = 128)
        }
    }
    
    /**
     * Resample points to a fixed number using linear interpolation
     */
    private fun resamplePoints(points: List<StrokePoint>, targetPoints: Int): List<StrokePoint> {
        if (points.size <= targetPoints) return points
        if (points.isEmpty()) return emptyList()
        if (points.size == 1) return List(targetPoints) { points[0] }
        
        // Calculate cumulative path length
        val pathLength = points.zipWithNext { a, b ->
            val dx = b.x - a.x
            val dy = b.y - a.y
            kotlin.math.sqrt(dx * dx + dy * dy)
        }.sum()
        
        if (pathLength == 0f) {
            return List(targetPoints) { points[0] }
        }
        
        val interval = pathLength / (targetPoints - 1)
        val resampled = mutableListOf<StrokePoint>()
        resampled.add(points[0])
        
        var currentLength = 0f
        var segmentIndex = 0
        
        for (i in 1 until targetPoints - 1) {
            val targetLength = interval * i
            
            while (segmentIndex < points.size - 1 && currentLength < targetLength) {
                val dx = points[segmentIndex + 1].x - points[segmentIndex].x
                val dy = points[segmentIndex + 1].y - points[segmentIndex].y
                val segmentLength = kotlin.math.sqrt(dx * dx + dy * dy)
                
                if (currentLength + segmentLength >= targetLength) {
                    // Interpolate within this segment
                    val t = (targetLength - currentLength) / segmentLength
                    val interpolated = StrokePoint(
                        x = points[segmentIndex].x + t * dx,
                        y = points[segmentIndex].y + t * dy,
                        t = points[segmentIndex].t + (t * (points[segmentIndex + 1].t - points[segmentIndex].t)).toLong()
                    )
                    resampled.add(interpolated)
                    break
                } else {
                    currentLength += segmentLength
                    segmentIndex++
                }
            }
        }
        
        resampled.add(points.last())
        return resampled
    }
    
    /**
     * Calculate point-to-point distance score (0-1)
     * Lower average distance = higher score
     */
    private fun calculatePointDistanceScore(
        normalizedUser: List<List<StrokePoint>>,
        normalizedTemplate: List<List<StrokePoint>>
    ): Float {
        if (normalizedUser.size != normalizedTemplate.size) {
            // Penalize stroke count mismatch
            return 0.3f
        }
        
        var totalDistance = 0f
        var totalPoints = 0
        
        for (i in normalizedUser.indices) {
            val userPoints = normalizedUser[i]
            val templatePoints = normalizedTemplate[i]
            
            val minSize = minOf(userPoints.size, templatePoints.size)
            if (minSize == 0) continue
            
            for (j in 0 until minSize) {
                val dx = userPoints[j].x - templatePoints[j].x
                val dy = userPoints[j].y - templatePoints[j].y
                totalDistance += kotlin.math.sqrt(dx * dx + dy * dy)
                totalPoints++
            }
        }
        
        if (totalPoints == 0) return 0f
        
        val avgDistance = totalDistance / totalPoints
        // Normalize: max expected distance in unit box is ~sqrt(2) = 1.41
        val maxExpectedDistance = 1.41f
        val normalizedDistance = (avgDistance / maxExpectedDistance).coerceIn(0f, 1f)
        
        // Convert distance to score (inverse: lower distance = higher score)
        return (1f - normalizedDistance).coerceIn(0f, 1f)
    }
    
    /**
     * Calculate path length ratio score (0-1)
     * Closer length ratio = higher score
     */
    private fun calculatePathLengthScore(
        userStrokes: List<Stroke>,
        templateStrokes: List<Stroke>
    ): Float {
        if (userStrokes.size != templateStrokes.size) return 0.3f
        
        var totalScore = 0f
        var count = 0
        
        for (i in userStrokes.indices) {
            val userLength = userStrokes[i].getPathLength()
            val templateLength = templateStrokes[i].getPathLength()
            
            if (templateLength == 0f) {
                totalScore += if (userLength == 0f) 1f else 0f
            } else {
                val ratio = userLength / templateLength
                // Score: 1 - abs(1 - ratio), clamped to 0-1
                val score = (1f - kotlin.math.abs(1f - ratio)).coerceIn(0f, 1f)
                totalScore += score
            }
            count++
        }
        
        return if (count > 0) totalScore / count else 0f
    }
    
    /**
     * Calculate bounding box IoU score (0-1)
     */
    private fun calculateBoundingBoxScore(
        userStrokes: List<Stroke>,
        templateStrokes: List<Stroke>
    ): Float {
        val userBox = userStrokes.mapNotNull { it.getBoundingBox() }
            .reduceOrNull { acc, box ->
                BoundingBox(
                    minX = minOf(acc.minX, box.minX),
                    maxX = maxOf(acc.maxX, box.maxX),
                    minY = minOf(acc.minY, box.minY),
                    maxY = maxOf(acc.maxY, box.maxY)
                )
            }
        
        val templateBox = templateStrokes.mapNotNull { it.getBoundingBox() }
            .reduceOrNull { acc, box ->
                BoundingBox(
                    minX = minOf(acc.minX, box.minX),
                    maxX = maxOf(acc.maxX, box.maxX),
                    minY = minOf(acc.minY, box.minY),
                    maxY = maxOf(acc.maxY, box.maxY)
                )
            }
        
        return if (userBox != null && templateBox != null) {
            userBox.iou(templateBox)
        } else {
            0f
        }
    }
}


