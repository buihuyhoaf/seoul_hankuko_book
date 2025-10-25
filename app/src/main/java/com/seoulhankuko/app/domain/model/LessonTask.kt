package com.seoulhankuko.app.domain.model

/**
 * Represents a task within a lesson (Quiz, Listening, Speaking, Writing)
 */
data class LessonTask(
    val id: Int,
    val type: TaskType,
    val title: String,
    val description: String,
    val completed: Boolean = false,
    val unlocked: Boolean = false,
    val orderIndex: Int = 0
)

/**
 * Types of tasks available in a lesson
 */
enum class TaskType {
    QUIZ,
    LISTENING,
    SPEAKING,
    WRITING
}

/**
 * Extension functions for TaskType
 */
fun TaskType.getIcon(): String {
    return when (this) {
        TaskType.QUIZ -> "🎯"
        TaskType.LISTENING -> "🎧"
        TaskType.SPEAKING -> "🎤"
        TaskType.WRITING -> "✍️"
    }
}

fun TaskType.getDisplayName(): String {
    return when (this) {
        TaskType.QUIZ -> "Quiz"
        TaskType.LISTENING -> "Listening"
        TaskType.SPEAKING -> "Speaking"
        TaskType.WRITING -> "Writing"
    }
}

fun TaskType.getColor(): Long {
    return when (this) {
        TaskType.QUIZ -> 0xFFFF6F61 // coral
        TaskType.LISTENING -> 0xFF4DB6AC // teal
        TaskType.SPEAKING -> 0xFF81C784 // mint
        TaskType.WRITING -> 0xFFFFB74D // orange
    }
}
