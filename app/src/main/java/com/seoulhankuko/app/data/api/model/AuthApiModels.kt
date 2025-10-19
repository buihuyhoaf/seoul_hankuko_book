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
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("token_type")
    val tokenType: String
)

// Refresh token request
data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

// Refresh token response
data class RefreshTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("token_type")
    val tokenType: String
)

// Logout request
data class LogoutRequest(
    @SerializedName("refresh_token")
    val refreshToken: String? = null
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
