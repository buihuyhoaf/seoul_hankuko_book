package com.seoulhankuko.app.data.api.service

import com.seoulhankuko.app.data.api.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // Authentication endpoints
    @POST("v1/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>
    
    @POST("v1/refresh")
    suspend fun refreshToken(@Body refreshTokenRequest: RefreshTokenRequest): Response<RefreshTokenResponse>
    
    @POST("v1/logout")
    suspend fun logout(
        @Header("Authorization") token: String,
        @Body logoutRequest: LogoutRequest? = null
    ): Response<Unit>
    
    @POST("v1/auth/google")
    suspend fun signInWithGoogle(@Body googleSignInRequest: GoogleSignInRequest): Response<GoogleSignInResponse>
    
    
    // User management endpoints
    @POST("v1/user")
    suspend fun createUser(@Body userCreateRequest: UserCreateRequest): Response<UserCreateResponse>
    
    @GET("v1/users")
    suspend fun getUsers(
        @Query("page") page: Int = 1,
        @Query("items_per_page") itemsPerPage: Int = 10
    ): Response<PaginatedResponse<UserReadResponse>>
    
    @GET("v1/user/me/")
    suspend fun getCurrentUser(@Header("Authorization") token: String): Response<UserReadResponse>
    
    @PATCH("v1/user/{username}")
    suspend fun updateUser(
        @Header("Authorization") token: String,
        @Path("username") username: String,
        @Body userUpdateRequest: UserUpdateRequest
    ): Response<Map<String, String>>
    
    @PATCH("v1/user/{username}/tier")
    suspend fun updateUserTier(
        @Header("Authorization") token: String,
        @Path("username") username: String,
        @Body userTierUpdateRequest: UserTierUpdateRequest
    ): Response<Map<String, String>>
    
    // Tier endpoints
    @POST("v1/tier")
    suspend fun createTier(@Body tierCreateRequest: TierCreateRequest): Response<TierReadResponse>
    
    @GET("v1/tiers")
    suspend fun getTiers(
        @Query("page") page: Int = 1,
        @Query("items_per_page") itemsPerPage: Int = 10
    ): Response<PaginatedResponse<TierReadResponse>>
    
    @GET("v1/tier/{name}")
    suspend fun getTier(@Path("name") name: String): Response<TierReadResponse>
    
    // Rate limit endpoints
    @POST("v1/tier/{tier_name}/rate_limit")
    suspend fun createRateLimit(
        @Path("tier_name") tierName: String,
        @Body rateLimitCreateRequest: RateLimitCreateRequest
    ): Response<RateLimitReadResponse>
    
    @GET("v1/tier/{tier_name}/rate_limits")
    suspend fun getRateLimits(
        @Path("tier_name") tierName: String,
        @Query("page") page: Int = 1,
        @Query("items_per_page") itemsPerPage: Int = 10
    ): Response<PaginatedResponse<RateLimitReadResponse>>
    
    // Course management endpoints
    @GET("v1/courses")
    suspend fun getCourses(
        @Query("page") page: Int = 1,
        @Query("items_per_page") itemsPerPage: Int = 10,
        @Header("Authorization") token: String? = null
    ): Response<PaginatedResponse<CourseResponse>>
    
    @GET("v1/courses/{course_id}")
    suspend fun getCourse(
        @Path("course_id") courseId: String,
        @Header("Authorization") token: String? = null
    ): Response<CourseDetailResponse>
    
    @GET("v1/units/{unit_id}")
    suspend fun getUnit(
        @Path("unit_id") unitId: String,
        @Header("Authorization") token: String? = null
    ): Response<UnitDetailResponse>
    
    @GET("v1/lessons/{lesson_id}")
    suspend fun getLesson(
        @Path("lesson_id") lessonId: String,
        @Header("Authorization") token: String? = null
    ): Response<LessonDetailResponse>

    @GET("v1/lessons/{lesson_id}/questions")
    suspend fun getLessonQuestions(
        @Path("lesson_id") lessonId: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int = 10,
        @Header("Authorization") token: String? = null
    ): Response<PaginatedResponse<QuestionResponse>>
    
    @POST("v1/lessons/{lesson_id}/progress/update")
    suspend fun updateLessonProgress(
        @Path("lesson_id") lessonId: String,
        @Header("Authorization") token: String? = null,
        @Body body: LessonProgressUpdateRequest
    ): Response<Map<String, Any>>

    // Push tokens
    @POST("v1/push-tokens/register")
    suspend fun registerPushToken(
        @Body request: PushTokenRegisterRequest
    ): Response<Map<String, String>>

    @HTTP(method = "DELETE", path = "v1/push-tokens/unregister", hasBody = true)
    suspend fun unregisterPushToken(
        @Body request: PushTokenUnregisterRequest
    ): Response<Map<String, String>>
    
    // Practice question submit endpoints (increment lesson progress on correct answers)
    @POST("v1/lessons/{lesson_id}/practice-questions/{question_id}/submit")
    suspend fun submitPracticeQuestionSelectedOption(
        @Path("lesson_id") lessonId: String,
        @Path("question_id") questionId: String,
        @Header("Authorization") token: String,
        @Body body: PracticeSelectedOptionRequest
    ): Response<Map<String, Any>>

    @POST("v1/lessons/{lesson_id}/practice-questions/{question_id}/submit")
    suspend fun submitPracticeQuestionTextAnswer(
        @Path("lesson_id") lessonId: String,
        @Path("question_id") questionId: String,
        @Header("Authorization") token: String,
        @Body body: PracticeTextAnswerRequest
    ): Response<Map<String, Any>>

    @Multipart
    @POST("v1/pronunciation")
    suspend fun evaluatePronunciation(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part("sentence") sentence: RequestBody
    ): Response<PronunciationEvaluationResponse>
    
    // ML Prediction endpoints
    @POST("v1/predict/stroke")
    suspend fun predictStroke(
        @Body request: com.seoulhankuko.app.data.api.model.PredictStrokeRequest
    ): Response<com.seoulhankuko.app.data.api.model.PredictStrokeResponse>
    
    // Stroke Analysis endpoints (new Phase 3 API)
    @POST("stroke/analyze")
    suspend fun analyzeStroke(
        @Body request: com.seoulhankuko.app.data.api.model.StrokeAnalysisRequest
    ): Response<com.seoulhankuko.app.data.api.model.StrokeAnalysisResponse>
    
    // Quiz management endpoints
    @GET("v1/quizzes/{quiz_id}")
    suspend fun getQuiz(
        @Path("quiz_id") quizId: String,
        @Header("Authorization") token: String
    ): Response<QuizDetailResponse>
    
    @POST("v1/quizzes/{quiz_id}/attempt")
    suspend fun submitQuizAttempt(
        @Path("quiz_id") quizId: String,
        @Header("Authorization") token: String,
        @Body answers: Map<String, String>
    ): Response<QuizAttemptResponse>
    
    @GET("v1/user/{username}/quiz-attempts")
    suspend fun getUserQuizAttempts(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("items_per_page") itemsPerPage: Int = 10,
        @Header("Authorization") token: String? = null
    ): Response<PaginatedResponse<QuizAttemptResponse>>
    
    // Exercise endpoints
    @GET("v1/exercises/listening/{exercise_id}")
    suspend fun getListeningExercise(
        @Path("exercise_id") exerciseId: String,
        @Header("Authorization") token: String
    ): Response<ListeningExerciseResponse>
    
    @GET("v1/exercises/speaking/{exercise_id}")
    suspend fun getSpeakingExercise(
        @Path("exercise_id") exerciseId: String,
        @Header("Authorization") token: String
    ): Response<SpeakingExerciseResponse>
    
    @GET("v1/exercises/writing/{exercise_id}")
    suspend fun getWritingExercise(
        @Path("exercise_id") exerciseId: String,
        @Header("Authorization") token: String
    ): Response<WritingExerciseResponse>
    
    @POST("v1/exercises/{exercise_id}/submit")
    suspend fun submitExercise(
        @Path("exercise_id") exerciseId: String,
        @Header("Authorization") token: String,
        @Body body: ExerciseSubmissionRequest
    ): Response<Map<String, Any>>

    @POST("v1/writing/{exercise_id}/submit")
    suspend fun submitWritingExercise(
        @Path("exercise_id") exerciseId: String,
        @Header("Authorization") token: String,
        @Body body: ExerciseSubmissionRequest
    ): Response<Map<String, Any>>

    @GET("v1/writing/results/{lesson_id}")
    suspend fun getWritingResults(
        @Path("lesson_id") lessonId: String,
        @Header("Authorization") token: String
    ): Response<WritingResultsResponse>
    
    // User progress endpoints
    @GET("v1/user/{username}/progress")
    suspend fun getUserProgress(
        @Path("username") username: String,
        @Header("Authorization") token: String? = null
    ): Response<UserProgressResponse>
    
    @GET("v1/user/{username}/course-progress")
    suspend fun getUserCourseProgress(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("items_per_page") itemsPerPage: Int = 10,
        @Header("Authorization") token: String? = null
    ): Response<PaginatedResponse<CourseProgressResponse>>
    
    @GET("v1/user/{username}/exp-history")
    suspend fun getUserExpHistory(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("items_per_page") itemsPerPage: Int = 20,
        @Header("Authorization") token: String? = null
    ): Response<PaginatedResponse<ExpLogResponse>>
    
    @GET("v1/user/{username}/exp-series")
    suspend fun getUserExpSeries(
        @Path("username") username: String,
        @Query("days") days: Int = 7,
        @Header("Authorization") token: String? = null
    ): Response<ExpSeriesResponse>
    
    @POST("v1/user/{username}/update-streak")
    suspend fun updateUserStreak(
        @Path("username") username: String,
        @Header("Authorization") token: String
    ): Response<StreakUpdateResponse>
    
    // Daily goals endpoints
    @GET("v1/user/{username}/daily-goals")
    suspend fun getUserDailyGoals(
        @Path("username") username: String,
        @Header("Authorization") token: String? = null
    ): Response<DailyGoalsResponse>
    
    @PUT("v1/user/{username}/daily-goals")
    suspend fun updateUserDailyGoals(
        @Path("username") username: String,
        @Header("Authorization") token: String,
        @Body goalData: Map<String, Int>
    ): Response<DailyGoalsUpdateResponse>
    
    // Badges endpoints
    @GET("v1/badges")
    suspend fun getAllBadges(
        @Query("page") page: Int = 1,
        @Query("items_per_page") itemsPerPage: Int = 20,
        @Header("Authorization") token: String? = null
    ): Response<PaginatedResponse<BadgeResponse>>
    
    // Leaderboard endpoints
    @GET("v1/leaderboard")
    suspend fun getLeaderboard(
        @Query("page") page: Int = 1,
        @Query("items_per_page") itemsPerPage: Int = 50,
        @Query("season") season: String? = null,
        @Header("Authorization") token: String? = null
    ): Response<PaginatedResponse<LeaderboardEntryResponse>>
    
    // Friends endpoints
    @GET("v1/user/{username}/friends")
    suspend fun getUserFriends(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("items_per_page") itemsPerPage: Int = 20,
        @Query("status") status: String? = null,
        @Header("Authorization") token: String? = null
    ): Response<PaginatedResponse<FriendResponse>>
    
    // Notification endpoints
    @GET("v1/user/{username}/notifications")
    suspend fun getUserNotifications(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("items_per_page") itemsPerPage: Int = 20,
        @Query("unread_only") unreadOnly: Boolean = false,
        @Query("notification_type") notificationType: String? = null
    ): Response<PaginatedResponse<NotificationResponse>>
    
    @GET("v1/user/{username}/notifications/unread-count")
    suspend fun getUnreadNotificationsCount(
        @Path("username") username: String
    ): Response<NotificationUnreadCountResponse>
    
    @PUT("v1/user/{username}/notifications/{notification_id}/read")
    suspend fun markNotificationRead(
        @Path("username") username: String,
        @Path("notification_id") notificationId: String
    ): Response<Map<String, String>>
    
    @PUT("v1/user/{username}/notifications/mark-all-read")
    suspend fun markAllNotificationsRead(
        @Path("username") username: String
    ): Response<Map<String, String>>
    
    // Task endpoints
    @POST("v1/tasks/task")
    suspend fun createTask(@Body taskCreateRequest: TaskCreateRequest): Response<TaskCreateResponse>
    
    @GET("v1/tasks/task/{task_id}")
    suspend fun getTask(@Path("task_id") taskId: String): Response<Task>
    
    // Missions endpoints
    @GET("v1/missions/today")
    suspend fun getTodayMissions(
        @Header("Authorization") token: String? = null
    ): Response<TodayMissionsResponse>
    
    @POST("v1/activity")
    suspend fun trackActivity(
        @Header("Authorization") token: String? = null,
        @Body body: ActivityRequest
    ): Response<ActivityResponse>
    
    @POST("v1/exp")
    suspend fun addExp(
        @Header("Authorization") token: String? = null,
        @Body body: ExpRequest
    ): Response<ExpResponse>
    
    // Weekly Leaderboard endpoints
    @GET("v1/leaderboard/weekly")
    suspend fun getWeeklyLeaderboard(
        @Header("Authorization") token: String? = null
    ): Response<WeeklyLeaderboardResponse>

    @POST("v1/leaderboard/weekly/update-xp")
    suspend fun updateWeeklyLeaderboardXp(
        @Header("Authorization") token: String? = null,
        @Body body: WeeklyLeaderboardUpdateRequest
    ): Response<WeeklyLeaderboardUpdateResponse>
    
    // Mistakes Review endpoints
    @GET("v1/user/{username}/mistakes")
    suspend fun getUserMistakes(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("items_per_page") itemsPerPage: Int = 20,
        @Query("question_type") questionType: String? = null,
        @Header("Authorization") token: String? = null
    ): Response<MistakesListResponse>
}
