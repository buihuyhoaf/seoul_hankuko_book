package com.seoulhankuko.app.domain.usecase

import com.seoulhankuko.app.data.repository.StrokeAnalysisRepository
import javax.inject.Inject

/**
 * Use case for analyzing hand-drawn strokes using ML
 * 
 * This use case calls the stroke analysis repository to get ML predictions
 * from the FastAPI backend
 */
class AnalyzeStrokeUseCase @Inject constructor(
    private val strokeAnalysisRepository: StrokeAnalysisRepository
) {
    /**
     * Analyze stroke and get prediction
     * 
     * @param points Optional list of stroke points as [[x1, y1], [x2, y2], ...]
     * @param imageBase64 Optional base64 encoded image
     * @param targetChar Optional target character for comparison
     * @return Result containing analysis result
     */
    suspend operator fun invoke(
        points: List<List<Float>>? = null,
        imageBase64: String? = null,
        targetChar: String? = null
    ): Result<StrokeAnalysisResult> {
        if (points == null && imageBase64 == null) {
            return Result.failure(IllegalArgumentException("Either points or imageBase64 must be provided"))
        }
        
        return strokeAnalysisRepository.analyzeStroke(
            points = points,
            imageBase64 = imageBase64,
            targetChar = targetChar
        ).fold(
            onSuccess = { response ->
                Result.success(
                    StrokeAnalysisResult(
                        predictedChar = response.predictedChar,
                        confidence = response.confidence,
                        message = response.message,
                        topPredictions = response.topPredictions.map {
                            StrokeTopPrediction(
                                index = it.index,
                                char = it.char,
                                confidence = it.confidence
                            )
                        }
                    )
                )
            },
            onFailure = { exception ->
                Result.failure(exception)
            }
        )
    }
}

/**
 * Result of stroke analysis
 */
data class StrokeAnalysisResult(
    val predictedChar: String,
    val confidence: Float,
    val message: String,
    val topPredictions: List<StrokeTopPrediction>
)

data class StrokeTopPrediction(
    val index: Int,
    val char: String,
    val confidence: Float
)

