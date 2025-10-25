package com.seoulhankuko.app.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.seoulhankuko.app.presentation.viewmodel.LessonViewModel
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Handles automatic navigation and progress update when quizzes or exercises are completed.
 * This provides a consistent experience across all completion flows.
 */
@Composable
fun useCompletionHandler(
    isCompleted: Boolean,
    lessonId: Int?,
    onNavigateBack: () -> Unit,
    delayMs: Long = 2000L,
    lessonViewModel: LessonViewModel = hiltViewModel()
) {
    var hasTriggeredCompletion by remember { mutableStateOf(false) }
    
    LaunchedEffect(isCompleted, lessonId) {
        if (isCompleted && lessonId != null && !hasTriggeredCompletion) {
            hasTriggeredCompletion = true
            
            Timber.d("Quiz/Exercise completed for lesson $lessonId, updating progress...")
            
            try {
                // Update lesson progress
                lessonViewModel.updateProgress(lessonId)
                
                // Show completion message briefly, then navigate back
                delay(delayMs)
                
                Timber.d("Navigating back to lesson screen...")
                onNavigateBack()
            } catch (e: Exception) {
                Timber.e(e, "Error during completion handling")
                // Still navigate back even if progress update fails
                onNavigateBack()
            }
        }
    }
}

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
