package com.tomsphone.core.data.repository

import com.tomsphone.core.data.model.Contact
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for contacts
 * 
 * Designed for easy Phase 2 swap to cloud-backed implementation
 */
interface ContactRepository {
    
    fun getContacts(limit: Int): Flow<List<Contact>>
    
    /**
     * Get only CARER contacts (those that can be called by user)
     * Ordered by buttonPosition for home screen display
     */
    fun getCarerContacts(limit: Int): Flow<List<Contact>>

    /**
     * Get carer contacts that have battery alert SMS enabled (for low battery / device connected notifications).
     */
    suspend fun getCarerContactsWithBatteryAlerts(): List<Contact>
    
    /**
     * Get only GREY_LIST contacts (answer-only, not on home screen)
     * Ordered alphabetically by name
     */
    fun getGreyListContacts(limit: Int): Flow<List<Contact>>
    
    fun getContactById(id: Long): Flow<Contact?>
    
    fun getContactByPhone(phoneNumber: String): Flow<Contact?>
    
    suspend fun addContact(contact: Contact): Result<Long>
    
    suspend fun updateContact(contact: Contact): Result<Unit>
    
    suspend fun removeContact(id: Long): Result<Unit>
    
    suspend fun getContactCount(): Int
    
    /**
     * Update button positions for a list of contacts
     * Used for drag-to-reorder functionality
     */
    suspend fun updateButtonPositions(contactPositions: List<Pair<Long, Int>>): Result<Unit>
}

