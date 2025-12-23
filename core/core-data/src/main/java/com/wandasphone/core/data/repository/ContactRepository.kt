package com.wandasphone.core.data.repository

import com.wandasphone.core.data.model.Contact
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for contacts
 * 
 * Designed for easy Phase 2 swap to cloud-backed implementation
 */
interface ContactRepository {
    
    fun getPrimaryContact(): Flow<Contact?>
    
    fun getContacts(limit: Int): Flow<List<Contact>>
    
    fun getContactByPhone(phoneNumber: String): Flow<Contact?>
    
    suspend fun addContact(contact: Contact): Result<Long>
    
    suspend fun updateContact(contact: Contact): Result<Unit>
    
    suspend fun removeContact(id: Long): Result<Unit>
    
    suspend fun setPrimaryContact(id: Long): Result<Unit>
    
    suspend fun getContactCount(): Int
}

