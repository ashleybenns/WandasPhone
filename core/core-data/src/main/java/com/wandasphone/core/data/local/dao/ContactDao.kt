package com.wandasphone.core.data.local.dao

import androidx.room.*
import com.wandasphone.core.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    
    @Query("SELECT * FROM contacts WHERE isPrimary = 1 LIMIT 1")
    fun getPrimaryContact(): Flow<ContactEntity?>
    
    @Query("SELECT * FROM contacts ORDER BY priority ASC, name ASC LIMIT :limit")
    fun getContacts(limit: Int): Flow<List<ContactEntity>>
    
    @Query("SELECT * FROM contacts WHERE phoneNumber = :phoneNumber LIMIT 1")
    fun getContactByPhone(phoneNumber: String): Flow<ContactEntity?>
    
    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: Long): ContactEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity): Long
    
    @Update
    suspend fun update(contact: ContactEntity)
    
    @Delete
    suspend fun delete(contact: ContactEntity)
    
    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("UPDATE contacts SET isPrimary = 0")
    suspend fun clearAllPrimary()
    
    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun getContactCount(): Int
}

