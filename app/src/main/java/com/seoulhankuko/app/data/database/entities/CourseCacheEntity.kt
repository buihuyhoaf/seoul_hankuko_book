package com.seoulhankuko.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for caching course data locally
 */
@Entity(tableName = "courses_cache")
data class CourseCacheEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val orderIndex: Int,
    val createdAt: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)

