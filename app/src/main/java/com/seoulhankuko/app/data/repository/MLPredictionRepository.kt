package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.api.model.*
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.data.api.util.ExceptionMapper
import com.seoulhankuko.app.domain.exception.AppException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for ML-based stroke prediction
 * Handles communication with FastAPI ML prediction endpoint
 */
@Singleton
class MLPredictionRepository @Inject constructor(
    private val apiService: ApiService
) {
    /**
     * Predict Hangul character from stroke data
     * 
     * @param strokes List of strokes (each stroke contains points)
     * @return PredictStrokeResponse with predicted character and confidence
     */
    suspend fun predictStroke(strokes: List<com.seoulhankuko.app.domain.model.Stroke>): Result<PredictStrokeResponse> {
        return try {
            // Convert domain strokes to API request format
            val strokeRequests = strokes.mapIndexed { index, stroke ->
                StrokeRequest(
                    strokeId = index,
                    points = stroke.points.map { point ->
                        StrokePointRequest(
                            x = point.x,
                            y = point.y
                        )
                    },
                    timestamp = if (stroke.points.isNotEmpty()) stroke.points[0].t else null
                )
            }
            
            val request = PredictStrokeRequest(strokes = strokeRequests)
            
            // Call API
            val response = apiService.predictStroke(request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(AppException.UnexpectedError("Empty response from prediction API"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to predict stroke"
                val appException = ExceptionMapper.mapToAppException(
                    Exception(errorMessage)
                )
                Result.failure(appException)
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
}

