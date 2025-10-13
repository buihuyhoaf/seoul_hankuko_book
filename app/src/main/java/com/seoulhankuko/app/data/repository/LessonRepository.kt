package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.database.daos.ChallengeDao
import com.seoulhankuko.app.data.database.daos.ChallengeOptionDao
import com.seoulhankuko.app.data.database.daos.ChallengeProgressDao
import com.seoulhankuko.app.data.database.daos.LessonDao
import com.seoulhankuko.app.data.database.entities.ChallengeProgress
import com.seoulhankuko.app.domain.model.ChallengeWithOptions
import com.seoulhankuko.app.domain.model.LessonWithChallenges
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LessonRepository @Inject constructor(
    private val lessonDao: LessonDao,
    private val challengeDao: ChallengeDao,
    private val challengeOptionDao: ChallengeOptionDao,
    private val challengeProgressDao: ChallengeProgressDao
) {
    suspend fun getLessonWithChallenges(lessonId: Int, userId: String): LessonWithChallenges? {
        val lesson = lessonDao.getLessonById(lessonId) ?: return null
        
        // Collect challenges from Flow
        val challenges = challengeDao.getChallengesByLessonId(lessonId).first()
        
        val challengesWithOptions = challenges.map { challenge ->
            // Collect options for each challenge
            val options = challengeOptionDao.getOptionsByChallengeId(challenge.id).first()
            
            // Check if challenge is completed
            val progress = challengeProgressDao.getChallengeProgress(userId, challenge.id).first()
            val completed = progress?.completed ?: false
            
            ChallengeWithOptions(
                challenge = challenge,
                options = options,
                completed = completed
            )
        }
        
        return LessonWithChallenges(lesson, challengesWithOptions)
    }
    
    suspend fun completeChallenge(userId: String, challengeId: Int) {
        val progress = ChallengeProgress(
            userId = userId,
            challengeId = challengeId,
            completed = true
        )
        challengeProgressDao.insertChallengeProgress(progress)
    }
}
