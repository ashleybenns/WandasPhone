package com.tomsphone.core.telecom

import com.tomsphone.core.config.CarerSettings
import com.tomsphone.core.config.HomeSlotAssignments

/**
 * Contact IDs that have a home call button, aligned with home screen button building:
 * effective 7-slot mode uses slot encodings; otherwise legacy toggles + callable carer order.
 */
fun contactIdsWithHomeCallButton(settings: CarerSettings): Set<Long> {
    val slots = settings.homeSlotAssignments
    // Single layout model: home call buttons are exactly the contact IDs present in home slots.
    // HomeViewModel migrates legacy installs to a 7-entry slot list on startup.
    return HomeSlotAssignments.contactIdsOnHome(slots)
}
