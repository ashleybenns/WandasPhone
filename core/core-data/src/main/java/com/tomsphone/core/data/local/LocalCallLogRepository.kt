package com.tomsphone.core.data.local

import com.tomsphone.core.data.local.dao.CallLogDao
import com.tomsphone.core.data.local.mapper.toCallLogEntry
import com.tomsphone.core.data.local.mapper.toEntity
import com.tomsphone.core.data.model.CallLogEntry
import com.tomsphone.core.data.model.Contact
import com.tomsphone.core.data.util.PhoneNumberUtils
import com.tomsphone.core.data.repository.CallLogRepository
import com.tomsphone.core.data.repository.dedupeOutstandingMissedCalls
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Local (Room) implementation of CallLogRepository
 */
class LocalCallLogRepository @Inject constructor(
    private val callLogDao: CallLogDao
) : CallLogRepository {
    
    override fun getMissedCalls(limit: Int): Flow<List<CallLogEntry>> {
        return callLogDao.getMissedCalls(limit).map { list ->
            list.map { it.toCallLogEntry() }
        }
    }

    override fun getMissedCallsList(limit: Int): Flow<List<CallLogEntry>> {
        return callLogDao.getMissedCallsList(limit).map { list ->
            list.map { it.toCallLogEntry() }
        }
    }

    override fun getOutstandingMissedCallsPerCaller(maxUniqueCallers: Int): Flow<List<CallLogEntry>> {
        val fetchLimit = (maxUniqueCallers * 50).coerceIn(200, 3000)
        return callLogDao.getUnreadMissedAndRejected(fetchLimit).map { list ->
            val entries = list.map { it.toCallLogEntry() }
            dedupeOutstandingMissedCalls(entries, maxUniqueCallers)
        }
    }

    override fun getCallsForNagReminder(limit: Int): Flow<List<CallLogEntry>> {
        return callLogDao.getUnreadMissedAndRejected(limit).map { list ->
            list.map { it.toCallLogEntry() }
        }
    }
    
    override fun getRecentCalls(limit: Int): Flow<List<CallLogEntry>> {
        return callLogDao.getRecentCalls(limit).map { list ->
            list.map { it.toCallLogEntry() }
        }
    }
    
    override fun getCallsForContact(contactId: Long): Flow<List<CallLogEntry>> {
        return callLogDao.getCallsForContact(contactId).map { list ->
            list.map { it.toCallLogEntry() }
        }
    }
    
    override suspend fun logCall(entry: CallLogEntry): Result<Long> {
        return runCatching {
            val entity = entry.toEntity()
            callLogDao.insert(entity)
        }
    }
    
    override suspend fun markAsRead(id: Long): Result<Unit> {
        return runCatching {
            callLogDao.markAsRead(id)
        }
    }
    
    override suspend fun markMissedCallsFromNumberAsRead(phoneNumber: String): Result<Unit> {
        return runCatching {
            callLogDao.markMissedCallsFromNumberAsRead(phoneNumber)
        }
    }
    
    override suspend fun markAllMissedAsRead(): Result<Unit> {
        return runCatching {
            callLogDao.markAllMissedAsRead()
        }
    }
    
    override suspend fun deleteOlderThan(timestamp: Long): Result<Unit> {
        return runCatching {
            callLogDao.deleteOlderThan(timestamp)
        }
    }
    
    override suspend fun deleteMissedCallsOlderThan(timestamp: Long): Result<Unit> {
        return runCatching {
            callLogDao.deleteMissedCallsOlderThan(timestamp)
        }
    }

    override suspend fun deleteAllCallLogs(): Result<Unit> {
        return runCatching {
            callLogDao.deleteAll()
        }
    }

    override suspend fun syncCallLogsWithContact(contact: Contact): Result<Unit> {
        return runCatching {
            if (contact.id == 0L) return@runCatching
            callLogDao.updateContactNamesForContactId(contact.id, contact.name)
            val snapshot = callLogDao.getRecentCallsSnapshot(2000)
            for (entity in snapshot) {
                if (entity.contactId != null) continue
                if (!PhoneNumberUtils.isMatch(entity.phoneNumber, contact.phoneNumber)) continue
                callLogDao.updateContactLinkage(entity.id, contact.id, contact.name)
            }
        }
    }
}

