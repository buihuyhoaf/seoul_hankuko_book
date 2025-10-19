package com.seoulhankuko.app.domain.model

/**
 * Domain model representing a logged-in account that has been saved to device
 * Maps to LoggedAccountEntity but uses domain-friendly types
 */
data class LoggedAccount(
    val id: Int = 0, // Local database ID
    val userId: Int, // Backend user ID
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val lastLogin: Long,
    val isActive: Boolean = false
)

/**
 * Extension functions to convert between domain model and entity
 */
fun LoggedAccount.toEntity(): com.seoulhankuko.app.data.database.entities.LoggedAccountEntity {
    return com.seoulhankuko.app.data.database.entities.LoggedAccountEntity(
        id = this.id,
        userId = this.userId,
        email = this.email,
        displayName = this.displayName,
        photoUrl = this.photoUrl,
        accessToken = this.accessToken,
        refreshToken = this.refreshToken,
        lastLogin = this.lastLogin,
        isActive = this.isActive
    )
}

fun com.seoulhankuko.app.data.database.entities.LoggedAccountEntity.toDomain(): LoggedAccount {
    return LoggedAccount(
        id = this.id,
        userId = this.userId,
        email = this.email,
        displayName = this.displayName,
        photoUrl = this.photoUrl,
        accessToken = this.accessToken,
        refreshToken = this.refreshToken,
        lastLogin = this.lastLogin,
        isActive = this.isActive
    )
}
