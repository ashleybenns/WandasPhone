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
     * Verify carer PIN
     */
    suspend fun verifyPin(hashedPin: String): Boolean
    
    /**
     * Set carer PIN
     */
    suspend fun setPin(hashedPin: String): Result<Unit>
}

