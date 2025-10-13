package com.seoulhankuko.app.data.api.model

import com.google.gson.annotations.SerializedName

// Login request
data class LoginRequest(
    val username: String, // Can be username or email
    val password: String
)

// Login response
data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String
)

// Refresh token response
data class RefreshTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String
)

// Token data
data class TokenData(
    val username: String,
    val email: String?
)

// API Error response
data class ApiErrorResponse(
    val detail: String,
    val message: String? = null,
    val code: String? = null
)
