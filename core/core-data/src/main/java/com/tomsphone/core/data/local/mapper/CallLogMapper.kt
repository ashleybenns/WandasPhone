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
        type = type.toCallTypeSafe(),
        timestamp = timestamp,
        duration = duration,
        isRead = isRead
    )
}

/** Tolerate unknown enum strings from older DB rows or future migrations. */
private fun String.toCallTypeSafe(): CallType = try {
    CallType.valueOf(this)
} catch (_: IllegalArgumentException) {
    CallType.MISSED
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

