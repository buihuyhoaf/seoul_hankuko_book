package com.seoulhankuko.app.data.api.model

import com.google.gson.annotations.SerializedName

// Paginated response wrapper
data class PaginatedResponse<T>(
    val data: List<T>?,  // ← Thay đổi từ items thành data
    val total: Int,
    val page: Int,
    @SerializedName("items_per_page")
    val itemsPerPage: Int,
    val pages: Int
)

// Tier model
data class Tier(
    val id: Int,
    val name: String,
    val description: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String?
)

// Tier creation request
data class TierCreateRequest(
    val name: String,
    val description: String? = null
)

// Tier read response
data class TierReadResponse(
    val id: Int,
    val name: String,
    val description: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String?
)

// Rate limit model
data class RateLimit(
    val id: Int,
    val name: String,
    val description: String?,
    @SerializedName("tier_id")
    val tierId: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String?
)

// Rate limit creation request
data class RateLimitCreateRequest(
    val name: String,
    val description: String? = null
)

// Rate limit read response
data class RateLimitReadResponse(
    val id: Int,
    val name: String,
    val description: String?,
    @SerializedName("tier_id")
    val tierId: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String?
)

// Post model (for Korean learning posts)
data class Post(
    val id: Int,
    val title: String,
    val content: String,
    val username: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String?
)

// Post creation request
data class PostCreateRequest(
    val title: String,
    val content: String
)

// Post read response
data class PostReadResponse(
    val id: Int,
    val title: String,
    val content: String,
    val username: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String?
)

// Task/Job model
data class Task(
    val id: String,
    val status: String,
    val result: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("completed_at")
    val completedAt: String?
)

// Task creation request
data class TaskCreateRequest(
    val message: String
)

// Task creation response
data class TaskCreateResponse(
    val id: String
)
