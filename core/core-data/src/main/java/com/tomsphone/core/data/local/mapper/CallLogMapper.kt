package com.tomsphone.core.data.local.mapper

import com.tomsphone.core.data.local.entity.CallLogEntity
import com.tomsphone.core.data.model.CallLogEntry
import com.tomsphone.core.data.model.CallType

/**
 * Mapper between CallLogEntry domain model and CallLogEntity
 */

fun CallLogEntity.toCallLogEntry(): CallLogEntry {
    return CallLogEntry(
        id = id,
        contactId = contactId,
        phoneNumber = phoneNumber,
        contactName = contactName,
        type = CallType.valueOf(type),
        timestamp = timestamp,
        duration = duration,
        isRead = isRead
    )
}

fun CallLogEntry.toEntity(): CallLogEntity {
    return CallLogEntity(
        id = id,
        contactId = contactId,
        phoneNumber = phoneNumber,
        contactName = contactName,
        type = type.name,
        timestamp = timestamp,
        duration = duration,
        isRead = isRead
    )
}

