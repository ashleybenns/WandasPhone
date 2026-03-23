package com.tomsphone.feature.home

import com.tomsphone.core.data.model.CallLogEntry
import com.tomsphone.core.data.model.Contact

/**
 * One row in the assistant missed-calls list: log line + resolved in-app contact (if any).
 * [contact] null means the number is not in the app — allowed unknown caller → show generic blue row.
 */
data class MissedCallListRow(
    val call: CallLogEntry,
    val contact: Contact?
)
