package com.seoulhankuko.app.domain.usecase

import com.seoulhankuko.app.data.repository.MLPredictionRepository
import com.seoulhankuko.app.domain.model.Stroke
import javax.inject.Inject

/**
 * Use case for ML-based character prediction
 * 
 * This use case calls the ML prediction repository to get character predictions
 * from the FastAPI backend using TensorFlow model.
 */
class PredictCharacterUseCase @Inject constructor(
    private val mlPredictionRepository: MLPredictionRepository
) {
    /**
     * Predict Hangul character from user-drawn strokes
     * 
     * @param strokes List of strokes drawn by the user
     * @return Result containing predicted character and confidence score
     */
    suspend operator fun invoke(strokes: List<Stroke>): Result<PredictCharacterResult> {
        if (strokes.isEmpty()) {
            return Result.failure(IllegalArgumentException("Strokes cannot be empty"))
        }
        
        return mlPredictionRepository.predictStroke(strokes).fold(
            onSuccess = { response ->
                Result.success(
                    PredictCharacterResult(
                        predictedChar = response.predictedChar,
                        confidence = response.confidence,
                        topK = response.topK?.map { 
                            TopKPrediction(it.char, it.confidence) 
                        } ?: emptyList()
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
 * Result of character prediction
 */
data class PredictCharacterResult(
    val predictedChar: String,
    val confidence: Float,
    val topK: List<TopKPrediction> = emptyList()
)

/**
 * Top K prediction entry
 */
data class TopKPrediction(
    val char: String,
    val confidence: Float
)

