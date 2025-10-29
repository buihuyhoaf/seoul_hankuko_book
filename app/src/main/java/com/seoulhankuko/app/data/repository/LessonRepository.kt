package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.api.model.LessonDetailResponse
import com.seoulhankuko.app.data.api.model.QuestionResponse
import com.seoulhankuko.app.data.api.model.QuizResponse
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.data.repository.AuthRepository
import com.seoulhankuko.app.domain.model.ChallengeType
import com.seoulhankuko.app.domain.model.ChallengeWithOptions
import com.seoulhankuko.app.domain.model.ChallengeLite
import com.seoulhankuko.app.domain.model.ChallengeOptionLite
import com.seoulhankuko.app.domain.model.LessonLite
import com.seoulhankuko.app.domain.model.LessonWithChallenges
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LessonRepository @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) {
    suspend fun getLessonWithChallenges(lessonId: Int, userId: String, token: String? = null): LessonWithChallenges? {
        return try {
            Timber.d("Fetching lesson $lessonId for user $userId")
            Timber.d("Using token: ${token?.take(20)}...")
            
            // Call API to get lesson data from backend
            val authToken = if (token != null && token.isNotBlank()) "Bearer $token" else null
            Timber.d("Auth token format: ${authToken?.take(30)}...")
            val response: Response<LessonDetailResponse> = apiService.getLesson(lessonId, authToken)
            
            Timber.d("Lesson API response - Code: ${response.code()}, Success: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val lessonDetail = response.body()
                if (lessonDetail != null) {
                    Timber.d("Successfully received lesson data: ${lessonDetail.title}")
                    Timber.d("Lesson has ${lessonDetail.questions.size} questions")
                    convertApiResponseToLessonWithChallenges(lessonDetail, userId)
                } else {
                    Timber.w("Lesson API response body is null")
                    null
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e("Lesson API failed - Code: ${response.code()}, Error: $errorBody")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while fetching lesson $lessonId")
            null
        }
    }
    
    private suspend fun convertApiResponseToLessonWithChallenges(
        lessonDetail: LessonDetailResponse, 
        userId: String
    ): LessonWithChallenges {
        Timber.d("Converting lesson data to challenges for lesson: ${lessonDetail.title}")
        
        // Convert API lesson to domain-only model (no DB persistence)
        val lesson = LessonLite(
            id = lessonDetail.id,
            title = lessonDetail.title,
            unitId = lessonDetail.unitId,
            order = lessonDetail.orderIndex
        )
        
        // Convert questions to challenges
        val challengesWithOptions = mutableListOf<ChallengeWithOptions>()
        
        Timber.d("Starting to convert ${lessonDetail.questions.size} questions to challenges")
        
        for (question in lessonDetail.questions) {
            try {
                Timber.d("Processing question ${question.id}: ${question.content.take(50)}...")
                Timber.d("Question ${question.id}: options count = ${question.options.size}")
                question.options.forEachIndexed { index, option ->
                    Timber.d("Question ${question.id} Option $index: id=${option.id}, text='${option.optionText}', correct=${option.isCorrect}")
                }
                
                // Determine challenge type based on question type
                val challengeType = when {
                    question.questionType.lowercase().contains("multiple") -> {
                        Timber.d("Question ${question.id}: Mapped to MULTIPLE_CHOICE")
                        com.seoulhankuko.app.domain.model.ChallengeType.MULTIPLE_CHOICE
                    }
                    question.questionType.lowercase().contains("fill") -> {
                        Timber.d("Question ${question.id}: Mapped to FILL_IN_BLANK")
                        com.seoulhankuko.app.domain.model.ChallengeType.FILL_IN_BLANK
                    }
                    question.questionType.lowercase().contains("true") -> {
                        Timber.d("Question ${question.id}: Mapped to TRUE_FALSE")
                        com.seoulhankuko.app.domain.model.ChallengeType.TRUE_FALSE
                    }
                    question.questionType.lowercase().contains("audio") -> {
                        Timber.d("Question ${question.id}: Mapped to AUDIO_COMPREHENSION")
                        com.seoulhankuko.app.domain.model.ChallengeType.AUDIO_COMPREHENSION
                    }
                    question.questionType.lowercase().contains("writing") -> {
                        Timber.d("Question ${question.id}: Mapped to WRITING_PRACTICE")
                        com.seoulhankuko.app.domain.model.ChallengeType.WRITING_PRACTICE
                    }
                    question.questionType.lowercase().contains("reading") -> {
                        Timber.d("Question ${question.id}: Mapped to READING_COMPREHENSION")
                        com.seoulhankuko.app.domain.model.ChallengeType.READING_COMPREHENSION
                    }
                    question.questionType.lowercase().contains("matching") -> {
                        Timber.d("Question ${question.id}: Mapped to MATCHING")
                        com.seoulhankuko.app.domain.model.ChallengeType.MATCHING
                    }
                    question.questionType.lowercase().contains("pronunciation") -> {
                        Timber.d("Question ${question.id}: Mapped to PRONUNCIATION")
                        com.seoulhankuko.app.domain.model.ChallengeType.PRONUNCIATION
                    }
                    else -> {
                        Timber.w("Question ${question.id}: Unknown question type '${question.questionType}', defaulting to MULTIPLE_CHOICE")
                        com.seoulhankuko.app.domain.model.ChallengeType.MULTIPLE_CHOICE
                    }
                }
                
                val challenge = ChallengeLite(
                    id = question.id,
                    lessonId = lessonDetail.id,
                    question = question.content,
                    type = challengeType,
                    order = question.orderIndex
                )
                
                // Convert question options to challenge options
                val options = question.options.map { option ->
                    ChallengeOptionLite(
                        id = option.id,
                        challengeId = question.id,
                        text = option.optionText,
                        correct = option.isCorrect,
                        audioSrc = question.audioUrl
                    )
                }
                
                Timber.d("Prepared challenge ${challenge.id} with ${options.size} options")
                
                // For now, assume challenges are not completed
                val completed = false
                
                challengesWithOptions.add(
                    ChallengeWithOptions(
                        challenge = challenge,
                        options = options,
                        completed = completed
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception while processing question ${question.id}")
            }
        }
        
        Timber.d("Successfully converted lesson to ${challengesWithOptions.size} challenges")
        Timber.d("Challenge questions: ${challengesWithOptions.map { it.challenge.question }}")
        return LessonWithChallenges(lesson, challengesWithOptions)
    }
    
    suspend fun updateLessonProgress(lessonId: Int, token: String? = null): Result<Map<String, Any>> {
        return try {
            val authToken = if (token != null && token.isNotBlank()) "Bearer $token" else null
            val response = apiService.updateLessonProgress(lessonId, authToken)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Timber.d("Successfully updated lesson progress for lesson $lessonId")
                    Result.success(body)
                } else {
                    Timber.w("Lesson progress update response body is null")
                    Result.failure(Exception("Response body is null"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e("Failed to update lesson progress - Code: ${response.code()}, Error: $errorBody")
                Result.failure(Exception("Failed to update progress: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while updating lesson progress for lesson $lessonId")
            Result.failure(e)
        }
    }
}
