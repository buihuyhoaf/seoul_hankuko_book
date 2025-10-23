package com.seoulhankuko.app.data.api.model

import com.google.gson.annotations.SerializedName

// Course Management Models
data class CourseResponse(
    val id: Int,
    val title: String,
    val description: String?,
    @SerializedName("order_index")
    val orderIndex: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("units_count")
    val unitsCount: Int,
    val progress: CourseProgress?
)

data class CourseProgress(
    val id: Int,
    @SerializedName("course_id")
    val courseId: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    @SerializedName("progress_percent")
    val progressPercent: Int,
    @SerializedName("completed_at")
    val completedAt: String?,
    @SerializedName("created_at")
    val createdAt: String
)

data class CourseDetailResponse(
    val id: Int,
    val title: String,
    val description: String?,
    @SerializedName("order_index")
    val orderIndex: Int,
    @SerializedName("created_at")
    val createdAt: String,
    val units: List<UnitResponse>,
    val progress: CourseProgress?
)

data class UnitResponse(
    val id: Int,
    @SerializedName("course_id")
    val courseId: Int,
    val title: String,
    val description: String?,
    @SerializedName("order_index")
    val orderIndex: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("lessons_count")
    val lessonsCount: Int,
    val progress: UnitProgress?
)

data class UnitProgress(
    val id: Int,
    @SerializedName("unit_id")
    val unitId: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    @SerializedName("progress_percent")
    val progressPercent: Int,
    @SerializedName("completed_at")
    val completedAt: String?,
    @SerializedName("created_at")
    val createdAt: String
)

data class UnitDetailResponse(
    val id: Int,
    @SerializedName("course_id")
    val courseId: Int,
    val title: String,
    val description: String?,
    @SerializedName("order_index")
    val orderIndex: Int,
    @SerializedName("created_at")
    val createdAt: String,
    val lessons: List<LessonResponse>,
    val progress: UnitProgress?
)

data class LessonResponse(
    val id: Int,
    @SerializedName("unit_id")
    val unitId: Int,
    val title: String,
    val description: String?,
    @SerializedName("order_index")
    val orderIndex: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("quizzes_count")
    val quizzesCount: Int,
    @SerializedName("exercises_count")
    val exercisesCount: Int,
    val progress: LessonProgress?
)

data class LessonProgress(
    val id: Int,
    @SerializedName("lesson_id")
    val lessonId: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    @SerializedName("progress_percent")
    val progressPercent: Int,
    @SerializedName("completed_at")
    val completedAt: String?,
    @SerializedName("created_at")
    val createdAt: String
)

data class LessonDetailResponse(
    val id: Int,
    @SerializedName("unit_id")
    val unitId: Int,
    val title: String,
    val description: String?,
    @SerializedName("order_index")
    val orderIndex: Int,
    @SerializedName("created_at")
    val createdAt: String,
    val quizzes: List<QuizResponse>,
    val exercises: ExerciseResponse,
    val progress: LessonProgress?
)

// Quiz Management Models
data class QuizResponse(
    val id: Int,
    val title: String,
    val description: String?,
    val type: String,
    @SerializedName("order_index")
    val orderIndex: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("questions_count")
    val questionsCount: Int
)

data class QuizDetailResponse(
    val id: Int,
    @SerializedName("lesson_id")
    val lessonId: Int,
    val title: String,
    val description: String?,
    val type: String,
    @SerializedName("order_index")
    val orderIndex: Int,
    @SerializedName("created_at")
    val createdAt: String,
    val questions: List<QuestionResponse>
)

data class QuestionResponse(
    val id: Int,
    val content: String,
    @SerializedName("audio_url")
    val audioUrl: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    val explanation: String?,
    @SerializedName("order_index")
    val orderIndex: Int,
    @SerializedName("question_type")
    val questionType: QuestionTypeResponse,
    val options: List<QuestionOptionResponse>
)

data class QuestionTypeResponse(
    val id: Int,
    val name: String,
    val description: String?
)

data class QuestionOptionResponse(
    val id: Int,
    @SerializedName("option_text")
    val optionText: String,
    @SerializedName("is_correct")
    val isCorrect: Boolean
)

data class QuizAttemptResponse(
    val id: Int,
    @SerializedName("quiz_id")
    val quizId: Int,
    @SerializedName("quiz_title")
    val quizTitle: String?,
    val score: Double,
    @SerializedName("exp_earned")
    val expEarned: Int,
    @SerializedName("started_at")
    val startedAt: String,
    @SerializedName("completed_at")
    val completedAt: String?
)

data class QuestionResultResponse(
    @SerializedName("question_id")
    val questionId: Int,
    @SerializedName("user_answer")
    val userAnswer: String,
    @SerializedName("is_correct")
    val isCorrect: Boolean
)

// Exercise Models
data class ExerciseResponse(
    val listening: List<ListeningExerciseResponse>,
    val speaking: List<SpeakingExerciseResponse>,
    val writing: List<WritingExerciseResponse>
)

data class ListeningExerciseResponse(
    val id: Int,
    @SerializedName("lesson_id")
    val lessonId: Int,
    @SerializedName("audio_url")
    val audioUrl: String,
    val transcript: String?,
    val description: String?,
    @SerializedName("created_at")
    val createdAt: String
)

data class SpeakingExerciseResponse(
    val id: Int,
    @SerializedName("lesson_id")
    val lessonId: Int,
    val prompt: String,
    @SerializedName("sample_answer")
    val sampleAnswer: String?,
    @SerializedName("created_at")
    val createdAt: String
)

data class WritingExerciseResponse(
    val id: Int,
    @SerializedName("lesson_id")
    val lessonId: Int,
    val prompt: String,
    @SerializedName("sample_answer")
    val sampleAnswer: String?,
    @SerializedName("created_at")
    val createdAt: String
)

// User Progress Models
data class UserProgressResponse(
    @SerializedName("user_id")
    val userId: Int,
    val username: String,
    @SerializedName("total_exp")
    val totalExp: Int,
    @SerializedName("streak_days")
    val streakDays: Int,
    val courses: ProgressStats,
    val units: ProgressStats,
    val lessons: ProgressStats,
    val quizzes: QuizStats,
    @SerializedName("recent_exp_activity")
    val recentExpActivity: List<ExpLogResponse>
)

data class ProgressStats(
    val total: Int,
    val completed: Int,
    @SerializedName("progress_percent")
    val progressPercent: Double
)

data class QuizStats(
    @SerializedName("total_attempts")
    val totalAttempts: Int,
    @SerializedName("average_score")
    val averageScore: Double
)

data class CourseProgressResponse(
    val id: Int,
    @SerializedName("course_id")
    val courseId: Int,
    @SerializedName("course_title")
    val courseTitle: String?,
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    @SerializedName("progress_percent")
    val progressPercent: Int,
    @SerializedName("completed_at")
    val completedAt: String?,
    @SerializedName("created_at")
    val createdAt: String
)

data class ExpLogResponse(
    val id: Int,
    val source: String,
    val amount: Int,
    @SerializedName("created_at")
    val createdAt: String
)

data class StreakUpdateResponse(
    val message: String,
    @SerializedName("new_streak")
    val newStreak: Int,
    @SerializedName("streak_bonus_exp")
    val streakBonusExp: Int
)

// Daily Goals Models
data class DailyGoalsResponse(
    val date: String,
    @SerializedName("target_exp")
    val targetExp: Int,
    @SerializedName("current_exp")
    val currentExp: Int,
    @SerializedName("exp_completed")
    val expCompleted: Boolean,
    @SerializedName("target_lessons")
    val targetLessons: Int,
    @SerializedName("current_lessons")
    val currentLessons: Int,
    @SerializedName("lessons_completed")
    val lessonsCompleted: Boolean,
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    @SerializedName("progress_percent")
    val progressPercent: Double
)

data class DailyGoalsUpdateResponse(
    val message: String,
    @SerializedName("target_exp")
    val targetExp: Int,
    @SerializedName("target_lessons")
    val targetLessons: Int
)

// Badge Models
data class BadgeResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val icon_url: String?,
    val criteria: String?,
    @SerializedName("created_at")
    val createdAt: String
)

// Leaderboard Models
data class LeaderboardEntryResponse(
    val username: String,
    val exp: Int,
    @SerializedName("streak_days")
    val streakDays: Int,
    val rank: Int,
    @SerializedName("profile_image_url")
    val profileImageUrl: String?
)

// Friend Models
data class FriendResponse(
    val id: Int,
    val username: String,
    val status: String, // "accepted", "pending", "blocked"
    @SerializedName("profile_image_url")
    val profileImageUrl: String?,
    @SerializedName("created_at")
    val createdAt: String
)

// Entry Test Models
data class EntryTestQuestionOptionResponse(
    val id: Int,
    @SerializedName("option_text")
    val optionText: String,
    @SerializedName("is_correct")
    val isCorrect: Boolean
)

data class EntryTestQuestionResponse(
    val id: Int,
    val content: String,
    @SerializedName("audio_url")
    val audioUrl: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("correct_answer")
    val correctAnswer: String,
    val explanation: String?,
    @SerializedName("order_index")
    val orderIndex: Int,
    val options: List<EntryTestQuestionOptionResponse>
)

data class EntryTestResponse(
    val id: Int,
    val name: String,
    val description: String,
    @SerializedName("related_course_id")
    val relatedCourseId: Int,
    @SerializedName("created_at")
    val createdAt: String,
    val questions: List<EntryTestQuestionResponse>
)

data class EntryTestAnswerRequest(
    @SerializedName("question_id")
    val questionId: Int,
    @SerializedName("selected_option_id")
    val selectedOptionId: Int
)

data class EntryTestSubmissionRequest(
    val answers: List<EntryTestAnswerRequest>
)

data class EntryTestSubmissionResponse(
    val score: Float,
    @SerializedName("recommended_course_id")
    val recommendedCourseId: Int,
    @SerializedName("recommended_course_title")
    val recommendedCourseTitle: String,
    val message: String
)

data class EntryTestResultResponse(
    val id: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("entry_test_id")
    val entryTestId: Int,
    val score: Float,
    @SerializedName("recommended_course_id")
    val recommendedCourseId: Int,
    @SerializedName("completed_at")
    val completedAt: String
)
