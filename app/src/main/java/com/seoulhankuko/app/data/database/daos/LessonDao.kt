package com.seoulhankuko.app.data.database.daos

import androidx.room.*
import com.seoulhankuko.app.data.database.entities.Lesson
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE unitId = :unitId ORDER BY `order` ASC")
    fun getLessonsByUnitId(unitId: Int): Flow<List<Lesson>>
    
    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun getLessonById(id: Int): Lesson?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: Lesson)
    
    @Update
    suspend fun updateLesson(lesson: Lesson)
    
    @Delete
    suspend fun deleteLesson(lesson: Lesson)
}

