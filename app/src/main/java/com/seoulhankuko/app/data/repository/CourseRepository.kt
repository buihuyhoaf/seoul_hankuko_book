package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.api.model.*
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.data.api.util.ExceptionMapper
import com.seoulhankuko.app.data.database.daos.CourseCacheDao
import com.seoulhankuko.app.data.database.entities.CourseCacheEntity
import com.seoulhankuko.app.data.database.entities.UnitCacheEntity
import com.seoulhankuko.app.data.database.entities.LessonCacheEntity
import com.seoulhankuko.app.domain.exception.AppException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val apiService: ApiService,
    private val courseCacheDao: CourseCacheDao
) {
    private val _courses = MutableStateFlow<List<CourseResponse>>(emptyList())
    val courses: StateFlow<List<CourseResponse>> = _courses.asStateFlow()
    
    private val _currentCourse = MutableStateFlow<CourseDetailResponse?>(null)
    val currentCourse: StateFlow<CourseDetailResponse?> = _currentCourse.asStateFlow()
    
    private val _currentUnit = MutableStateFlow<UnitDetailResponse?>(null)
    val currentUnit: StateFlow<UnitDetailResponse?> = _currentUnit.asStateFlow()
    
    private val _currentLesson = MutableStateFlow<LessonDetailResponse?>(null)
    val currentLesson: StateFlow<LessonDetailResponse?> = _currentLesson.asStateFlow()
    
    suspend fun getCourses(page: Int = 1, itemsPerPage: Int = 10, token: String? = null): Result<PaginatedResponse<CourseResponse>> {
        return try {
            val formattedToken = token?.let { "Bearer $it" }
            Timber.d("Calling getCourses API with token: ${formattedToken?.take(20)}...")
            val response = apiService.getCourses(page, itemsPerPage, formattedToken)
            
            Timber.d("API Response - Code: ${response.code()}, Success: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val coursesResponse = response.body()
                Timber.d("Response body: $coursesResponse")
                if (coursesResponse != null) {
                    val courses = coursesResponse.data ?: emptyList()
                    Timber.d("Courses from response: $courses")
                    _courses.value = courses
                    Result.success(coursesResponse)
                } else {
                    Timber.e("Courses response body is null")
                    Result.failure(Exception("Courses response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get courses"
                Timber.e("API Error - Code: ${response.code()}, Message: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    /**
     * Check if course is cached (synchronous, fast check)
     */
    suspend fun hasCourseCache(courseId: String): Boolean {
        return try {
            val cachedCourse = courseCacheDao.getCourseSync(courseId)
            val cachedUnits = if (cachedCourse != null) {
                courseCacheDao.getUnitsByCourseIdSync(courseId)
            } else {
                emptyList()
            }
            cachedCourse != null && cachedUnits.isNotEmpty()
        } catch (e: Exception) {
            Timber.e(e, "Error checking course cache")
            false
        }
    }
    
    /**
     * Get cached course data immediately (synchronous, fast)
     */
    suspend fun getCachedCourse(courseId: String): CourseDetailResponse? {
        return try {
            val cachedCourse = courseCacheDao.getCourseSync(courseId)
            val cachedUnits = if (cachedCourse != null) {
                courseCacheDao.getUnitsByCourseIdSync(courseId)
            } else {
                emptyList()
            }
            
            if (cachedCourse != null && cachedUnits.isNotEmpty()) {
                convertCacheToCourseDetailResponse(cachedCourse, cachedUnits)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting cached course")
            null
        }
    }
    
    /**
     * Get course with cache-first strategy:
     * 1. Emit cached data immediately (if available)
     * 2. Fetch fresh data from API in background
     * 3. Update cache and emit new data
     */
    fun getCourseFlow(courseId: String, token: String? = null): Flow<Result<CourseDetailResponse>> = flow {
        // First, try to emit cached data if available
        val cachedCourse = courseCacheDao.getCourseSync(courseId)
        val cachedUnits = if (cachedCourse != null) {
            courseCacheDao.getUnitsByCourseIdSync(courseId)
        } else {
            emptyList()
        }
        
        if (cachedCourse != null && cachedUnits.isNotEmpty()) {
            Timber.d("Emitting cached course data for courseId: $courseId")
            val cachedResponse = convertCacheToCourseDetailResponse(cachedCourse, cachedUnits)
            emit(Result.success(cachedResponse))
        }
        
        // Then fetch fresh data from API
        val result = getCourse(courseId, token)
        result.onSuccess { courseDetail ->
            // Save to cache
            saveCourseToCache(courseDetail)
        }
        emit(result)
    }.flowOn(Dispatchers.IO)
    
    suspend fun getCourse(courseId: String, token: String? = null): Result<CourseDetailResponse> {
        return try {
            // Note: Token is now handled by AuthInterceptor automatically
            // We only pass token manually if it's provided for backward compatibility
            val response = if (token != null) {
                apiService.getCourse(courseId, "Bearer $token")
            } else {
                // Let the AuthInterceptor handle adding the token automatically
                apiService.getCourse(courseId, null)
            }
            
            if (response.isSuccessful) {
                val courseResponse = response.body()
                if (courseResponse != null) {
                    _currentCourse.value = courseResponse
                    Result.success(courseResponse)
                } else {
                    Result.failure(Exception("Course response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get course"
                val httpCode = response.code()
                when (httpCode) {
                    401 -> {
                        Timber.w("Authentication failed for course $courseId - token may need refresh")
                        Result.failure(AppException.AuthException.TokenExpired())
                    }
                    403 -> Result.failure(AppException.UnexpectedError("Access denied: You don't have permission to access this course"))
                    404 -> Result.failure(AppException.DataException.DataNotFound())
                    else -> Result.failure(AppException.UnexpectedError("Failed to get course (HTTP $httpCode): $errorMessage"))
                }
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    /**
     * Save course data to cache
     */
    private suspend fun saveCourseToCache(courseDetail: CourseDetailResponse) {
        try {
            // Save course
            courseCacheDao.insertCourse(
                CourseCacheEntity(
                    id = courseDetail.id,
                    title = courseDetail.title,
                    description = courseDetail.description,
                    imageUrl = courseDetail.imageUrl,
                    orderIndex = courseDetail.orderIndex,
                    createdAt = courseDetail.createdAt,
                    lastUpdated = System.currentTimeMillis()
                )
            )
            
            // Save units
            if (courseDetail.units.isNotEmpty()) {
                courseCacheDao.insertUnits(
                    courseDetail.units.map { unit ->
                        UnitCacheEntity(
                            id = unit.id,
                            courseId = courseDetail.id,
                            title = unit.title,
                            description = unit.description,
                            orderIndex = unit.orderIndex,
                            createdAt = unit.createdAt,
                            lessonsCount = unit.lessonsCount,
                            lastUpdated = System.currentTimeMillis()
                        )
                    }
                )
            }
            
            Timber.d("Saved course ${courseDetail.id} to cache with ${courseDetail.units.size} units")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save course to cache")
        }
    }
    
    /**
     * Convert cached entities to CourseDetailResponse
     */
    private fun convertCacheToCourseDetailResponse(
        course: CourseCacheEntity,
        units: List<UnitCacheEntity>
    ): CourseDetailResponse {
        return CourseDetailResponse(
            id = course.id,
            title = course.title,
            description = course.description,
            imageUrl = course.imageUrl,
            orderIndex = course.orderIndex,
            createdAt = course.createdAt ?: "",
            units = units.map { unit ->
                UnitResponse(
                    id = unit.id,
                    courseId = unit.courseId,
                    title = unit.title,
                    description = unit.description,
                    orderIndex = unit.orderIndex,
                    createdAt = unit.createdAt ?: "",
                    lessonsCount = unit.lessonsCount,
                    progress = null // Progress is not cached
                )
            },
            progress = null // Progress is not cached
        )
    }
    
    /**
     * Check if unit is cached (synchronous, fast check)
     */
    suspend fun hasUnitCache(unitId: String): Boolean {
        return try {
            val cachedUnit = courseCacheDao.getUnitSync(unitId)
            val cachedLessons = if (cachedUnit != null) {
                courseCacheDao.getLessonsByUnitIdSync(unitId)
            } else {
                emptyList()
            }
            cachedUnit != null && cachedLessons.isNotEmpty()
        } catch (e: Exception) {
            Timber.e(e, "Error checking unit cache")
            false
        }
    }
    
    /**
     * Get cached unit data immediately (synchronous, fast)
     */
    suspend fun getCachedUnit(unitId: String): UnitDetailResponse? {
        return try {
            val cachedUnit = courseCacheDao.getUnitSync(unitId)
            val cachedLessons = if (cachedUnit != null) {
                courseCacheDao.getLessonsByUnitIdSync(unitId)
            } else {
                emptyList()
            }
            
            if (cachedUnit != null && cachedLessons.isNotEmpty()) {
                convertCacheToUnitDetailResponse(cachedUnit, cachedLessons)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting cached unit")
            null
        }
    }
    
    /**
     * Get unit with cache-first strategy:
     * 1. Emit cached data immediately (if available)
     * 2. Fetch fresh data from API in background
     * 3. Update cache and emit new data
     */
    fun getUnitFlow(unitId: String, token: String? = null): Flow<Result<UnitDetailResponse>> = flow {
        // First, try to emit cached data if available
        val cachedUnit = courseCacheDao.getUnitSync(unitId)
        val cachedLessons = if (cachedUnit != null) {
            courseCacheDao.getLessonsByUnitIdSync(unitId)
        } else {
            emptyList()
        }
        
        if (cachedUnit != null && cachedLessons.isNotEmpty()) {
            Timber.d("Emitting cached unit data for unitId: $unitId")
            val cachedResponse = convertCacheToUnitDetailResponse(cachedUnit, cachedLessons)
            emit(Result.success(cachedResponse))
        }
        
        // Then fetch fresh data from API
        val result = getUnit(unitId, token)
        result.onSuccess { unitDetail ->
            // Save to cache
            saveUnitToCache(unitDetail)
        }
        emit(result)
    }.flowOn(Dispatchers.IO)
    
    suspend fun getUnit(unitId: String, token: String? = null): Result<UnitDetailResponse> {
        return try {
            val formattedToken = token?.let { "Bearer $it" }
            val response = apiService.getUnit(unitId, formattedToken)
            
            if (response.isSuccessful) {
                val unitResponse = response.body()
                if (unitResponse != null) {
                    _currentUnit.value = unitResponse
                    Result.success(unitResponse)
                } else {
                    Result.failure(Exception("Unit response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get unit"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    /**
     * Save unit data to cache
     */
    private suspend fun saveUnitToCache(unitDetail: UnitDetailResponse) {
        try {
            // Save unit
            courseCacheDao.insertUnits(
                listOf(
                    UnitCacheEntity(
                        id = unitDetail.id,
                        courseId = unitDetail.courseId,
                        title = unitDetail.title,
                        description = unitDetail.description,
                        orderIndex = unitDetail.orderIndex,
                        createdAt = unitDetail.createdAt,
                        lessonsCount = unitDetail.lessons.size,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            )
            
            // Save lessons
            if (unitDetail.lessons.isNotEmpty()) {
                courseCacheDao.insertLessons(
                    unitDetail.lessons.map { lesson ->
                        LessonCacheEntity(
                            id = lesson.id,
                            unitId = unitDetail.id,
                            title = lesson.title,
                            description = lesson.description,
                            orderIndex = lesson.orderIndex,
                            createdAt = lesson.createdAt,
                            quizzesCount = lesson.quizzesCount,
                            exercisesCount = lesson.exercisesCount,
                            lastUpdated = System.currentTimeMillis()
                        )
                    }
                )
            }
            
            Timber.d("Saved unit ${unitDetail.id} to cache with ${unitDetail.lessons.size} lessons")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save unit to cache")
        }
    }
    
    /**
     * Convert cached entities to UnitDetailResponse
     */
    private fun convertCacheToUnitDetailResponse(
        unit: UnitCacheEntity,
        lessons: List<LessonCacheEntity>
    ): UnitDetailResponse {
        return UnitDetailResponse(
            id = unit.id,
            courseId = unit.courseId,
            title = unit.title,
            description = unit.description,
            orderIndex = unit.orderIndex,
            createdAt = unit.createdAt ?: "",
            lessons = lessons.map { lesson ->
                LessonResponse(
                    id = lesson.id,
                    unitId = lesson.unitId,
                    title = lesson.title,
                    description = lesson.description,
                    orderIndex = lesson.orderIndex,
                    createdAt = lesson.createdAt ?: "",
                    quizzesCount = lesson.quizzesCount,
                    exercisesCount = lesson.exercisesCount,
                    progress = null // Progress is not cached
                )
            },
            progress = null // Progress is not cached
        )
    }
    
    suspend fun getLesson(lessonId: String, token: String? = null): Result<LessonDetailResponse> {
        return try {
            val formattedToken = token?.let { "Bearer $it" }
            val response = apiService.getLesson(lessonId, formattedToken)
            
            if (response.isSuccessful) {
                val lessonResponse = response.body()
                if (lessonResponse != null) {
                    _currentLesson.value = lessonResponse
                    Result.success(lessonResponse)
                } else {
                    Result.failure(Exception("Lesson response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get lesson"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    fun clearCurrentCourse() {
        _currentCourse.value = null
    }
    
    fun clearCurrentUnit() {
        _currentUnit.value = null
    }
    
    fun clearCurrentLesson() {
        _currentLesson.value = null
    }
}