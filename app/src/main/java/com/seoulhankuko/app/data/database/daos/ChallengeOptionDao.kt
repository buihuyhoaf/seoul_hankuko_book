package com.seoulhankuko.app.data.database.daos

import androidx.room.*
import com.seoulhankuko.app.data.database.entities.ChallengeOption
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeOptionDao {
    @Query("SELECT * FROM challenge_options WHERE challengeId = :challengeId")
    fun getOptionsByChallengeId(challengeId: Int): Flow<List<ChallengeOption>>
    
    @Query("SELECT * FROM challenge_options WHERE id = :id")
    suspend fun getOptionById(id: Int): ChallengeOption?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOption(option: ChallengeOption)
    
    @Update
    suspend fun updateOption(option: ChallengeOption)
    
    @Delete
    suspend fun deleteOption(option: ChallengeOption)
}

