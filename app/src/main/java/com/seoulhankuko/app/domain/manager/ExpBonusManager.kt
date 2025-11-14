package com.seoulhankuko.app.domain.manager

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpBonusManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("exp_bonus_prefs", Context.MODE_PRIVATE)
    
    private val KEY_EXPIRES_AT = "expires_at"
    
    /**
     * Activate x2 EXP bonus for specified duration
     * If bonus is already active, stack the time
     */
    fun activateBonus(durationMinutes: Int = 15) {
        val now = System.currentTimeMillis()
        val durationMs = durationMinutes * 60 * 1000L
        
        val currentExpiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        
        val newExpiresAt = if (isActive(currentExpiresAt)) {
            // Stack: add to existing time
            currentExpiresAt + durationMs
        } else {
            // New: start from now
            now + durationMs
        }
        
        prefs.edit().putLong(KEY_EXPIRES_AT, newExpiresAt).apply()
    }
    
    /**
     * Check if bonus is currently active
     */
    fun isActive(): Boolean {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        return isActive(expiresAt)
    }
    
    private fun isActive(expiresAt: Long): Boolean {
        return expiresAt > 0 && System.currentTimeMillis() < expiresAt
    }
    
    /**
     * Get remaining time in milliseconds
     */
    fun getRemainingTime(): Long {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        return if (isActive(expiresAt)) {
            maxOf(0, expiresAt - System.currentTimeMillis())
        } else {
            0L
        }
    }
    
    /**
     * Get remaining time in seconds (for countdown display)
     */
    fun getRemainingTimeSeconds(): Long {
        return getRemainingTime() / 1000
    }
    
    /**
     * Calculate EXP with bonus multiplier
     */
    fun calculateExpWithBonus(baseExp: Int): Int {
        return if (isActive()) {
            baseExp * 2
        } else {
            baseExp
        }
    }
    
    /**
     * Get expires_at timestamp (for sync with server)
     */
    fun getExpiresAt(): Long? {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        return if (expiresAt > 0) expiresAt else null
    }
    
    /**
     * Set expires_at from server (for sync)
     */
    fun setExpiresAt(timestamp: Long) {
        prefs.edit().putLong(KEY_EXPIRES_AT, timestamp).apply()
    }
    
    /**
     * Clear bonus (for testing or manual reset)
     */
    fun clearBonus() {
        prefs.edit().remove(KEY_EXPIRES_AT).apply()
    }
}

