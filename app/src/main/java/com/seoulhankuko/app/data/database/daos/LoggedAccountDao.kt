package com.seoulhankuko.app.data.database.daos

import androidx.room.*
import com.seoulhankuko.app.data.database.entities.LoggedAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoggedAccountDao {
    
    @Query("SELECT * FROM logged_accounts ORDER BY lastLogin DESC")
    fun getAllAccounts(): Flow<List<LoggedAccountEntity>>
    
    @Query("SELECT * FROM logged_accounts WHERE email = :email LIMIT 1")
    suspend fun getAccountByEmail(email: String): LoggedAccountEntity?
    
    @Query("SELECT * FROM logged_accounts WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveAccount(): LoggedAccountEntity?
    
    @Query("SELECT * FROM logged_accounts WHERE userId = :userId LIMIT 1")
    suspend fun getAccountByUserId(userId: Int): LoggedAccountEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: LoggedAccountEntity): Long
    
    @Update
    suspend fun updateAccount(account: LoggedAccountEntity)
    
    @Query("UPDATE logged_accounts SET isActive = 0")
    suspend fun clearActiveAccounts()
    
    @Query("UPDATE logged_accounts SET isActive = 1 WHERE email = :email")
    suspend fun setActiveAccount(email: String)
    
    @Query("UPDATE logged_accounts SET accessToken = NULL, refreshToken = NULL WHERE email = :email")
    suspend fun clearTokensForAccount(email: String)
    
    @Query("UPDATE logged_accounts SET accessToken = NULL WHERE email = :email")
    suspend fun clearAccessTokenOnly(email: String)
    
    @Query("UPDATE logged_accounts SET accessToken = :accessToken, refreshToken = :refreshToken, lastLogin = :lastLogin WHERE email = :email")
    suspend fun updateTokensForAccount(
        email: String, 
        accessToken: String?, 
        refreshToken: String?, 
        lastLogin: Long
    )
    
    @Delete
    suspend fun deleteAccount(account: LoggedAccountEntity)
    
    @Query("DELETE FROM logged_accounts WHERE email = :email")
    suspend fun deleteAccountByEmail(email: String)
    
    @Query("SELECT COUNT(*) FROM logged_accounts")
    suspend fun getAccountCount(): Int
}
