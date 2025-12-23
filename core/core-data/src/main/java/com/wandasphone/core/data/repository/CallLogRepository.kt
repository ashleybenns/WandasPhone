package com.wandasphone.core.data.repository

import com.wandasphone.core.data.model.CallLogEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for call logs
 */
interface CallLogRepository {
    
    fun getMissedCalls(limit: Int): Flow<List<CallLogEntry>>
    
    fun getRecentCalls(limit: Int): Flow<List<CallLogEntry>>
    
    fun getCallsForContact(contactId: Long): Flow<List<CallLogEntry>>
    
    suspend fun logCall(entry: CallLogEntry): Result<Long>
    
    suspend fun markAsRead(id: Long): Result<Unit>
    
    suspend fun markAllMissedAsRead(): Result<Unit>
    
    suspend fun deleteOlderThan(timestamp: Long): Result<Unit>
}

