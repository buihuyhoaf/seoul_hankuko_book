package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.api.model.StrokeAnalysisRequest
import com.seoulhankuko.app.data.api.model.StrokeAnalysisResponse
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.data.api.util.ExceptionMapper
import com.seoulhankuko.app.domain.exception.AppException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for stroke analysis API calls
 * Handles communication with FastAPI stroke analysis endpoint
 */
@Singleton
class StrokeAnalysisRepository @Inject constructor(
    private val apiService: ApiService
) {
    /**
     * Analyze stroke data and get ML prediction
     * 
     * @param points Optional list of stroke points
     * @param imageBase64 Optional base64 encoded image
     * @param targetChar Optional target character for comparison
     * @return Result containing predicted character and confidence
     */
    suspend fun analyzeStroke(
        points: List<List<Float>>? = null,
        imageBase64: String? = null,
        targetChar: String? = null
    ): Result<StrokeAnalysisResponse> {
        return try {
            // Validate input
            if (points == null && imageBase64 == null) {
                return Result.failure(
                    AppException.UnexpectedError("Either points or imageBase64 must be provided")
                )
            }
            
            val request = StrokeAnalysisRequest(
                points = points,
                imageBase64 = imageBase64,
                targetChar = targetChar
            )
            
            // Call API
            val response = apiService.analyzeStroke(request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(AppException.UnexpectedError("Empty response from stroke analysis API"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to analyze stroke"
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

