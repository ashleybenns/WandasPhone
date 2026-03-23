package com.tomsphone.core.data.model

/**
 * Domain model for a contact
 */
data class Contact(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val photoUri: String?,
    val priority: Int,
    val contactType: ContactType,
    val createdAt: Long,
    val updatedAt: Long,
    
    // ========== BUTTON CONFIGURATION ==========
    // Each field is individually addressable for remote sync and paywall gating
    
    /** Button background color (ARGB), null = use theme default */
    val buttonColor: Long? = null,
    
    /** Whether this contact has auto-answer enabled */
    val autoAnswerEnabled: Boolean = false,
    
    /** Whether to send this carer SMS for low battery and device connected after low battery */
    val notifyBatteryAlerts: Boolean = false,
    
    /** Position on home screen (0 = top, higher = lower) */
    val buttonPosition: Int = 0,
    
    /** Whether button should be half-width (for split layouts) */
    val isHalfWidth: Boolean = false
) {
    /**
     * Legacy: stored category for carer UI (Assistant vs answer-only lists).
     * Home call buttons, missed-call nag, auto-answer, and battery SMS are driven by
     * whether the contact occupies a home screen slot — see `HomeSlotAssignments.contactIdsOnHome`.
     */
    val canCallOut: Boolean get() = contactType == ContactType.CARER
}
