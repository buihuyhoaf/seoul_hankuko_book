package com.seoulhankuko.app.data.database.daos

import androidx.room.*
import com.seoulhankuko.app.data.database.entities.UserProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress WHERE userId = :userId")
    fun getUserProgress(userId: String): Flow<UserProgress?>
    
    @Query("SELECT * FROM user_progress ORDER BY points DESC LIMIT 10")
    fun getTopTenUsers(): Flow<List<UserProgress>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProgress(userProgress: UserProgress)
    
    @Update
    suspend fun updateUserProgress(userProgress: UserProgress)
    
    @Query("UPDATE user_progress SET hearts = :hearts WHERE userId = :userId")
    suspend fun updateHearts(userId: String, hearts: Int)
    
    @Query("UPDATE user_progress SET points = :points WHERE userId = :userId")
    suspend fun updatePoints(userId: String, points: Int)
}

