package com.wandasphone.core.data.local.dao

import androidx.room.*
import com.wandasphone.core.data.local.entity.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    
    @Query("SELECT * FROM call_logs WHERE type = 'MISSED' AND isRead = 0 ORDER BY timestamp DESC LIMIT :limit")
    fun getMissedCalls(limit: Int): Flow<List<CallLogEntity>>
    
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCalls(limit: Int): Flow<List<CallLogEntity>>
    
    @Query("SELECT * FROM call_logs WHERE contactId = :contactId ORDER BY timestamp DESC")
    fun getCallsForContact(contactId: Long): Flow<List<CallLogEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(callLog: CallLogEntity): Long
    
    @Query("UPDATE call_logs SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)
    
    @Query("UPDATE call_logs SET isRead = 1 WHERE type = 'MISSED'")
    suspend fun markAllMissedAsRead()
    
    @Query("DELETE FROM call_logs WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}

