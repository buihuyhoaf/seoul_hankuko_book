package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.api.model.*
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.data.api.util.ExceptionMapper
import com.seoulhankuko.app.domain.exception.AppException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            val response = apiService.getCourses(page, itemsPerPage, token)
            
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
            val response = apiService.getCourse(courseId, token)
            
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
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Throwable) {
            val appException = ExceptionMapper.mapToAppException(e)
            Result.failure(appException)
        }
    }
    
    suspend fun getUnit(unitId: Int, token: String? = null): Result<UnitDetailResponse> {
        return try {
            val response = apiService.getUnit(unitId, token)
            
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
            val response = apiService.getLesson(lessonId, token)
            
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