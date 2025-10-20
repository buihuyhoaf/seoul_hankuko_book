package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.api.model.*
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.data.api.util.ExceptionMapper
import com.seoulhankuko.app.domain.exception.AppException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val apiService: ApiService
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
            val response = apiService.getCourses(page, itemsPerPage, formattedToken)
            
            if (response.isSuccessful) {
                val coursesResponse = response.body()
                if (coursesResponse != null) {
                    _courses.value = coursesResponse.items
                    Result.success(coursesResponse)
                } else {
                    Result.failure(Exception("Courses response is null"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to get courses"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    suspend fun getCourse(courseId: Int, token: String? = null): Result<CourseDetailResponse> {
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
    
    suspend fun getUnit(unitId: Int, token: String? = null): Result<UnitDetailResponse> {
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
    
    suspend fun getLesson(lessonId: Int, token: String? = null): Result<LessonDetailResponse> {
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