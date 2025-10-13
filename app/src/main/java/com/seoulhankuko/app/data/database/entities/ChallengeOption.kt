package com.seoulhankuko.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "challenge_options",
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
data class ChallengeOption(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val challengeId: Int,
    val text: String,
    val correct: Boolean,
    val imageSrc: String? = null,
    val audioSrc: String? = null
)
