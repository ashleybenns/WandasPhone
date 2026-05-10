package com.tomsphone.core.data.local.dao

import androidx.room.*
import com.tomsphone.core.data.local.entity.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    
    /** True missed only (not user-declined). Used for grey-list “missed return” and similar. */
    @Query("SELECT * FROM call_logs WHERE type = 'MISSED' AND isRead = 0 ORDER BY timestamp DESC LIMIT :limit")
    fun getMissedCalls(limit: Int): Flow<List<CallLogEntity>>

    /**
     * Missed + declined rows (read and unread). Prefer [getUnreadMissedAndRejected] for outstanding list.
     */
    @Query(
        "SELECT * FROM call_logs WHERE type IN ('MISSED', 'REJECTED') " +
            "ORDER BY timestamp DESC LIMIT :limit"
    )
    fun getMissedCallsList(limit: Int): Flow<List<CallLogEntity>>

    /**
     * Unread missed + declined, newest first. Large [limit] supports deduping to unique callers in code.
     */
    @Query(
        "SELECT * FROM call_logs WHERE type IN ('MISSED', 'REJECTED') AND isRead = 0 " +
            "ORDER BY timestamp DESC LIMIT :limit"
    )
    fun getUnreadMissedAndRejected(limit: Int): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCalls(limit: Int): Flow<List<CallLogEntity>>
    
    @Query("SELECT * FROM call_logs WHERE contactId = :contactId ORDER BY timestamp DESC")
    fun getCallsForContact(contactId: Long): Flow<List<CallLogEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(callLog: CallLogEntity): Long
    
    @Query("UPDATE call_logs SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)
    
    @Query(
        "UPDATE call_logs SET isRead = 1 WHERE phoneNumber = :phoneNumber AND type IN ('MISSED', 'REJECTED')"
    )
    suspend fun markMissedCallsFromNumberAsRead(phoneNumber: String)
    
    @Query("UPDATE call_logs SET isRead = 1 WHERE type IN ('MISSED', 'REJECTED')")
    suspend fun markAllMissedAsRead()
    
    @Query("DELETE FROM call_logs WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
    
    @Query("DELETE FROM call_logs WHERE type = 'MISSED' AND timestamp < :timestamp")
    suspend fun deleteMissedCallsOlderThan(timestamp: Long)

    @Query("DELETE FROM call_logs")
    suspend fun deleteAll()

    /** Snapshot for linking orphan rows to a newly saved contact (same limit scale as [getRecentCalls]). */
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentCallsSnapshot(limit: Int): List<CallLogEntity>

    @Query("UPDATE call_logs SET contactName = :contactName WHERE contactId = :contactId")
    suspend fun updateContactNamesForContactId(contactId: Long, contactName: String)

    @Query(
        "UPDATE call_logs SET contactId = :contactId, contactName = :contactName WHERE id = :rowId"
    )
    suspend fun updateContactLinkage(rowId: Long, contactId: Long, contactName: String)
}

