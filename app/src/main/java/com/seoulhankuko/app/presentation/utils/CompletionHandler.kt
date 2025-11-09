package com.seoulhankuko.app.presentation.utils

/**
 * Data class to hold completion information
 */
data class CompletionInfo(
    val isCompleted: Boolean,
    val lessonId: Int?,
    val completionType: CompletionType = CompletionType.QUIZ
)

/**
 * Enum to distinguish between different types of completions
 */
enum class CompletionType {
    QUIZ,
    EXERCISE,
    LESSON
}

