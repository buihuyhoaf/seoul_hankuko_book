package com.seoulhankuko.app.data.database.daos

import androidx.room.*
import com.seoulhankuko.app.data.database.entities.Challenge
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges WHERE lessonId = :lessonId ORDER BY `order` ASC")
    fun getChallengesByLessonId(lessonId: Int): Flow<List<Challenge>>
    
    @Query("SELECT * FROM challenges WHERE id = :id")
    suspend fun getChallengeById(id: Int): Challenge?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: Challenge)
    
    @Update
    suspend fun updateChallenge(challenge: Challenge)
    
    @Delete
    suspend fun deleteChallenge(challenge: Challenge)
}

