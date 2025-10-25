package com.seoulhankuko.app.presentation.utils

import org.junit.Test
import org.junit.Assert.*

/**
 * Test class for CompletionHandler utility
 */
class CompletionHandlerTest {

    @Test
    fun `completion info should hold correct values`() {
        // Given
        val isCompleted = true
        val lessonId = 123
        val completionType = CompletionType.QUIZ

        // When
        val completionInfo = CompletionInfo(
            isCompleted = isCompleted,
            lessonId = lessonId,
            completionType = completionType
        )

        // Then
        assertEquals(isCompleted, completionInfo.isCompleted)
        assertEquals(lessonId, completionInfo.lessonId)
        assertEquals(completionType, completionInfo.completionType)
    }

    @Test
    fun `completion info should have default completion type`() {
        // Given
        val isCompleted = true
        val lessonId = 456

        // When
        val completionInfo = CompletionInfo(
            isCompleted = isCompleted,
            lessonId = lessonId
        )

        // Then
        assertEquals(CompletionType.QUIZ, completionInfo.completionType)
    }

    @Test
    fun `completion type enum should have correct values`() {
        // Then
        assertEquals("QUIZ", CompletionType.QUIZ.name)
        assertEquals("EXERCISE", CompletionType.EXERCISE.name)
        assertEquals("LESSON", CompletionType.LESSON.name)
    }
}
