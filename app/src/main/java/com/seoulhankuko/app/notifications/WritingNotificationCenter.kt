package com.seoulhankuko.app.notifications

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

object WritingNotificationCenter {
    const val TYPE_WRITING_GRADED = "writing_graded"
    const val EXTRA_TARGET_LESSON_ID = "extra_target_lesson_id"
    const val EXTRA_SUBMISSION_ID = "extra_submission_id"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _events = MutableSharedFlow<WritingNotificationEvent>(
        replay = 0,
        extraBufferCapacity = 8
    )
    val events: SharedFlow<WritingNotificationEvent> = _events.asSharedFlow()

    fun publish(event: WritingNotificationEvent) {
        val emitted = _events.tryEmit(event)
        if (!emitted) {
            scope.launch {
                _events.emit(event)
            }
        }
        Timber.d("Published writing notification event: %s", event)
    }
}

sealed interface WritingNotificationEvent {
    data class WritingGraded(
        val lessonId: String,
        val submissionId: String?,
        val title: String?,
        val body: String?
    ) : WritingNotificationEvent
}


