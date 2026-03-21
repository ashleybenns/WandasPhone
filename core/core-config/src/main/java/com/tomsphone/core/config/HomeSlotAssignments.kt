package com.tomsphone.core.config

/**
 * Home screen slot assignment encoding.
 * 8 slots total: 7 assignable (1–7) + slot 8 = Emergency (fixed).
 * Stored as List<String> in CarerSettings.homeSlotAssignments (size 7).
 */
object HomeSlotAssignments {
    const val SLOT_COUNT = 7
    const val TOTAL_SLOTS = 8  // 7 assignable + Emergency

    const val EMPTY = ""
    const val PREFIX_CONTACT = "c:"
    const val MISSED_CALL_RETURN = "mcr"
    const val MISSED_CALLS_LIST = "mcl"
    const val OTHER_CONTACTS = "oc"
    const val SCREEN_OFF = "so"

    fun contactSlot(contactId: Long): String = "$PREFIX_CONTACT$contactId"
    fun parseContactId(value: String): Long? =
        if (value.startsWith(PREFIX_CONTACT)) value.removePrefix(PREFIX_CONTACT).toLongOrNull() else null
    fun isContact(value: String): Boolean = value.startsWith(PREFIX_CONTACT)
}
