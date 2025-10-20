package com.seoulhankuko.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing logged-in accounts locally
 * Maps to the requirements: user_id, email, display_name, photo_url, access_token, refresh_token, last_login, is_active
 */
@Entity(tableName = "logged_accounts")
data class LoggedAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val userId: Int, // From backend
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val lastLogin: Long,
    val isActive: Boolean = false
)


