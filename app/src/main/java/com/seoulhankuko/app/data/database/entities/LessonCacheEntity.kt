package com.seoulhankuko.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for caching lesson data locally
 */
@Entity(
    tableName = "lessons_cache",
    foreignKeys = [
        ForeignKey(
            entity = UnitCacheEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["unitId"])]
)
data class LessonCacheEntity(
    @PrimaryKey
    val id: String,
    val unitId: String,
    val title: String,
    val description: String?,
    val orderIndex: Int,
    val createdAt: String?,
    val quizzesCount: Int = 0,
    val exercisesCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

