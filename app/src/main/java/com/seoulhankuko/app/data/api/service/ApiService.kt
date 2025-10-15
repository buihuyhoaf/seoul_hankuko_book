package com.seoulhankuko.app.data.api.service

import com.seoulhankuko.app.data.api.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // Authentication endpoints
    @POST("v1/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>
    
    @POST("v1/refresh")
    suspend fun refreshToken(): Response<RefreshTokenResponse>
    
    @POST("v1/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<Unit>
    
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
        @Path("course_id") courseId: Int,
        @Header("Authorization") token: String? = null
    ): Response<CourseDetailResponse>
    
    @GET("v1/units/{unit_id}")
    suspend fun getUnit(
        @Path("unit_id") unitId: Int,
        @Header("Authorization") token: String? = null
    ): Response<UnitDetailResponse>
    
    @GET("v1/lessons/{lesson_id}")
    suspend fun getLesson(
        @Path("lesson_id") lessonId: Int,
        @Header("Authorization") token: String? = null
    ): Response<LessonDetailResponse>
    
    // Quiz management endpoints
    @GET("v1/quizzes/{quiz_id}")
    suspend fun getQuiz(
        @Path("quiz_id") quizId: Int,
        @Header("Authorization") token: String
    ): Response<QuizDetailResponse>
    
    @POST("v1/quizzes/{quiz_id}/attempt")
    suspend fun submitQuizAttempt(
        @Path("quiz_id") quizId: Int,
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
        @Path("exercise_id") exerciseId: Int,
        @Header("Authorization") token: String
    ): Response<ListeningExerciseResponse>
    
    @GET("v1/exercises/speaking/{exercise_id}")
    suspend fun getSpeakingExercise(
        @Path("exercise_id") exerciseId: Int,
        @Header("Authorization") token: String
    ): Response<SpeakingExerciseResponse>
    
    @GET("v1/exercises/writing/{exercise_id}")
    suspend fun getWritingExercise(
        @Path("exercise_id") exerciseId: Int,
        @Header("Authorization") token: String
    ): Response<WritingExerciseResponse>
    
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
    
    // Task endpoints
    @POST("v1/tasks/task")
    suspend fun createTask(@Body taskCreateRequest: TaskCreateRequest): Response<TaskCreateResponse>
    
    @GET("v1/tasks/task/{task_id}")
    suspend fun getTask(@Path("task_id") taskId: String): Response<Task>
}
