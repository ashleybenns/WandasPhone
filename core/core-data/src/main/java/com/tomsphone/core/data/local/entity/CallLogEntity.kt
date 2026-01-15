package com.tomsphone.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for call logs (internal to data layer)
 */
@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long?,
    val phoneNumber: String,
    val contactName: String?,
    val type: String,  // INCOMING, OUTGOING, MISSED, REJECTED
    val timestamp: Long,
    val duration: Long,
    val isRead: Boolean
)

