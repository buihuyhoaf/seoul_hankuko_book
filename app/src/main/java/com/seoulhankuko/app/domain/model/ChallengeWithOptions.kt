package com.seoulhankuko.app.domain.model

import com.seoulhankuko.app.data.database.entities.Challenge
import com.seoulhankuko.app.data.database.entities.ChallengeOption

data class ChallengeWithOptions(
    val challenge: Challenge,
    val options: List<ChallengeOption>,
    val completed: Boolean
)

