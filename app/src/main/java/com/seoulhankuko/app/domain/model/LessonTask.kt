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
    LISTENING,
    SPEAKING,
    WRITING,
    FINAL_QUIZ
}

/**
 * Extension functions for TaskType
 */
fun TaskType.getIcon(): String {
    return when (this) {
        TaskType.LISTENING -> "🎧"
        TaskType.SPEAKING -> "🎤"
        TaskType.WRITING -> "✍️"
        TaskType.FINAL_QUIZ -> "🎯"
    }
}

fun TaskType.getDisplayName(): String {
    return when (this) {
        TaskType.LISTENING -> "Listening"
        TaskType.SPEAKING -> "Speaking"
        TaskType.WRITING -> "Writing"
        TaskType.FINAL_QUIZ -> "Final Quiz"
    }
}

fun TaskType.getColor(): Long {
    return when (this) {
        TaskType.LISTENING -> 0xFF4DB6AC // teal
        TaskType.SPEAKING -> 0xFF81C784 // mint
        TaskType.WRITING -> 0xFFFFB74D // orange
        TaskType.FINAL_QUIZ -> 0xFFFF6F61 // coral
    }
}

