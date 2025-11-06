package com.seoulhankuko.app.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Request model for stroke prediction
 */
data class StrokePointRequest(
    @SerializedName("x")
    val x: Float,
    
    @SerializedName("y")
    val y: Float
)

/**
 * Request model for a single stroke
 */
data class StrokeRequest(
    @SerializedName("stroke_id")
    val strokeId: Int,
    
    @SerializedName("points")
    val points: List<StrokePointRequest>,
    
    @SerializedName("timestamp")
    val timestamp: Long? = null
)

/**
 * Request body for stroke prediction endpoint
 */
data class PredictStrokeRequest(
    @SerializedName("strokes")
    val strokes: List<StrokeRequest>
)

/**
 * Top K prediction result
 */
data class TopKPrediction(
    @SerializedName("char")
    val char: String,
    
    @SerializedName("confidence")
    val confidence: Float
)

/**
 * Response model for stroke prediction
 */
data class PredictStrokeResponse(
    @SerializedName("predicted_char")
    val predictedChar: String,
    
    @SerializedName("confidence")
    val confidence: Float,
    
    @SerializedName("top_k")
    val topK: List<TopKPrediction>? = null
)

