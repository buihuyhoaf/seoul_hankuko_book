package com.seoulhankuko.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_subscription")
data class UserSubscription(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String,
    val stripeCustomerId: String,
    val stripeSubscriptionId: String,
    val stripePriceId: String,
    val stripeCurrentPeriodEnd: Long
)

