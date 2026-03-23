package com.tomsphone.core.data.repository

import com.tomsphone.core.data.model.CallLogEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for call logs.
 *
 * All rows are stored in Room (`call_logs`) for on-device history and future remote sync.
 */
interface CallLogRepository {
    
    fun getMissedCalls(limit: Int): Flow<List<CallLogEntry>>

    /** Missed + declined for the assistant “Missed calls” screen (read and unread). */
    fun getMissedCallsList(limit: Int): Flow<List<CallLogEntry>>

    /**
     * Outstanding missed/declined: **unread only**, **one row per caller** (latest time each).
     * Cleared when the user returns the call ([markMissedCallsFromNumberAsRead]).
     */
    fun getOutstandingMissedCallsPerCaller(maxUniqueCallers: Int): Flow<List<CallLogEntry>>

    /** Unread missed + user-declined; used for carer missed-call nag only. */
    fun getCallsForNagReminder(limit: Int): Flow<List<CallLogEntry>>
    
    fun getRecentCalls(limit: Int): Flow<List<CallLogEntry>>
    
    fun getCallsForContact(contactId: Long): Flow<List<CallLogEntry>>
    
    suspend fun logCall(entry: CallLogEntry): Result<Long>
    
    suspend fun markAsRead(id: Long): Result<Unit>
    
    suspend fun markMissedCallsFromNumberAsRead(phoneNumber: String): Result<Unit>
    
    suspend fun markAllMissedAsRead(): Result<Unit>
    
    suspend fun deleteOlderThan(timestamp: Long): Result<Unit>
    
    suspend fun deleteMissedCallsOlderThan(timestamp: Long): Result<Unit>
}

