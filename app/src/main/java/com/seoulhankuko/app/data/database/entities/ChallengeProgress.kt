package com.seoulhankuko.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "challenge_progress",
    foreignKeys = [
        ForeignKey(
            entity = Challenge::class,
            parentColumns = ["id"],
            childColumns = ["challengeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["challengeId"])]
)
data class ChallengeProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String,
    val challengeId: Int,
    val completed: Boolean = false
)
