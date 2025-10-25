package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.database.daos.UserProgressDao
import com.seoulhankuko.app.data.database.entities.UserProgress
import com.seoulhankuko.app.domain.model.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalUserProgressRepository @Inject constructor(
    private val userProgressDao: UserProgressDao
) {
    fun getUserProgress(userId: String): Flow<UserProgress?> {
        return userProgressDao.getUserProgress(userId)
    }
    
    fun getTopTenUsers(): Flow<List<UserProgress>> {
        return userProgressDao.getTopTenUsers()
    }
    
    suspend fun insertUserProgress(userProgress: UserProgress) {
        userProgressDao.insertUserProgress(userProgress)
    }
    
    suspend fun updateUserProgress(userProgress: UserProgress) {
        userProgressDao.updateUserProgress(userProgress)
    }
    
    suspend fun updateHearts(userId: String, hearts: Int) {
        userProgressDao.updateHearts(userId, hearts)
    }
    
    suspend fun updatePoints(userId: String, points: Int) {
        userProgressDao.updatePoints(userId, points)
    }
    
    suspend fun reduceHearts(userId: String): Boolean {
        val userProgress = userProgressDao.getUserProgress(userId).first()
        return if (userProgress != null && userProgress.hearts > 0) {
            val newHearts = userProgress.hearts - 1
            userProgressDao.updateHearts(userId, newHearts)
            true
        } else {
            false
        }
    }
    
    suspend fun addPoints(userId: String, pointsToAdd: Int) {
        val userProgress = userProgressDao.getUserProgress(userId).first()
        if (userProgress != null) {
            val newPoints = userProgress.points + pointsToAdd
            userProgressDao.updatePoints(userId, newPoints)
        }
    }
    
    suspend fun refillHearts(userId: String) {
        userProgressDao.updateHearts(userId, Constants.HEARTS_MAX)
    }
}





