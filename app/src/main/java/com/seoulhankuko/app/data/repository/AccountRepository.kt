package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.database.AppDatabase
import com.seoulhankuko.app.data.database.daos.LoggedAccountDao
import com.seoulhankuko.app.domain.model.LoggedAccount
import com.seoulhankuko.app.domain.model.toDomain
import com.seoulhankuko.app.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val database: AppDatabase
) {
    private val loggedAccountDao: LoggedAccountDao = database.loggedAccountDao()

    /**
     * Get all logged accounts as Flow
     */
    fun getAllAccounts(): Flow<List<LoggedAccount>> {
        return loggedAccountDao.getAllAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Insert or update an account
     */
    suspend fun saveOrUpdateAccount(account: LoggedAccount) {
        val existingAccount = loggedAccountDao.getAccountByEmail(account.email)
        if (existingAccount != null) {
            // Update existing account with new data, preserving the existing ID
            val updatedEntity = existingAccount.copy(
                userId = account.userId,
                displayName = account.displayName,
                photoUrl = account.photoUrl,
                accessToken = account.accessToken,
                refreshToken = account.refreshToken,
                lastLogin = account.lastLogin,
                isActive = account.isActive
            )
            loggedAccountDao.updateAccount(updatedEntity)
        } else {
            // Insert new account
            loggedAccountDao.insertAccount(account.toEntity())
        }
    }

    /**
     * Set an account as active and deactivate others
     */
    suspend fun setActiveAccount(email: String) {
        loggedAccountDao.clearActiveAccounts()
        loggedAccountDao.setActiveAccount(email)
    }

    /**
     * Clear tokens for a specific account (used when account is removed)
     */
    suspend fun clearTokens(email: String) {
        loggedAccountDao.clearTokensForAccount(email)
    }

    /**
     * Clear only access token but keep refresh token (used for logout)
     */
    suspend fun clearAccessTokenOnly(email: String) {
        loggedAccountDao.clearAccessTokenOnly(email)
    }

    /**
     * Update tokens for a specific account
     */
    suspend fun updateTokens(
        email: String,
        accessToken: String?,
        refreshToken: String?,
        lastLogin: Long = System.currentTimeMillis()
    ) {
        loggedAccountDao.updateTokensForAccount(email, accessToken, refreshToken, lastLogin)
    }

    /**
     * Delete an account completely
     */
    suspend fun deleteAccount(email: String) {
        loggedAccountDao.deleteAccountByEmail(email)
    }

    /**
     * Get the currently active account
     */
    suspend fun getActiveAccount(): LoggedAccount? {
        return loggedAccountDao.getActiveAccount()?.toDomain()
    }

    /**
     * Get account by email
     */
    suspend fun getAccountByEmail(email: String): LoggedAccount? {
        return loggedAccountDao.getAccountByEmail(email)?.toDomain()
    }

    /**
     * Get account count for checking if any accounts exist
     */
    suspend fun getAccountCount(): Int {
        return loggedAccountDao.getAccountCount()
    }

    /**
     * Check if any accounts exist in the database
     */
    suspend fun hasAnyAccounts(): Boolean {
        return getAccountCount() > 0
    }
}
