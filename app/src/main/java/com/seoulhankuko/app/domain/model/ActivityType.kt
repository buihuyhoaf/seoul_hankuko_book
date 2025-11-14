package com.seoulhankuko.app.domain.model

enum class ActivityType(val apiValue: String) {
    LESSON("lesson"),
    SPEAKING("speaking"),
    LISTENING("listening");
    
    companion object {
        fun fromApiValue(value: String): ActivityType? {
            return values().find { it.apiValue == value }
        }
    }
}

