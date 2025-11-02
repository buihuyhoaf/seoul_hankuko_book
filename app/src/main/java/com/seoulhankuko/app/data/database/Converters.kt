package com.seoulhankuko.app.data.database

import androidx.room.TypeConverter
import com.seoulhankuko.app.domain.model.QuestionType

class Converters {
    @TypeConverter
    fun fromQuestionType(type: QuestionType): String {
        return type.name
    }
    
    @TypeConverter
    fun toQuestionType(type: String): QuestionType {
        return QuestionType.valueOf(type)
    }
}

