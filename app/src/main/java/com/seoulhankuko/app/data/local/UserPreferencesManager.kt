package com.seoulhankuko.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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

