package com.tomsphone.core.config

import kotlinx.coroutines.flow.Flow

/**
 * Repository for carer settings
 * 
 * Uses DataStore for persistence
 */
interface SettingsRepository {
    
    /**
     * Get current carer settings
     */
    fun getSettings(): Flow<CarerSettings>
    
    /**
     * Update carer settings
     */
    suspend fun updateSettings(settings: CarerSettings): Result<Unit>
    
    /**
     * Get current feature level
     */
    fun getFeatureLevel(): Flow<FeatureLevel>
    
    /**
     * Update feature level
     */
    suspend fun setFeatureLevel(level: FeatureLevel): Result<Unit>
    
    /**
     * Get user name
     */
    fun getUserName(): Flow<String>
    
    /**
     * Check if feature is enabled based on current level
     */
    fun isFeatureEnabled(feature: Feature): Flow<Boolean>
    
    /**
     * Get maximum number of contacts based on level
     */
    fun getMaxContacts(): Flow<Int>
    
    /**
     * Check if auto-answer is allowed.
     * 
     * SECURITY: Auto-answer is only available at Level 2+ (BASIC or higher).
     * This protects Level 1 users who may not understand the privacy implications.
     * 
     * Returns true only if:
     * - Feature level is BASIC or higher AND
     * - autoAnswerEnabled is true in settings
     */
    fun isAutoAnswerAllowed(): Flow<Boolean>
    
    /**
     * Verify carer PIN
     */
    suspend fun verifyPin(hashedPin: String): Boolean
    
    /**
     * Set carer PIN
     */
    suspend fun setPin(hashedPin: String): Result<Unit>
    
    /**
     * Clear all settings (factory reset)
     * 
     * SECURITY: This permanently deletes all settings.
     * Used during factory reset to ensure no user data remains.
     */
    suspend fun clearAllSettings(): Result<Unit>
}

