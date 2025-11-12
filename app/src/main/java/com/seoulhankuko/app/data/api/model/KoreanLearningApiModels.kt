package com.seoulhankuko.app.data.api.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// Course Management Models
data class CourseResponse(
    val id: String,
    val title: String,
    val description: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("order_index")
    val orderIndex: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("units_count")
    val unitsCount: Int,
    val progress: CourseProgress?
)

data class CourseProgress(
    val id: String,
    @SerializedName("course_id")
    val courseId: String,
    @SerializedName("user_id")
    val userId: String,
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
    val id: String,
    val title: String,
    val description: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("order_index")
    val orderIndex: Int,
    @SerializedName("created_at")
    val createdAt: String,
    val units: List<UnitResponse>,
    val progress: CourseProgress?
)

data class UnitResponse(
    val id: String,
    @SerializedName("course_id")
    val courseId: String,
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
    val id: String,
    @SerializedName("unit_id")
    val unitId: String,
    @SerializedName("user_id")
    val userId: String,
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
    val id: String,
    @SerializedName("course_id")
    val courseId: String,
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
    val id: String,
    @SerializedName("unit_id")
    val unitId: String,
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
    val id: String,
    @SerializedName("lesson_id")
    val lessonId: String,
    @SerializedName("user_id")
    val userId: String,
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
    val id: String,
    @SerializedName("unit_id")
    val unitId: String,
    val title: String,
    val description: String?,
    @SerializedName("order_index")
    val orderIndex: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("target_exp")
    val targetExp: Int? = null,
    @SerializedName("exp_per_question")
    val expPerQuestion: Float? = null,
    @SerializedName("has_more_questions")
    val hasMoreQuestions: Boolean? = null,
    val questions: List<QuestionResponse>,
    val exercises: List<ExerciseResponse>,
    val progress: LessonProgress?
)

// Quiz Management Models
data class QuizResponse(
    val id: String,
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
    val id: String,
    @SerializedName("lesson_id")
    val lessonId: String,
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
    val id: String,
    val content: String,
    @SerializedName("audio_url")
    val audioUrl: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    val explanation: String?,
    @SerializedName("question_metadata")
    val metadata: QuestionMetadataResponse? = null,
    @SerializedName("order_index")
    val orderIndex: Int,
    @SerializedName("question_type")
    val questionType: String,
    @SerializedName("question_type_id")
    val questionTypeId: String? = null,
    val options: List<QuestionOptionResponse> = emptyList(),
    @SerializedName("matching_pairs")
    val matchingPairs: List<QuestionMatchingPairResponse>? = null,
    @SerializedName("sentence_order")
    val sentenceOrder: QuestionSentenceOrderResponse? = null,
    @SerializedName("audio_comprehension")
    val audioComprehension: QuestionAudioComprehensionResponse? = null,
    val pronunciation: QuestionPronunciationResponse? = null,
    val blank: QuestionBlankResponse? = null
)

data class QuestionMetadataResponse(
    val choices: List<String>? = null,
    @SerializedName("pairs")
    val pairs: List<QuestionMetadataPairResponse>? = null
)

data class QuestionMetadataPairResponse(
    val left: String?,
    val right: String?
)

data class QuestionMatchingPairResponse(
    val id: String,
    @SerializedName("left_text")
    val leftText: String?,
    @SerializedName("left_media")
    val leftMedia: JsonElement? = null,
    @SerializedName("right_text")
    val rightText: String?,
    @SerializedName("right_media")
    val rightMedia: JsonElement? = null,
    @SerializedName("sort_order")
    val sortOrder: Int
)

data class QuestionSentenceOrderResponse(
    val id: String,
    @SerializedName("correct_sequence")
    val correctSequence: List<String>
)

data class QuestionAudioComprehensionResponse(
    val id: String,
    val transcript: String?,
    @SerializedName("tts_config")
    val ttsConfig: JsonElement? = null
)

data class QuestionPronunciationResponse(
    val id: String,
    @SerializedName("target_phrase")
    val targetPhrase: String,
    @SerializedName("reference_audio_url")
    val referenceAudioUrl: String?,
    @SerializedName("tts_config")
    val ttsConfig: JsonElement? = null
)

data class QuestionBlankResponse(
    val id: String,
    @SerializedName("correct_answer")
    val correctAnswer: String?,
    @SerializedName("case_sensitive")
    val caseSensitive: Boolean
)

data class PronunciationEvaluationResponse(
    val transcript: String,
    val score: Float,
    val passed: Boolean
)

data class QuestionTypeResponse(
    val id: String,
    val name: String,
    val description: String?
)

data class QuestionOptionResponse(
    val id: String,
    @SerializedName("option_text")
    val optionText: String,
    @SerializedName("is_correct")
    val isCorrect: Boolean
)

data class QuizAttemptResponse(
    val id: String,
    @SerializedName("quiz_id")
    val quizId: String,
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
    val questionId: String,
    @SerializedName("user_answer")
    val userAnswer: String,
    @SerializedName("is_correct")
    val isCorrect: Boolean
)

// Exercise Question Models
data class ExerciseQuestionOptionResponse(
    val id: String,
    @SerializedName("question_id")
    val questionId: String,
    @SerializedName("option_text")
    val optionText: String,
    @SerializedName("is_correct")
    val isCorrect: Boolean,
    @SerializedName("order_index")
    val orderIndex: Int,
    @SerializedName("created_at")
    val createdAt: String
)

data class ExerciseQuestionResponse(
    val id: String,
    @SerializedName("exercise_id")
    val exerciseId: String,
    @SerializedName("question_text")
    val questionText: String,
    val explanation: String?,
    @SerializedName("order_index")
    val orderIndex: Int,
    val options: List<ExerciseQuestionOptionResponse>,
    @SerializedName("created_at")
    val createdAt: String
)

// Exercise Models
data class ExerciseResponse(
    val id: String,
    val type: String,
    val title: String?,
    val content: String?,
    @SerializedName("audio_url")
    val audioUrl: String? = null, // Deprecated - use text_to_speak instead
    @SerializedName("text_to_speak")
    val textToSpeech: String?,
    val transcript: String?,
    val prompt: String?,
    @SerializedName("sample_answer")
    val sampleAnswer: String?,
    @SerializedName("order_index")
    val orderIndex: Int,
    @SerializedName("created_at")
    val createdAt: String,
    val questions: List<ExerciseQuestionResponse> = emptyList()
)

// Legacy exercise models for backward compatibility
data class ListeningExerciseResponse(
    val id: String,
    @SerializedName("lesson_id")
    val lessonId: String,
    @SerializedName("audio_url")
    val audioUrl: String,
    val transcript: String?,
    val description: String?,
    @SerializedName("created_at")
    val createdAt: String
)

data class SpeakingExerciseResponse(
    val id: String,
    @SerializedName("lesson_id")
    val lessonId: String,
    val prompt: String,
    @SerializedName("sample_answer")
    val sampleAnswer: String?,
    @SerializedName("created_at")
    val createdAt: String
)

data class WritingExerciseResponse(
    val id: String,
    @SerializedName("lesson_id")
    val lessonId: String,
    val prompt: String,
    @SerializedName("sample_answer")
    val sampleAnswer: String?,
    @SerializedName("created_at")
    val createdAt: String
)

// User Progress Models
data class UserProgressResponse(
    @SerializedName("user_id")
    val userId: String,
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
    val id: String,
    @SerializedName("course_id")
    val courseId: String,
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
    val id: String,
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
    val id: String,
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
    val id: String,
    val username: String,
    val status: String, // "accepted", "pending", "blocked"
    @SerializedName("profile_image_url")
    val profileImageUrl: String?,
    @SerializedName("created_at")
    val createdAt: String
)

// Entry Test Models
data class EntryTestQuestionOptionResponse(
    val id: String,
    @SerializedName("option_text")
    val optionText: String,
    @SerializedName("is_correct")
    val isCorrect: Boolean
)

data class EntryTestQuestionResponse(
    val id: String,
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
    val id: String,
    val name: String,
    val description: String,
    @SerializedName("related_course_id")
    val relatedCourseId: String,
    @SerializedName("created_at")
    val createdAt: String,
    val questions: List<EntryTestQuestionResponse>
)

data class EntryTestAnswerRequest(
    @SerializedName("question_id")
    val questionId: String,
    @SerializedName("selected_option_id")
    val selectedOptionId: String
)

data class EntryTestSubmissionRequest(
    val answers: List<EntryTestAnswerRequest>
)

data class EntryTestSubmissionResponse(
    val score: Float,
    @SerializedName("recommended_course_id")
    val recommendedCourseId: String,
    @SerializedName("recommended_course_title")
    val recommendedCourseTitle: String,
    val message: String
)

data class EntryTestResultResponse(
    val id: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("entry_test_id")
    val entryTestId: String,
    val score: Float,
    @SerializedName("recommended_course_id")
    val recommendedCourseId: String,
    @SerializedName("completed_at")
    val completedAt: String
)

// Practice question submission requests
data class PracticeSelectedOptionRequest(
    @SerializedName("selected_option_id") val selectedOptionId: String
)

data class PracticeTextAnswerRequest(
    @SerializedName("answer") val answer: String
)

// Exercise submission request
data class ExerciseSubmissionRequest(
    @SerializedName("response") val response: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("audio_url") val audioUrl: String? = null,
    @SerializedName("selected_answers") val selectedAnswers: Map<String, String>? = null,
    @SerializedName("mode") val mode: String? = null
)

data class WritingResultResponse(
    @SerializedName("submission_id") val submissionId: String,
    @SerializedName("exercise_id") val exerciseId: String,
    @SerializedName("mode") val mode: String,
    @SerializedName("status") val status: String,
    @SerializedName("text") val text: String? = null,
    @SerializedName("ai_score") val aiScore: Float? = null,
    @SerializedName("ai_feedback") val aiFeedback: String? = null,
    @SerializedName("teacher_spelling_score") val teacherSpellingScore: Float? = null,
    @SerializedName("teacher_grammar_score") val teacherGrammarScore: Float? = null,
    @SerializedName("teacher_structure_score") val teacherStructureScore: Float? = null,
    @SerializedName("teacher_vocabulary_score") val teacherVocabularyScore: Float? = null,
    @SerializedName("teacher_feedback") val teacherFeedback: String? = null,
    @SerializedName("final_score") val finalScore: Float? = null,
    @SerializedName("submitted_at") val submittedAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class WritingResultsResponse(
    @SerializedName("lesson_id") val lessonId: String,
    @SerializedName("submissions") val submissions: List<WritingResultResponse> = emptyList()
)

data class LessonProgressUpdateRequest(
    @SerializedName("question_exp") val questionExp: Int,
    @SerializedName("listening_exp") val listeningExp: Int,
    @SerializedName("speaking_exp") val speakingExp: Int,
    @SerializedName("writing_exp") val writingExp: Int
)
