package com.seoulhankuko.app.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Data models for Google Sign-In authentication
 */

/**
 * Request model for Google Sign-In API
 */
data class GoogleSignInRequest(
    @SerializedName("token")
    val token: String
)

/**
 * Response model for Google Sign-In API
 */
data class GoogleSignInResponse(
    @SerializedName("token")
    val token: String,
    @SerializedName("token_type")
    val tokenType: String = "bearer"
)

/**
 * User information model
 */
data class UserInfo(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    @SerializedName("is_premium")
    val isPremium: Boolean = false
)

/**
 * Google Sign-In result states
 */
sealed class GoogleSignInResult {
    object Loading : GoogleSignInResult()
    data class Success(val userInfo: UserInfo) : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
}

