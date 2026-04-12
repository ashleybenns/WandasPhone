package com.tomsphone.core.config

/**
 * Sync legacy toggle fields and derived counters from a 7-slot home layout list.
 *
 * We still persist the legacy fields for backwards compatibility / remote sync, but runtime layout
 * should treat [CarerSettings.homeSlotAssignments] as the single source of truth.
 */
fun CarerSettings.withSlotsSynced(slots: List<String>): CarerSettings = copy(
    homeSlotAssignments = slots,
    homeContactCount = slots.count { HomeSlotAssignments.isContact(it) },
    homeShowMissedCallReturnButton = slots.contains(HomeSlotAssignments.MISSED_CALL_RETURN),
    homeShowMissedCallsButton = slots.contains(HomeSlotAssignments.MISSED_CALLS_LIST),
    homeShowContactsListButton = slots.contains(HomeSlotAssignments.OTHER_CONTACTS),
    homeShowDialerButton = slots.contains(HomeSlotAssignments.DIALER),
    showDisplayOffButton = slots.contains(HomeSlotAssignments.SCREEN_OFF)
)

