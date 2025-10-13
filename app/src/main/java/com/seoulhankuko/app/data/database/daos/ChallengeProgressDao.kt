package com.seoulhankuko.app.data.database.daos

import androidx.room.*
import com.seoulhankuko.app.data.database.entities.ChallengeProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeProgressDao {
    @Query("SELECT * FROM challenge_progress WHERE userId = :userId AND challengeId = :challengeId")
    fun getChallengeProgress(userId: String, challengeId: Int): Flow<ChallengeProgress?>
    
    @Query("SELECT * FROM challenge_progress WHERE userId = :userId")
    fun getAllUserProgress(userId: String): Flow<List<ChallengeProgress>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallengeProgress(progress: ChallengeProgress)
    
    @Update
    suspend fun updateChallengeProgress(progress: ChallengeProgress)
    
    @Delete
    suspend fun deleteChallengeProgress(progress: ChallengeProgress)
}

