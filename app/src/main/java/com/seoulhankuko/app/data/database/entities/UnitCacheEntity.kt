package com.seoulhankuko.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for caching unit data locally
 */
@Entity(
    tableName = "units_cache",
    foreignKeys = [
        ForeignKey(
            entity = CourseCacheEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["courseId"])]
)
data class UnitCacheEntity(
    @PrimaryKey
    val id: String,
    val courseId: String,
    val title: String,
    val description: String?,
    val orderIndex: Int,
    val createdAt: String?,
    val lessonsCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

