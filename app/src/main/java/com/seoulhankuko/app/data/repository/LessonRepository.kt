package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.api.model.LessonDetailResponse
import com.seoulhankuko.app.data.api.model.PracticeSelectedOptionRequest
import com.seoulhankuko.app.data.api.model.PracticeTextAnswerRequest
import com.seoulhankuko.app.data.api.model.ExerciseSubmissionRequest
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.domain.model.ChallengeLite
import com.seoulhankuko.app.domain.model.ChallengeOptionLite
import com.seoulhankuko.app.domain.model.QuestionType
import com.seoulhankuko.app.domain.model.ChallengeWithOptions
import com.seoulhankuko.app.domain.model.LessonLite
import com.seoulhankuko.app.domain.model.LessonWithChallenges
import com.seoulhankuko.app.domain.model.ExerciseLite
import com.seoulhankuko.app.domain.model.QuestionAudioComprehension
import com.seoulhankuko.app.domain.model.QuestionBlank
import com.seoulhankuko.app.domain.model.QuestionMatchingPair
import com.seoulhankuko.app.domain.model.QuestionMetadata
import com.seoulhankuko.app.domain.model.QuestionMetadataPair
import com.seoulhankuko.app.domain.model.QuestionPronunciation
import com.seoulhankuko.app.domain.model.QuestionSentenceOrder
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LessonRepository @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) {
    /**
     * Map question_type_id to QuestionType enum
     * Mapping based on BE question_types table:
     * ID 1 -> MULTIPLE_CHOICE
     * ID 2 -> BLANK
     * ID 3 -> MATCHING
     * ID 4 -> AUDIO_COMPREHENSION
     * ID 5 -> PRONUNCIATION
     * ID 6 -> SENTENCE_ORDER
     * ID 7 -> IMAGE_SELECTION
     */
    private fun getQuestionType(questionTypeId: String?, questionTypeCode: String?): QuestionType {
        // Prefer explicit code from API if provided; fallback to legacy numeric mapping if present in id string
        when (questionTypeCode?.uppercase()) {
            "MULTIPLE_CHOICE" -> return QuestionType.MULTIPLE_CHOICE
            "BLANK" -> return QuestionType.BLANK
            "MATCHING" -> return QuestionType.MATCHING
            "AUDIO_COMPREHENSION" -> return QuestionType.AUDIO_COMPREHENSION
            "PRONUNCIATION" -> return QuestionType.PRONUNCIATION
            "SENTENCE_ORDER" -> return QuestionType.SENTENCE_ORDER
            "IMAGE_SELECTION" -> return QuestionType.IMAGE_SELECTION
        }

        // Legacy fallback: if BE still sends numeric-like ids as strings
        val numericId = questionTypeId?.toIntOrNull()
        if (numericId != null) {
            return when (numericId) {
                1 -> QuestionType.MULTIPLE_CHOICE
                2 -> QuestionType.BLANK
                3 -> QuestionType.MATCHING
                4 -> QuestionType.AUDIO_COMPREHENSION
                5 -> QuestionType.PRONUNCIATION
                6 -> QuestionType.SENTENCE_ORDER
                7 -> QuestionType.IMAGE_SELECTION
                else -> {
                    Timber.w("Unknown question_type_id: $questionTypeId, defaulting to MULTIPLE_CHOICE")
                    QuestionType.MULTIPLE_CHOICE
                }
            }
        }

        Timber.w("Unknown question type. id=$questionTypeId code=$questionTypeCode. Defaulting to MULTIPLE_CHOICE")
        return QuestionType.MULTIPLE_CHOICE
    }
    
    /**
     * Map question_type_id to QuestionType string name for logging
     */
    private fun getQuestionTypeName(questionTypeId: String?, questionTypeCode: String?): String {
        return getQuestionType(questionTypeId, questionTypeCode).name
    }
    suspend fun getLessonWithChallenges(lessonId: String, userId: String, token: String? = null): LessonWithChallenges? {
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
                    
                    // Log lesson summary
                    Timber.d("=== Lesson Summary ===")
                    Timber.d("Lesson ID: ${lessonDetail.id}")
                    Timber.d("Lesson Title: ${lessonDetail.title}")
                    Timber.d("Total Questions: ${lessonDetail.questions.size}")
                    Timber.d("Total Exercises: ${lessonDetail.exercises.size}")
                    
                    // Log each question with type
                    Timber.d("=== Questions ===")
                    lessonDetail.questions.forEach { question ->
                        val typeName = getQuestionTypeName(question.questionTypeId, question.questionType)
                        Timber.d("Question ID ${question.id}: ${question.content} - type: $typeName (question_type_id: ${question.questionTypeId})")
                    }
                    
                    // Log each exercise
                    Timber.d("=== Exercises ===")
                    lessonDetail.exercises.forEachIndexed { index, exercise ->
                        Timber.d("Exercise $index: id=${exercise.id}, type=${exercise.type}, title=${exercise.title}")
                    }
                    
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
    
    suspend fun submitPracticeQuestion(
        lessonId: String,
        questionId: String,
        token: String,
        selectedOptionId: String? = null,
        textAnswer: String? = null
    ): Result<Map<String, Any>> {
        return try {
            val authHeader = "Bearer $token"
            val response: Response<Map<String, Any>> = when {
                selectedOptionId != null -> {
                    apiService.submitPracticeQuestionSelectedOption(
                        lessonId, questionId, authHeader,
                        PracticeSelectedOptionRequest(selectedOptionId)
                    )
                }
                textAnswer != null -> {
                    apiService.submitPracticeQuestionTextAnswer(
                        lessonId, questionId, authHeader,
                        PracticeTextAnswerRequest(textAnswer)
                    )
                }
                else -> {
                    throw IllegalArgumentException("Either selectedOptionId or textAnswer must be provided")
                }
            }
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyMap())
            } else {
                val error = response.errorBody()?.string()
                Timber.e("Submit practice question failed: code=${response.code()}, error=$error")
                Result.failure(Exception("Submit failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception submitting practice question")
            Result.failure(e)
        }
    }

    suspend fun submitExercise(
        exerciseId: String,
        token: String,
        response: String? = null,
        audioUrl: String? = null,
        selectedAnswers: Map<String, String>? = null
    ): Result<Map<String, Any>> {
        return try {
            val authHeader = "Bearer $token"
            val submissionRequest = ExerciseSubmissionRequest(
                response = response,
                audioUrl = audioUrl,
                selectedAnswers = selectedAnswers
            )
            val apiResponse = apiService.submitExercise(exerciseId, authHeader, submissionRequest)
            
            if (apiResponse.isSuccessful) {
                Result.success(apiResponse.body() ?: emptyMap())
            } else {
                val error = apiResponse.errorBody()?.string()
                Timber.e("Submit exercise failed: code=${apiResponse.code()}, error=$error")
                Result.failure(Exception("Submit exercise failed: ${apiResponse.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception submitting exercise")
            Result.failure(e)
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
            order = lessonDetail.orderIndex,
            description = lessonDetail.description
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
                
                // Determine challenge type based on question_type_id
                val questionType = getQuestionType(question.questionTypeId, question.questionType)
                Timber.d("Question ${question.id}: question_type_id=${question.questionTypeId} mapped to ${questionType.name}")
                
                val challenge = ChallengeLite(
                    id = question.id,
                    lessonId = lessonDetail.id,
                    question = question.content,
                    type = questionType,
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
                
                val metadata = question.metadata?.let {
                    QuestionMetadata(
                        choices = it.choices,
                        matchingPairs = it.pairs?.map { pair ->
                            QuestionMetadataPair(
                                left = pair.left,
                                right = pair.right
                            )
                        }
                    )
                }
                val matchingPairs = question.matchingPairs?.map {
                    QuestionMatchingPair(
                        id = it.id,
                        leftText = it.leftText,
                        rightText = it.rightText
                    )
                }
                val sentenceOrder = question.sentenceOrder?.let {
                    QuestionSentenceOrder(
                        id = it.id,
                        correctSequence = it.correctSequence
                    )
                }
                val audioComprehension = question.audioComprehension?.let {
                    QuestionAudioComprehension(
                        id = it.id,
                        transcript = it.transcript
                    )
                }
                val pronunciation = question.pronunciation?.let {
                    QuestionPronunciation(
                        id = it.id,
                        targetPhrase = it.targetPhrase,
                        referenceAudioUrl = it.referenceAudioUrl
                    )
                }
                val blank = question.blank?.let {
                    QuestionBlank(
                        id = it.id,
                        caseSensitive = it.caseSensitive
                    )
                }
                
                challengesWithOptions.add(
                    ChallengeWithOptions(
                        challenge = challenge,
                        options = options,
                        completed = false,
                        metadata = metadata,
                        matchingPairs = matchingPairs,
                        sentenceOrder = sentenceOrder,
                        audioComprehension = audioComprehension,
                        pronunciation = pronunciation,
                        blank = blank
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception while processing question ${question.id}")
            }
        }
        
        Timber.d("Successfully converted lesson to ${challengesWithOptions.size} challenges")
        Timber.d("Challenge questions: ${challengesWithOptions.map { it.challenge.question }}")
        
        // Convert exercises to ExerciseLite from API response
        // This is REAL data from API, NOT fake data
        Timber.d("Converting ${lessonDetail.exercises.size} exercises from API response...")
        val exercises = lessonDetail.exercises.map { exercise ->
            ExerciseLite(
                id = exercise.id,
                type = exercise.type.lowercase(),
                title = exercise.title,
                content = exercise.content,
                orderIndex = exercise.orderIndex
            )
        }
        Timber.d("Converted ${exercises.size} exercises from API: ${exercises.map { "id=${it.id}, type=${it.type}, title=${it.title}" }}")
        
        // Keep full ExerciseResponse for detailed access (audioUrl, transcript, etc.)
        val exercisesResponse = lessonDetail.exercises
        Timber.d("Preserved ${exercisesResponse.size} full exercise responses with complete data")
        
        if (exercises.isEmpty()) {
            Timber.w("WARNING: No exercises found in API response for lesson ${lessonDetail.id}")
        }
        
        // Get progress from API response
        val progressPercent = lessonDetail.progress?.progressPercent ?: 0
        Timber.d("Lesson progress: $progressPercent%")
        
        return LessonWithChallenges(
            lesson = lesson,
            challenges = challengesWithOptions,
            questionResponses = lessonDetail.questions,
            exercises = exercises,
            exercisesResponse = exercisesResponse,
            progressPercent = progressPercent
        )
    }
    
    suspend fun updateLessonProgress(lessonId: String, token: String? = null): Result<Map<String, Any>> {
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
