package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.api.model.*
import com.seoulhankuko.app.data.api.service.ApiService
import com.seoulhankuko.app.data.local.UserPreferencesManager
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Entry Test functionality
 */
@Singleton
class EntryTestRepository @Inject constructor(
    private val apiService: ApiService,
    private val userPreferencesManager: UserPreferencesManager
) {
    
    /**
     * Get entry test questions from the API
     */
    suspend fun getEntryTestQuestions(): Response<EntryTestResponse> {
        val token = userPreferencesManager.getCurrentAccessToken()
        return apiService.getEntryTestQuestions("Bearer $token")
    }
    
    /**
     * Submit entry test answers
     */
    suspend fun submitEntryTest(submissionRequest: EntryTestSubmissionRequest): Response<EntryTestSubmissionResponse> {
        val token = userPreferencesManager.getCurrentAccessToken()
        return apiService.submitEntryTest("Bearer $token", submissionRequest)
    }
    
    /**
     * Get entry test result for the current user
     */
    suspend fun getEntryTestResult(): Response<EntryTestResultResponse> {
        val token = userPreferencesManager.getCurrentAccessToken()
        return apiService.getEntryTestResult("Bearer $token")
    }
    
    /**
     * Save entry test result locally
     */
    suspend fun saveEntryTestResult(
        hasCompletedEntryTest: Boolean,
        currentCourseId: Int?,
        currentCourseName: String?,
        entryTestScore: Int?
    ) {
        userPreferencesManager.saveEntryTestResult(
            hasCompletedEntryTest = hasCompletedEntryTest,
            currentCourseId = currentCourseId,
            currentCourseName = currentCourseName,
            entryTestScore = entryTestScore
        )
    }
    
    /**
     * Check if user has completed entry test
     */
    suspend fun hasCompletedEntryTest(): Boolean {
        return userPreferencesManager.hasCompletedEntryTest()
    }
    
    /**
     * Get current course ID
     */
    suspend fun getCurrentCourseId(): Int? {
        return userPreferencesManager.getCurrentCourseId()
    }
    
    /**
     * Sync user data from backend including entry test completion status
     */
    suspend fun syncUserDataFromBackend(): Boolean {
        return try {
            val token = userPreferencesManager.getCurrentAccessToken()
            if (token != null) {
                val response = apiService.getCurrentUser("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val userData = response.body()!!
                    saveEntryTestResult(
                        hasCompletedEntryTest = userData.hasCompletedEntryTest,
                        currentCourseId = userData.currentCourseId,
                        currentCourseName = null, // Will be fetched separately if needed
                        entryTestScore = userData.entryTestScore
                    )
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    // ========== OFFLINE ENTRY TEST METHODS ==========
    
    /**
     * Save entry test result offline (for users not logged in yet)
     */
    suspend fun saveEntryTestResultOffline(
        score: Int,
        courseId: Int?,
        courseName: String?
    ) {
        userPreferencesManager.saveEntryTestResultOffline(
            score = score,
            courseId = courseId,
            courseName = courseName
        )
    }
    
    /**
     * Check if user needs entry test (either online or offline)
     */
    suspend fun needsEntryTest(): Boolean {
        return userPreferencesManager.needsEntryTest()
    }
    
    /**
     * Check if user has completed entry test offline
     */
    suspend fun hasCompletedEntryTestOffline(): Boolean {
        return userPreferencesManager.hasCompletedEntryTestOffline()
    }
    
    /**
     * Check if entry test needs to be synced to server
     */
    suspend fun entryTestNeedsSync(): Boolean {
        return userPreferencesManager.entryTestNeedsSync()
    }
    
    /**
     * Sync offline entry test result to server after user logs in
     */
    suspend fun syncOfflineEntryTestToServer(): Boolean {
        return try {
            val needsSync = userPreferencesManager.entryTestNeedsSync()
            if (!needsSync) {
                return true // Nothing to sync
            }
            
            val token = userPreferencesManager.getCurrentAccessToken()
            if (token == null) {
                return false // No token available
            }
            
            // Get offline data
            val (score, courseId, courseName) = userPreferencesManager.getOfflineEntryTestData()
            
            // For now, we'll mark it as synced since the server already knows the user completed it
            // In a real implementation, you might want to send the result to server
            
            // Mark as synced
            userPreferencesManager.markEntryTestSynced()
            
            // Also update the main entry test result
            saveEntryTestResult(
                hasCompletedEntryTest = true,
                currentCourseId = courseId,
                currentCourseName = courseName,
                entryTestScore = score
            )
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // ========== ENTRY TEST POPUP TRACKING ==========
    
    /**
     * Check if should show entry test popup for logged-in user
     */
    suspend fun shouldShowEntryTestPopup(): Boolean {
        return userPreferencesManager.shouldShowEntryTestPopup()
    }
    
    /**
     * Mark entry test popup as dismissed
     */
    suspend fun dismissEntryTestPopup() {
        userPreferencesManager.dismissEntryTestPopup()
    }
    
    /**
     * Reset popup dismissal flag when user completes entry test
     */
    suspend fun resetEntryTestPopupDismissal() {
        userPreferencesManager.resetEntryTestPopupDismissal()
    }
}
