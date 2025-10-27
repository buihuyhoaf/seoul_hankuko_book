package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.api.model.LessonDetailResponse
import com.seoulhankuko.app.data.api.model.QuestionResponse
import com.seoulhankuko.app.data.api.model.QuizResponse
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.data.repository.AuthRepository
import com.seoulhankuko.app.data.database.daos.ChallengeDao
import com.seoulhankuko.app.data.database.daos.ChallengeOptionDao
import com.seoulhankuko.app.data.database.daos.ChallengeProgressDao
import com.seoulhankuko.app.data.database.daos.LessonDao
import com.seoulhankuko.app.data.database.entities.Challenge
import com.seoulhankuko.app.data.database.entities.ChallengeOption
import com.seoulhankuko.app.data.database.entities.ChallengeProgress
import com.seoulhankuko.app.data.database.entities.Lesson
import com.seoulhankuko.app.domain.model.ChallengeType
import com.seoulhankuko.app.domain.model.ChallengeWithOptions
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
    private val authRepository: AuthRepository,
    private val lessonDao: LessonDao,
    private val challengeDao: ChallengeDao,
    private val challengeOptionDao: ChallengeOptionDao,
    private val challengeProgressDao: ChallengeProgressDao
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
        
        // Convert API lesson to domain lesson
        val lesson = Lesson(
            id = lessonDetail.id,
            title = lessonDetail.title,
            unitId = lessonDetail.unitId,
            order = lessonDetail.orderIndex
        )
        
        // Insert lesson first to ensure foreign key constraint is satisfied
        try {
            lessonDao.insertLesson(lesson)
            Timber.d("Successfully inserted lesson ${lesson.id}")
        } catch (e: Exception) {
            Timber.w(e, "Lesson ${lesson.id} might already exist, continuing...")
        }
        
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
                    question.questionType.name.lowercase().contains("multiple") -> {
                        Timber.d("Question ${question.id}: Mapped to MULTIPLE_CHOICE")
                        ChallengeType.MULTIPLE_CHOICE
                    }
                    question.questionType.name.lowercase().contains("fill") -> {
                        Timber.d("Question ${question.id}: Mapped to FILL_IN_BLANK")
                        ChallengeType.FILL_IN_BLANK
                    }
                    question.questionType.name.lowercase().contains("true") -> {
                        Timber.d("Question ${question.id}: Mapped to TRUE_FALSE")
                        ChallengeType.TRUE_FALSE
                    }
                    question.questionType.name.lowercase().contains("audio") -> {
                        Timber.d("Question ${question.id}: Mapped to AUDIO_COMPREHENSION")
                        ChallengeType.AUDIO_COMPREHENSION
                    }
                    question.questionType.name.lowercase().contains("writing") -> {
                        Timber.d("Question ${question.id}: Mapped to WRITING_PRACTICE")
                        ChallengeType.WRITING_PRACTICE
                    }
                    question.questionType.name.lowercase().contains("reading") -> {
                        Timber.d("Question ${question.id}: Mapped to READING_COMPREHENSION")
                        ChallengeType.READING_COMPREHENSION
                    }
                    question.questionType.name.lowercase().contains("matching") -> {
                        Timber.d("Question ${question.id}: Mapped to MATCHING")
                        ChallengeType.MATCHING
                    }
                    question.questionType.name.lowercase().contains("pronunciation") -> {
                        Timber.d("Question ${question.id}: Mapped to PRONUNCIATION")
                        ChallengeType.PRONUNCIATION
                    }
                    else -> {
                        Timber.w("Question ${question.id}: Unknown question type '${question.questionType.name}', defaulting to MULTIPLE_CHOICE")
                        ChallengeType.MULTIPLE_CHOICE
                    }
                }
                
                val challenge = Challenge(
                    id = question.id,
                    lessonId = lessonDetail.id,
                    question = question.content,
                    type = challengeType,
                    order = question.orderIndex
                )
                
                // Insert challenge into database (with conflict resolution)
                try {
                    challengeDao.insertChallenge(challenge)
                    Timber.d("Successfully inserted challenge ${challenge.id}")
                } catch (e: Exception) {
                    Timber.w(e, "Challenge ${challenge.id} might already exist, continuing...")
                }
                
                // Convert question options to challenge options
                val options = question.options.map { option ->
                    ChallengeOption(
                        id = option.id,
                        challengeId = question.id,
                        text = option.optionText,
                        correct = option.isCorrect,
                        audioSrc = question.audioUrl
                    )
                }
                
                // Insert challenge options into database (with conflict resolution)
                options.forEach { option ->
                    try {
                        challengeOptionDao.insertOption(option)
                    } catch (e: Exception) {
                        Timber.w(e, "Challenge option ${option.id} might already exist, continuing...")
                    }
                }
                
                Timber.d("Created and inserted challenge ${challenge.id} with ${options.size} options")
                
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
    
    
    suspend fun completeChallenge(userId: String, challengeId: Int) {
        try {
            // First, verify that the challenge exists in the database
            val challenge = challengeDao.getChallengeById(challengeId)
            if (challenge == null) {
                Timber.w("Challenge with ID $challengeId not found in database, cannot create progress")
                return
            }
            
            val progress = ChallengeProgress(
                userId = userId,
                challengeId = challengeId,
                completed = true
            )
            challengeProgressDao.insertChallengeProgress(progress)
            Timber.d("Successfully completed challenge $challengeId for user $userId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to complete challenge $challengeId for user $userId")
            throw e
        }
    }
    
    /**
     * Updates lesson progress when a quiz or exercise is completed
     * This method calls the backend API to update progress
     */
    suspend fun updateLessonProgress(lessonId: Int, userId: String, token: String? = null): Boolean {
        return try {
            Timber.d("Updating lesson progress for lesson $lessonId, user $userId")
            
            // Call API to update lesson progress
            val authToken = if (token != null && token.isNotBlank()) "Bearer $token" else null
            val response = apiService.updateLessonProgress(lessonId, authToken)
            
            if (response.isSuccessful) {
                Timber.d("Successfully updated lesson progress for lesson $lessonId")
                true
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e("Failed to update lesson progress - Code: ${response.code()}, Error: $errorBody")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while updating lesson progress for lesson $lessonId")
            false
        }
    }
}
