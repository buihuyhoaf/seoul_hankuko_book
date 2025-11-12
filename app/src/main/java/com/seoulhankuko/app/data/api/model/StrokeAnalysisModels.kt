package com.seoulhankuko.app.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Request model for stroke analysis API
 */
data class StrokeAnalysisRequest(
    @SerializedName("points")
    val points: List<List<Float>>? = null,
    
    @SerializedName("image_base64")
    val imageBase64: String? = null,
    
    @SerializedName("target_char")
    val targetChar: String? = null
)

data class StrokeTopPredictionResponse(
    @SerializedName("index") val index: Int,
    @SerializedName("char") val char: String,
    @SerializedName("confidence") val confidence: Float,
)

data class StrokeAnalysisResponse(
    @SerializedName("predicted_char")
    val predictedChar: String,

    @SerializedName("confidence")
    val confidence: Float,

    @SerializedName("message")
    val message: String,

    @SerializedName("top_predictions")
    val topPredictions: List<StrokeTopPredictionResponse> = emptyList()
)

