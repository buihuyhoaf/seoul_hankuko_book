package com.seoulhankuko.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey
    val userId: String,
    val userName: String = "User",
    val userImageSrc: String = "/mascot.svg",
    val activeCourseId: Int? = null,
    val hearts: Int = 5,
    val points: Int = 0
)

