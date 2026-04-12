package com.tomsphone.core.data.local.dao

import androidx.room.*
import com.tomsphone.core.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    
    @Query("SELECT * FROM contacts ORDER BY priority ASC, name ASC LIMIT :limit")
    fun getContacts(limit: Int): Flow<List<ContactEntity>>
    
    @Query("SELECT * FROM contacts WHERE contactType = 'CARER' ORDER BY buttonPosition ASC, priority ASC, name ASC LIMIT :limit")
    fun getCarerContacts(limit: Int): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE contactType = 'CARER' AND notifyBatteryAlerts = 1")
    suspend fun getCarerContactsWithBatteryAlerts(): List<ContactEntity>

    /** All contacts with battery SMS enabled (filter to home-slot assistants in app layer). */
    @Query("SELECT * FROM contacts WHERE notifyBatteryAlerts = 1")
    suspend fun getContactsWithBatteryAlertsEnabled(): List<ContactEntity>
    
    @Query("SELECT * FROM contacts WHERE contactType = 'GREY_LIST' ORDER BY name ASC LIMIT :limit")
    fun getGreyListContacts(limit: Int): Flow<List<ContactEntity>>
    
    @Query("SELECT * FROM contacts WHERE phoneNumber = :phoneNumber LIMIT 1")
    fun getContactByPhone(phoneNumber: String): Flow<ContactEntity?>
    
    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: Long): ContactEntity?
    
    @Query("SELECT * FROM contacts WHERE id = :id")
    fun getContactByIdFlow(id: Long): Flow<ContactEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity): Long
    
    @Update
    suspend fun update(contact: ContactEntity)
    
    @Delete
    suspend fun delete(contact: ContactEntity)
    
    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun getContactCount(): Int
    
    @Query("UPDATE contacts SET buttonPosition = :position WHERE id = :contactId")
    suspend fun updateButtonPosition(contactId: Long, position: Int)
}

