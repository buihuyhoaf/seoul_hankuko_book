package com.seoulhankuko.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.seoulhankuko.app.domain.model.ChallengeType

@Entity(
    tableName = "challenges",
    foreignKeys = [
        ForeignKey(
            entity = Lesson::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["lessonId"])]
)
data class Challenge(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val lessonId: Int,
    val type: ChallengeType,
    val question: String,
    val order: Int
)
