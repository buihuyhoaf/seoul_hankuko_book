package com.seoulhankuko.app.data.database.daos

import androidx.room.*
import com.seoulhankuko.app.data.database.entities.CourseCacheEntity
import com.seoulhankuko.app.data.database.entities.UnitCacheEntity
import com.seoulhankuko.app.data.database.entities.LessonCacheEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for course, unit, and lesson cache operations
 */
@Dao
interface CourseCacheDao {
    // Course queries
    @Query("SELECT * FROM courses_cache WHERE id = :courseId")
    fun getCourse(courseId: String): Flow<CourseCacheEntity?>
    
    @Query("SELECT * FROM courses_cache WHERE id = :courseId")
    suspend fun getCourseSync(courseId: String): CourseCacheEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseCacheEntity)
    
    @Query("DELETE FROM courses_cache WHERE id = :courseId")
    suspend fun deleteCourse(courseId: String)
    
    // Unit queries
    @Query("SELECT * FROM units_cache WHERE courseId = :courseId ORDER BY orderIndex")
    fun getUnitsByCourseId(courseId: String): Flow<List<UnitCacheEntity>>
    
    @Query("SELECT * FROM units_cache WHERE courseId = :courseId ORDER BY orderIndex")
    suspend fun getUnitsByCourseIdSync(courseId: String): List<UnitCacheEntity>
    
    @Query("SELECT * FROM units_cache WHERE id = :unitId")
    fun getUnit(unitId: String): Flow<UnitCacheEntity?>
    
    @Query("SELECT * FROM units_cache WHERE id = :unitId")
    suspend fun getUnitSync(unitId: String): UnitCacheEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<UnitCacheEntity>)
    
    @Query("DELETE FROM units_cache WHERE courseId = :courseId")
    suspend fun deleteUnitsByCourseId(courseId: String)
    
    // Lesson queries
    @Query("SELECT * FROM lessons_cache WHERE unitId = :unitId ORDER BY orderIndex")
    fun getLessonsByUnitId(unitId: String): Flow<List<LessonCacheEntity>>
    
    @Query("SELECT * FROM lessons_cache WHERE unitId = :unitId ORDER BY orderIndex")
    suspend fun getLessonsByUnitIdSync(unitId: String): List<LessonCacheEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonCacheEntity>)
    
    @Query("DELETE FROM lessons_cache WHERE unitId = :unitId")
    suspend fun deleteLessonsByUnitId(unitId: String)
    
    // Clear all cache
    @Query("DELETE FROM courses_cache")
    suspend fun clearAllCourses()
    
    @Query("DELETE FROM units_cache")
    suspend fun clearAllUnits()
    
    @Query("DELETE FROM lessons_cache")
    suspend fun clearAllLessons()
}

