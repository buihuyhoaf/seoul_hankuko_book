package com.seoulhankuko.app.data.database.daos

import androidx.room.*
import com.seoulhankuko.app.data.database.entities.UnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitDao {
    @Query("SELECT * FROM units WHERE courseId = :courseId ORDER BY `order` ASC")
    fun getUnitsByCourseId(courseId: Int): Flow<List<UnitEntity>>
    
    @Query("SELECT * FROM units WHERE id = :id")
    suspend fun getUnitById(id: Int): UnitEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: UnitEntity)
    
    @Update
    suspend fun updateUnit(unit: UnitEntity)
    
    @Delete
    suspend fun deleteUnit(unit: UnitEntity)
}
