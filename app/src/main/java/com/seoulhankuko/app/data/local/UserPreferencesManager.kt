package com.seoulhankuko.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore manager for user preferences and authentication state
 */
@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")
        
        // Keys for user preferences
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_AVATAR_URL_KEY = stringPreferencesKey("user_avatar_url")
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        private val IS_PREMIUM_KEY = booleanPreferencesKey("is_premium")
        
        // Entry test related keys
        private val HAS_COMPLETED_ENTRY_TEST_KEY = booleanPreferencesKey("has_completed_entry_test")
        private val CURRENT_COURSE_ID_KEY = intPreferencesKey("current_course_id")
        private val CURRENT_COURSE_NAME_KEY = stringPreferencesKey("current_course_name")
        private val ENTRY_TEST_SCORE_KEY = intPreferencesKey("entry_test_score")
        
        // Offline entry test tracking
        private val HAS_COMPLETED_ENTRY_TEST_OFFLINE_KEY = booleanPreferencesKey("has_completed_entry_test_offline")
        private val ENTRY_TEST_SCORE_OFFLINE_KEY = intPreferencesKey("entry_test_score_offline")
        private val CURRENT_COURSE_ID_OFFLINE_KEY = intPreferencesKey("current_course_id_offline")
        private val CURRENT_COURSE_NAME_OFFLINE_KEY = stringPreferencesKey("current_course_name_offline")
        private val ENTRY_TEST_NEEDS_SYNC_KEY = booleanPreferencesKey("entry_test_needs_sync")
        
        // Entry test popup tracking for logged-in users
        private val ENTRY_TEST_POPUP_DISMISSED_KEY = booleanPreferencesKey("entry_test_popup_dismissed")
        
        // Guest mode tracking
        private val IS_GUEST_MODE_KEY = booleanPreferencesKey("is_guest_mode")
        private val GUEST_LESSONS_COMPLETED_KEY = intPreferencesKey("guest_lessons_completed")
    }

    /**
     * Save user authentication data
     */
    suspend fun saveUserData(
        userId: String,
        email: String,
        name: String,
        avatarUrl: String? = null,
        accessToken: String,
        refreshToken: String? = null,
        isPremium: Boolean = false
    ) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[USER_EMAIL_KEY] = email
            preferences[USER_NAME_KEY] = name
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[IS_LOGGED_IN_KEY] = true
            preferences[IS_PREMIUM_KEY] = isPremium
            
            avatarUrl?.let { preferences[USER_AVATAR_URL_KEY] = it }
            refreshToken?.let { preferences[REFRESH_TOKEN_KEY] = it }
        }
    }

    /**
     * Update tokens only (for refresh)
     */
    suspend fun updateTokens(accessToken: String, refreshToken: String?) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            refreshToken?.let { preferences[REFRESH_TOKEN_KEY] = it }
        }
    }

    /**
     * Clear user data (logout)
     */
    suspend fun clearUserData() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_ID_KEY)
            preferences.remove(USER_EMAIL_KEY)
            preferences.remove(USER_NAME_KEY)
            preferences.remove(USER_AVATAR_URL_KEY)
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(HAS_COMPLETED_ENTRY_TEST_KEY)
            preferences.remove(CURRENT_COURSE_ID_KEY)
            preferences.remove(CURRENT_COURSE_NAME_KEY)
            preferences.remove(ENTRY_TEST_SCORE_KEY)
            preferences.remove(ENTRY_TEST_POPUP_DISMISSED_KEY) // Reset popup dismissal on logout
            preferences[IS_LOGGED_IN_KEY] = false
            preferences[IS_PREMIUM_KEY] = false
        }
    }

    /**
     * Get user ID
     */
    val userId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }

    /**
     * Get user email
     */
    val userEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL_KEY]
    }

    /**
     * Get user name
     */
    val userName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY]
    }

    /**
     * Get user avatar URL
     */
    val userAvatarUrl: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_AVATAR_URL_KEY]
    }

    /**
     * Get access token
     */
    val accessToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN_KEY]
    }

    /**
     * Get refresh token
     */
    val refreshToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN_KEY]
    }

    /**
     * Check if user is logged in
     */
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN_KEY] ?: false
    }

    /**
     * Check if user is premium
     */
    val isPremium: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_PREMIUM_KEY] ?: false
    }

    /**
     * Get all user data as a flow
     */
    val userData: Flow<UserData> = context.dataStore.data.map { preferences ->
        UserData(
            userId = preferences[USER_ID_KEY],
            email = preferences[USER_EMAIL_KEY],
            name = preferences[USER_NAME_KEY],
            avatarUrl = preferences[USER_AVATAR_URL_KEY],
            accessToken = preferences[ACCESS_TOKEN_KEY],
            refreshToken = preferences[REFRESH_TOKEN_KEY],
            isLoggedIn = preferences[IS_LOGGED_IN_KEY] ?: false,
            isPremium = preferences[IS_PREMIUM_KEY] ?: false
        )
    }
    
    /**
     * Get current access token synchronously
     */
    suspend fun getCurrentAccessToken(): String? {
        return context.dataStore.data.first()[ACCESS_TOKEN_KEY]
    }
    
    /**
     * Save entry test completion data
     */
    suspend fun saveEntryTestResult(
        hasCompletedEntryTest: Boolean,
        currentCourseId: Int?,
        currentCourseName: String?,
        entryTestScore: Int?
    ) {
        context.dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_ENTRY_TEST_KEY] = hasCompletedEntryTest
            currentCourseId?.let { preferences[CURRENT_COURSE_ID_KEY] = it }
            currentCourseName?.let { preferences[CURRENT_COURSE_NAME_KEY] = it }
            entryTestScore?.let { preferences[ENTRY_TEST_SCORE_KEY] = it }
        }
    }
    
    /**
     * Check if user has completed entry test
     */
    suspend fun hasCompletedEntryTest(): Boolean {
        return context.dataStore.data.first()[HAS_COMPLETED_ENTRY_TEST_KEY] ?: false
    }
    
    /**
     * Get current course ID
     */
    suspend fun getCurrentCourseId(): Int? {
        return context.dataStore.data.first()[CURRENT_COURSE_ID_KEY]
    }
    
    /**
     * Get current course name
     */
    suspend fun getCurrentCourseName(): String? {
        return context.dataStore.data.first()[CURRENT_COURSE_NAME_KEY]
    }
    
    /**
     * Get entry test score
     */
    suspend fun getEntryTestScore(): Int? {
        return context.dataStore.data.first()[ENTRY_TEST_SCORE_KEY]
    }
    
    /**
     * Flow for has completed entry test
     */
    val hasCompletedEntryTestFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAS_COMPLETED_ENTRY_TEST_KEY] ?: false
    }
    
    /**
     * Flow for current course ID
     */
    val currentCourseIdFlow: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_COURSE_ID_KEY]
    }
    
    // ========== OFFLINE ENTRY TEST METHODS ==========
    
    /**
     * Save entry test result offline (before user logs in)
     */
    suspend fun saveEntryTestResultOffline(
        score: Int,
        courseId: Int?,
        courseName: String?
    ) {
        context.dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_ENTRY_TEST_OFFLINE_KEY] = true
            preferences[ENTRY_TEST_SCORE_OFFLINE_KEY] = score
            preferences[ENTRY_TEST_NEEDS_SYNC_KEY] = true
            courseId?.let { preferences[CURRENT_COURSE_ID_OFFLINE_KEY] = it }
            courseName?.let { preferences[CURRENT_COURSE_NAME_OFFLINE_KEY] = it }
        }
    }
    
    /**
     * Check if user has completed entry test offline
     */
    suspend fun hasCompletedEntryTestOffline(): Boolean {
        return context.dataStore.data.first()[HAS_COMPLETED_ENTRY_TEST_OFFLINE_KEY] ?: false
    }
    
    /**
     * Check if entry test result needs to be synced to server
     */
    suspend fun entryTestNeedsSync(): Boolean {
        return context.dataStore.data.first()[ENTRY_TEST_NEEDS_SYNC_KEY] ?: false
    }
    
    /**
     * Get offline entry test data
     */
    suspend fun getOfflineEntryTestData(): Triple<Int, Int?, String?> {
        val prefs = context.dataStore.data.first()
        return Triple(
            prefs[ENTRY_TEST_SCORE_OFFLINE_KEY] ?: 0,
            prefs[CURRENT_COURSE_ID_OFFLINE_KEY],
            prefs[CURRENT_COURSE_NAME_OFFLINE_KEY]
        )
    }
    
    /**
     * Mark entry test as synced (clear offline flags after successful server sync)
     */
    suspend fun markEntryTestSynced() {
        context.dataStore.edit { preferences ->
            preferences.remove(HAS_COMPLETED_ENTRY_TEST_OFFLINE_KEY)
            preferences.remove(ENTRY_TEST_SCORE_OFFLINE_KEY)
            preferences.remove(CURRENT_COURSE_ID_OFFLINE_KEY)
            preferences.remove(CURRENT_COURSE_NAME_OFFLINE_KEY)
            preferences[ENTRY_TEST_NEEDS_SYNC_KEY] = false
        }
    }
    
    /**
     * Check if user needs to see entry test (either online or offline completion)
     */
    suspend fun needsEntryTest(): Boolean {
        val prefs = context.dataStore.data.first()
        val hasCompletedOnline = prefs[HAS_COMPLETED_ENTRY_TEST_KEY] ?: false
        val hasCompletedOffline = prefs[HAS_COMPLETED_ENTRY_TEST_OFFLINE_KEY] ?: false
        return !hasCompletedOnline && !hasCompletedOffline
    }
    
    // ========== ENTRY TEST POPUP TRACKING ==========
    
    /**
     * Mark entry test popup as dismissed
     */
    suspend fun dismissEntryTestPopup() {
        context.dataStore.edit { preferences ->
            preferences[ENTRY_TEST_POPUP_DISMISSED_KEY] = true
        }
    }
    
    /**
     * Check if entry test popup was dismissed
     */
    suspend fun hasDismissedEntryTestPopup(): Boolean {
        return context.dataStore.data.first()[ENTRY_TEST_POPUP_DISMISSED_KEY] ?: false
    }
    
    /**
     * Reset popup dismissal flag (when user completes entry test)
     */
    suspend fun resetEntryTestPopupDismissal() {
        context.dataStore.edit { preferences ->
            preferences.remove(ENTRY_TEST_POPUP_DISMISSED_KEY)
        }
    }
    
    /**
     * Check if should show entry test popup for logged-in user
     */
    suspend fun shouldShowEntryTestPopup(): Boolean {
        val prefs = context.dataStore.data.first()
        val hasCompleted = prefs[HAS_COMPLETED_ENTRY_TEST_KEY] ?: false
        val hasDismissed = prefs[ENTRY_TEST_POPUP_DISMISSED_KEY] ?: false
        return !hasCompleted && !hasDismissed
    }
    
    // ========== GUEST MODE METHODS ==========
    
    /**
     * Enter guest mode
     */
    suspend fun enterGuestMode() {
        context.dataStore.edit { preferences ->
            preferences[IS_GUEST_MODE_KEY] = true
            preferences[GUEST_LESSONS_COMPLETED_KEY] = 0
        }
    }
    
    /**
     * Check if user is in guest mode
     */
    val isGuestMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_GUEST_MODE_KEY] ?: false
    }
    
    suspend fun isGuestModeSync(): Boolean {
        return context.dataStore.data.first()[IS_GUEST_MODE_KEY] ?: false
    }
    
    /**
     * Get number of lessons completed in guest mode
     */
    val guestLessonsCompleted: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[GUEST_LESSONS_COMPLETED_KEY] ?: 0
    }
    
    suspend fun getGuestLessonsCompleted(): Int {
        return context.dataStore.data.first()[GUEST_LESSONS_COMPLETED_KEY] ?: 0
    }
    
    /**
     * Increment guest lessons completed
     */
    suspend fun incrementGuestLessonsCompleted() {
        context.dataStore.edit { preferences ->
            val current = preferences[GUEST_LESSONS_COMPLETED_KEY] ?: 0
            preferences[GUEST_LESSONS_COMPLETED_KEY] = current + 1
        }
    }
    
    /**
     * Exit guest mode (when user logs in)
     */
    suspend fun exitGuestMode() {
        context.dataStore.edit { preferences ->
            preferences.remove(IS_GUEST_MODE_KEY)
            preferences.remove(GUEST_LESSONS_COMPLETED_KEY)
        }
    }
}

/**
 * Data class for user information
 */
data class UserData(
    val userId: String?,
    val email: String?,
    val name: String?,
    val avatarUrl: String?,
    val accessToken: String?,
    val refreshToken: String?,
    val isLoggedIn: Boolean,
    val isPremium: Boolean
)

