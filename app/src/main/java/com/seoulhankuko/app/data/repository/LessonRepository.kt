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
                    Timber.d("Lesson has ${lessonDetail.quizzes.size} quizzes")
                    Timber.d("Quiz IDs: ${lessonDetail.quizzes.map { it.id }}")
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
        
        // Get user's quiz attempts to check completion status
        val completedQuizIds = getUserCompletedQuizIds(userId)
        Timber.d("User has completed ${completedQuizIds.size} quizzes")
        
        // Convert quizzes to challenges by getting detailed quiz data
        val challengesWithOptions = mutableListOf<ChallengeWithOptions>()
        
        Timber.d("Starting to convert ${lessonDetail.quizzes.size} quizzes to challenges")
        
        for (quiz in lessonDetail.quizzes) {
            try {
                Timber.d("Fetching detailed quiz data for quiz ${quiz.id}: ${quiz.title}")
                
                // Get detailed quiz data with questions and options
                val token = authRepository.getCurrentToken()
                val quizToken = if (token != null && token.isNotBlank()) "Bearer $token" else "Bearer "
                val quizResponse: Response<com.seoulhankuko.app.data.api.model.QuizDetailResponse> = 
                    apiService.getQuiz(quiz.id, quizToken)
                
                Timber.d("Quiz API response - Code: ${quizResponse.code()}, Success: ${quizResponse.isSuccessful}")
                
                if (quizResponse.isSuccessful) {
                    val quizDetail = quizResponse.body()
                    if (quizDetail != null) {
                        Timber.d("Successfully received quiz data with ${quizDetail.questions.size} questions")
                        
                        // Convert each question to a challenge
                        for (question in quizDetail.questions) {
                            val challenge = Challenge(
                                id = question.id,
                                lessonId = lessonDetail.id,
                                question = question.content,
                                type = ChallengeType.SELECT, // Default to SELECT type
                                order = question.orderIndex
                            )
                            
                            // Convert question options to challenge options
                            val options = question.options.map { option ->
                                ChallengeOption(
                                    id = option.id,
                                    challengeId = question.id,
                                    text = option.optionText,
                                    correct = option.isCorrect,
                                    audioSrc = question.audioUrl // Use audio URL from question
                                )
                            }
                            
                            Timber.d("Created challenge ${challenge.id} with ${options.size} options")
                            
                            // Check if challenge is completed based on quiz attempts
                            val completed = completedQuizIds.contains(quiz.id)
                            
                            challengesWithOptions.add(
                                ChallengeWithOptions(
                                    challenge = challenge,
                                    options = options,
                                    completed = completed
                                )
                            )
                        }
                    } else {
                        Timber.w("Quiz API response body is null for quiz ${quiz.id}")
                    }
                } else {
                    val errorBody = quizResponse.errorBody()?.string()
                    Timber.e("Quiz API failed for quiz ${quiz.id} - Code: ${quizResponse.code()}, Error: $errorBody")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while fetching quiz details for quiz ${quiz.id}")
                
                // If we can't get quiz details, create a simple challenge
                val challenge = Challenge(
                    id = quiz.id,
                    lessonId = lessonDetail.id,
                    question = quiz.title,
                    type = ChallengeType.SELECT,
                    order = quiz.orderIndex
                )
                
                val options = listOf(
                    ChallengeOption(
                        id = quiz.id * 100 + 1,
                        challengeId = quiz.id,
                        text = "Option A",
                        correct = true,
                        audioSrc = null
                    ),
                    ChallengeOption(
                        id = quiz.id * 100 + 2,
                        challengeId = quiz.id,
                        text = "Option B",
                        correct = false,
                        audioSrc = null
                    )
                )
                
                Timber.d("Created fallback challenge ${challenge.id} with ${options.size} options")
                
                // Check if challenge is completed based on quiz attempts
                val completed = completedQuizIds.contains(quiz.id)
                
                challengesWithOptions.add(
                    ChallengeWithOptions(
                        challenge = challenge,
                        options = options,
                        completed = completed
                    )
                )
            }
        }
        
        Timber.d("Successfully converted lesson to ${challengesWithOptions.size} challenges")
        Timber.d("Challenge questions: ${challengesWithOptions.map { it.challenge.question }}")
        return LessonWithChallenges(lesson, challengesWithOptions)
    }
    
    private suspend fun getUserCompletedQuizIds(userId: String): Set<Int> {
        return try {
            Timber.d("Fetching completed quiz IDs for user: $userId")
            
            // Get username from auth repository or use userId as fallback
            val username = getUsernameFromUserId(userId)
            
            val token = authRepository.getCurrentToken()
            val authToken = if (token != null && token.isNotBlank()) "Bearer $token" else null
            val response: Response<com.seoulhankuko.app.data.api.model.PaginatedResponse<com.seoulhankuko.app.data.api.model.QuizAttemptResponse>> = 
                apiService.getUserQuizAttempts(username, 1, 100, authToken)
            
            if (response.isSuccessful) {
                val quizAttempts = response.body()
                if (quizAttempts != null) {
                    val completedQuizIds = quizAttempts.items
                        .filter { it.score > 0.0 } // Consider quizzes with score > 0 as completed
                        .map { it.quizId }
                        .toSet()
                    
                    Timber.d("Found ${completedQuizIds.size} completed quizzes for user")
                    completedQuizIds
                } else {
                    Timber.w("Quiz attempts response body is null")
                    emptySet()
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e("Failed to fetch quiz attempts - Code: ${response.code()}, Error: $errorBody")
                emptySet()
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while fetching user quiz attempts")
            emptySet()
        }
    }
    
    private suspend fun getUsernameFromUserId(userId: String): String {
        return try {
            // Try to get current user info from API
            val token = authRepository.getCurrentToken()
            if (token != null) {
                val response = apiService.getCurrentUser("Bearer $token")
                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        Timber.d("Retrieved username: ${user.username} for userId: $userId")
                        return user.username
                    }
                }
            }
            
            // Fallback to userId if we can't get username
            Timber.w("Could not retrieve username for userId: $userId, using userId as fallback")
            userId
        } catch (e: Exception) {
            Timber.e(e, "Failed to get username for userId: $userId")
            userId
        }
    }
    
    suspend fun completeChallenge(userId: String, challengeId: Int) {
        val progress = ChallengeProgress(
            userId = userId,
            challengeId = challengeId,
            completed = true
        )
        challengeProgressDao.insertChallengeProgress(progress)
    }
}
