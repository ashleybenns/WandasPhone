package com.tomsphone.core.data.repository

import com.tomsphone.core.data.model.CallLogEntry
import com.tomsphone.core.data.util.PhoneNumberUtils

private const val DEFAULT_REGION = "GB"

/**
 * One row per distinct caller: keeps the most recent unread missed/declined event per normalized number.
 */
internal fun dedupeOutstandingMissedCalls(
    entries: List<CallLogEntry>,
    maxUniqueCallers: Int
): List<CallLogEntry> {
    return entries
        .asSequence()
        .filter { it.phoneNumber.isNotBlank() }
        .groupBy { callerKey(it.phoneNumber) }
        .values
        .map { group -> group.maxByOrNull { it.timestamp }!! }
        .sortedByDescending { it.timestamp }
        .take(maxUniqueCallers)
}

private fun callerKey(phoneNumber: String): String {
    val e164 = PhoneNumberUtils.normalizeToE164(phoneNumber, DEFAULT_REGION)
    return e164.ifEmpty { phoneNumber.trim() }
}
