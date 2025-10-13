package com.seoulhankuko.app.data.database

import androidx.room.TypeConverter
import com.seoulhankuko.app.domain.model.ChallengeType

class Converters {
    @TypeConverter
    fun fromChallengeType(type: ChallengeType): String {
        return type.name
    }
    
    @TypeConverter
    fun toChallengeType(type: String): ChallengeType {
        return ChallengeType.valueOf(type)
    }
}

