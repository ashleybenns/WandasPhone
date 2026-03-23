package com.tomsphone.core.data.model

/**
 * Stored contact category (legacy ordering / analytics).
 *
 * Who has a home button and assistant features is determined by home screen **slot**
 * assignments (`HomeSlotAssignments`), not this enum alone.
 * New contacts default to [GREY_LIST]; assign a home slot to elevate.
 */
enum class ContactType {
    /** Legacy / migration: historically “assistant” rows; same privileges as others once slotted. */
    CARER,

    /** Default for new contacts: normal contact until given a home screen slot. */
    GREY_LIST
}
