package com.tomsphone.core.data.local

import com.tomsphone.core.data.local.dao.ContactDao
import com.tomsphone.core.data.local.mapper.toContact
import com.tomsphone.core.data.local.mapper.toEntity
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.repository.ContactRepository
import com.tomsphone.core.data.util.PhoneNumberUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Local (Room) implementation of ContactRepository
 * Phase 1: Local storage only
 * Phase 2: Can swap for cloud-backed implementation
 */
class LocalContactRepository @Inject constructor(
    private val contactDao: ContactDao
) : ContactRepository {
    
    override fun getContacts(limit: Int): Flow<List<Contact>> {
        return contactDao.getContacts(limit).map { list -> 
            list.map { it.toContact() }
        }
    }
    
    override fun getCarerContacts(limit: Int): Flow<List<Contact>> {
        return contactDao.getCarerContacts(limit).map { list ->
            list.map { it.toContact() }
        }
    }

    override suspend fun getCarerContactsWithBatteryAlerts(): List<Contact> {
        return contactDao.getCarerContactsWithBatteryAlerts().map { it.toContact() }
    }

    override suspend fun getContactsWithBatteryAlertsEnabled(): List<Contact> {
        return contactDao.getContactsWithBatteryAlertsEnabled().map { it.toContact() }
    }
    
    override fun getGreyListContacts(limit: Int): Flow<List<Contact>> {
        return contactDao.getGreyListContacts(limit).map { list ->
            list.map { it.toContact() }
        }
    }
    
    override fun getContactById(id: Long): Flow<Contact?> {
        return contactDao.getContactByIdFlow(id).map { it?.toContact() }
    }
    
    /**
     * Find contact by phone number with flexible matching
     * Handles different formats: +44, 0, spacing variations
     */
    override fun getContactByPhone(phoneNumber: String): Flow<Contact?> {
        // Get all contacts and find matching one
        // This is more flexible than SQL exact match
        return contactDao.getContacts(100).map { contacts ->
            contacts.find { entity ->
                PhoneNumberUtils.isMatch(entity.phoneNumber, phoneNumber)
            }?.toContact()
        }
    }
    
    override suspend fun addContact(contact: Contact): Result<Long> {
        return runCatching {
            val entity = contact.toEntity()
            contactDao.insert(entity)
        }
    }
    
    override suspend fun updateContact(contact: Contact): Result<Unit> {
        return runCatching {
            val entity = contact.toEntity()
            contactDao.update(entity)
        }
    }
    
    override suspend fun removeContact(id: Long): Result<Unit> {
        return runCatching {
            contactDao.deleteById(id)
        }
    }

    override suspend fun deleteAllContacts(): Result<Unit> {
        return runCatching {
            contactDao.deleteAll()
        }
    }
    
    override suspend fun getContactCount(): Int {
        return contactDao.getContactCount()
    }
    
    override suspend fun updateButtonPositions(contactPositions: List<Pair<Long, Int>>): Result<Unit> {
        return runCatching {
            contactPositions.forEach { (contactId, position) ->
                contactDao.updateButtonPosition(contactId, position)
            }
        }
    }
}

