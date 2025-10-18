package com.seoulhankuko.app.data.api.model

import com.google.gson.annotations.SerializedName
import java.util.Date

// Base User model
data class User(
    val id: Int,
    val name: String,
    val username: String,
    val email: String,
    @SerializedName("profile_image_url")
    val profileImageUrl: String,
    val uuid: String,
    @SerializedName("created_at")
    val createdAt: Date,
    @SerializedName("updated_at")
    val updatedAt: Date?,
    @SerializedName("deleted_at")
    val deletedAt: Date?,
    @SerializedName("is_deleted")
    val isDeleted: Boolean,
    @SerializedName("is_superuser")
    val isSuperuser: Boolean,
    @SerializedName("tier_id")
    val tierId: Int?
)

// User creation request
data class UserCreateRequest(
    val name: String,
    val username: String,
    val email: String,
    val password: String
)

// User creation response
data class UserCreateResponse(
    val id: Int,
    val name: String,
    val username: String,
    val email: String,
    @SerializedName("profile_image_url")
    val profileImageUrl: String,
    val uuid: String,
    @SerializedName("created_at")
    val createdAt: Date,
    @SerializedName("updated_at")
    val updatedAt: Date?,
    @SerializedName("deleted_at")
    val deletedAt: Date?,
    @SerializedName("is_deleted")
    val isDeleted: Boolean,
    @SerializedName("is_superuser")
    val isSuperuser: Boolean,
    @SerializedName("tier_id")
    val tierId: Int?
)

// User update request
data class UserUpdateRequest(
    val name: String? = null,
    val username: String? = null,
    val email: String? = null
)

// User tier update request
data class UserTierUpdateRequest(
    @SerializedName("tier_id")
    val tierId: Int
)

// User read response
data class UserReadResponse(
    val id: Int,
    val username: String,
    val email: String,
    val picture: String?,
    val role: String,
    val exp: Int,
    @SerializedName("streak_days")
    val streakDays: Int,
    @SerializedName("created_at")
    val createdAt: String, // Changed to String to match backend timestamp format
    @SerializedName("tier_id")
    val tierId: Int?,
    @SerializedName("has_completed_entry_test")
    val hasCompletedEntryTest: Boolean,
    @SerializedName("current_course_id")
    val currentCourseId: Int?,
    @SerializedName("entry_test_score")
    val entryTestScore: Int?
)
