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
import java.time.Instant
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
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val USER_AVATAR_URL_KEY = stringPreferencesKey("user_avatar_url")
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        private val IS_PREMIUM_KEY = booleanPreferencesKey("is_premium")
        private val USER_CREATED_AT_KEY = stringPreferencesKey("user_created_at")
        
        // Current course tracking (for lesson navigation)
        private val CURRENT_COURSE_ID_KEY = stringPreferencesKey("current_course_id")
        
        // Guest mode tracking
        private val IS_GUEST_MODE_KEY = booleanPreferencesKey("is_guest_mode")
        private val GUEST_LESSONS_COMPLETED_KEY = intPreferencesKey("guest_lessons_completed")
        
        // User stats tracking
        private val STREAK_DAYS_KEY = intPreferencesKey("streak_days")
        private val EXP_KEY = intPreferencesKey("exp")

        // Celebration tracking
        private val STREAK_SCREEN_LAST_SHOWN_KEY = stringPreferencesKey("streak_screen_last_shown_date")

        // Current lesson tracking
        private val CURRENT_LESSON_ID_KEY = stringPreferencesKey("current_lesson_id")
        private val CURRENT_LESSON_TITLE_KEY = stringPreferencesKey("current_lesson_title")
        private val CURRENT_LESSON_UNIT_ID_KEY = stringPreferencesKey("current_lesson_unit_id")
        private val CURRENT_LESSON_COURSE_ID_KEY = stringPreferencesKey("current_lesson_course_id")
        private val CURRENT_LESSON_SAVED_AT_KEY = stringPreferencesKey("current_lesson_saved_at")

        // Push notification tracking
        private val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")
        private val FCM_TOKEN_SYNCED_USER_ID_KEY = stringPreferencesKey("fcm_token_synced_user_id")
    }

    /**
     * Save user authentication data
     */
    suspend fun saveUserData(
        userId: String,
        email: String,
        name: String,
        username: String? = null,
        avatarUrl: String? = null,
        accessToken: String,
        refreshToken: String? = null,
        isPremium: Boolean = false,
        streakDays: Int = 0,
        exp: Int = 0,
        createdAt: String? = null
    ) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[USER_EMAIL_KEY] = email
            preferences[USER_NAME_KEY] = name
            username?.let { preferences[USERNAME_KEY] = it }
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[IS_LOGGED_IN_KEY] = true
            preferences[IS_PREMIUM_KEY] = isPremium
            preferences[STREAK_DAYS_KEY] = streakDays
            preferences[EXP_KEY] = exp
            createdAt?.let { preferences[USER_CREATED_AT_KEY] = it }
            
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

    suspend fun updateStreakDays(newDays: Int) {
        context.dataStore.edit { preferences ->
            preferences[STREAK_DAYS_KEY] = newDays.coerceAtLeast(0)
        }
    }


    suspend fun updateUserProfile(
        userId: String? = null,
        email: String? = null,
        name: String? = null,
        username: String? = null,
        avatarUrl: String? = null,
        exp: Int? = null,
        streakDays: Int? = null,
        createdAt: String? = null
    ) {
        context.dataStore.edit { preferences ->
            userId?.let { preferences[USER_ID_KEY] = it }
            email?.let { preferences[USER_EMAIL_KEY] = it }
            name?.let { preferences[USER_NAME_KEY] = it }
            username?.let { preferences[USERNAME_KEY] = it }
            avatarUrl?.let { preferences[USER_AVATAR_URL_KEY] = it }
            exp?.let { preferences[EXP_KEY] = it.coerceAtLeast(0) }
            streakDays?.let { preferences[STREAK_DAYS_KEY] = it.coerceAtLeast(0) }
            createdAt?.let { preferences[USER_CREATED_AT_KEY] = it }
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
            preferences.remove(USERNAME_KEY)
            preferences.remove(USER_AVATAR_URL_KEY)
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(STREAK_DAYS_KEY)
            preferences.remove(EXP_KEY)
            preferences.remove(CURRENT_LESSON_ID_KEY)
            preferences.remove(CURRENT_LESSON_TITLE_KEY)
            preferences.remove(CURRENT_LESSON_UNIT_ID_KEY)
            preferences.remove(CURRENT_LESSON_COURSE_ID_KEY)
            preferences.remove(CURRENT_LESSON_SAVED_AT_KEY)
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
            username = preferences[USERNAME_KEY],
            avatarUrl = preferences[USER_AVATAR_URL_KEY],
            accessToken = preferences[ACCESS_TOKEN_KEY],
            refreshToken = preferences[REFRESH_TOKEN_KEY],
            isLoggedIn = preferences[IS_LOGGED_IN_KEY] ?: false,
            isPremium = preferences[IS_PREMIUM_KEY] ?: false,
            streakDays = preferences[STREAK_DAYS_KEY] ?: 0,
            exp = preferences[EXP_KEY] ?: 0,
            createdAt = preferences[USER_CREATED_AT_KEY]
        )
    }

    val currentLesson: Flow<CurrentLessonData?> = context.dataStore.data.map { preferences ->
        val lessonId = preferences[CURRENT_LESSON_ID_KEY] ?: return@map null
        CurrentLessonData(
            lessonId = lessonId,
            lessonTitle = preferences[CURRENT_LESSON_TITLE_KEY],
            unitId = preferences[CURRENT_LESSON_UNIT_ID_KEY],
            courseId = preferences[CURRENT_LESSON_COURSE_ID_KEY],
            savedAtIso = preferences[CURRENT_LESSON_SAVED_AT_KEY]
        )
    }
 
    /**
     * Get current access token synchronously
     */
    suspend fun getCurrentAccessToken(): String? {
        return context.dataStore.data.first()[ACCESS_TOKEN_KEY]
    }

    suspend fun getSavedUserId(): String? {
        return context.dataStore.data.first()[USER_ID_KEY]
    }

    suspend fun saveFcmToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[FCM_TOKEN_KEY] = token
            preferences.remove(FCM_TOKEN_SYNCED_USER_ID_KEY)
        }
    }

    suspend fun getFcmToken(): String? {
        return context.dataStore.data.first()[FCM_TOKEN_KEY]
    }

    suspend fun clearFcmToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(FCM_TOKEN_KEY)
            preferences.remove(FCM_TOKEN_SYNCED_USER_ID_KEY)
        }
    }

    suspend fun markFcmTokenSynced(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[FCM_TOKEN_SYNCED_USER_ID_KEY] = userId
        }
    }

    suspend fun getFcmTokenSyncedUserId(): String? {
        return context.dataStore.data.first()[FCM_TOKEN_SYNCED_USER_ID_KEY]
    }

    suspend fun clearFcmTokenSyncState() {
        context.dataStore.edit { preferences ->
            preferences.remove(FCM_TOKEN_SYNCED_USER_ID_KEY)
        }
    }

    suspend fun saveCurrentLesson(
        lessonId: String,
        lessonTitle: String?,
        unitId: String?,
        courseId: String?
    ) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_LESSON_ID_KEY] = lessonId

            if (lessonTitle.isNullOrBlank()) {
                preferences.remove(CURRENT_LESSON_TITLE_KEY)
            } else {
                preferences[CURRENT_LESSON_TITLE_KEY] = lessonTitle
            }

            if (unitId.isNullOrBlank()) {
                preferences.remove(CURRENT_LESSON_UNIT_ID_KEY)
            } else {
                preferences[CURRENT_LESSON_UNIT_ID_KEY] = unitId
            }

            if (courseId.isNullOrBlank()) {
                preferences.remove(CURRENT_LESSON_COURSE_ID_KEY)
            } else {
                preferences[CURRENT_LESSON_COURSE_ID_KEY] = courseId
            }

            preferences[CURRENT_LESSON_SAVED_AT_KEY] = Instant.now().toString()
        }
    }

    suspend fun clearCurrentLesson() {
        context.dataStore.edit { preferences ->
            preferences.remove(CURRENT_LESSON_ID_KEY)
            preferences.remove(CURRENT_LESSON_TITLE_KEY)
            preferences.remove(CURRENT_LESSON_UNIT_ID_KEY)
            preferences.remove(CURRENT_LESSON_COURSE_ID_KEY)
            preferences.remove(CURRENT_LESSON_SAVED_AT_KEY)
        }
    }

    suspend fun getCurrentLesson(): CurrentLessonData? {
        val preferences = context.dataStore.data.first()
        val lessonId = preferences[CURRENT_LESSON_ID_KEY] ?: return null
        return CurrentLessonData(
            lessonId = lessonId,
            lessonTitle = preferences[CURRENT_LESSON_TITLE_KEY],
            unitId = preferences[CURRENT_LESSON_UNIT_ID_KEY],
            courseId = preferences[CURRENT_LESSON_COURSE_ID_KEY],
            savedAtIso = preferences[CURRENT_LESSON_SAVED_AT_KEY]
        )
    }
    
    /**
     * Get current course ID
     */
    suspend fun getCurrentCourseId(): String? {
        return context.dataStore.data.first()[CURRENT_COURSE_ID_KEY]
    }
    
    /**
     * Update current course ID
     */
    suspend fun updateCurrentCourseId(courseId: String?) {
        context.dataStore.edit { preferences ->
            if (courseId.isNullOrBlank()) {
                preferences.remove(CURRENT_COURSE_ID_KEY)
            } else {
                preferences[CURRENT_COURSE_ID_KEY] = courseId
            }
        }
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
     * Get ISO date string when streak celebration screen was last shown
     */
    suspend fun getStreakScreenLastShownDate(): String? {
        return context.dataStore.data.first()[STREAK_SCREEN_LAST_SHOWN_KEY]
    }

    /**
     * Save ISO date string for the last streak celebration screen display
     */
    suspend fun setStreakScreenLastShownDate(dateIso: String) {
        context.dataStore.edit { preferences ->
            preferences[STREAK_SCREEN_LAST_SHOWN_KEY] = dateIso
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
    val username: String?,
    val avatarUrl: String?,
    val accessToken: String?,
    val refreshToken: String?,
    val isLoggedIn: Boolean,
    val isPremium: Boolean,
    val streakDays: Int = 0,
    val exp: Int = 0,
    val createdAt: String? = null
)

data class CurrentLessonData(
    val lessonId: String,
    val lessonTitle: String?,
    val unitId: String?,
    val courseId: String?,
    val savedAtIso: String?
)

