package com.tomsphone.core.data.model

/**
 * Domain model for a call log entry
 */
data class CallLogEntry(
    val id: Long,
    val contactId: Long?,
    val phoneNumber: String,
    val contactName: String?,
    val type: CallType,
    val timestamp: Long,
    val duration: Long,
    val isRead: Boolean
)

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED
}

