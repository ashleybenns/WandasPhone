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
    /** Answered incoming call */
    INCOMING,
    /** Outgoing call (placed from the app) */
    OUTGOING,
    /** Not answered (rang through / no pickup) */
    MISSED,
    /** User declined the call from the incoming screen */
    REJECTED,
    /** Blocked by call screening (e.g. unknown caller when reject-unknown is on) */
    BLOCKED
}

