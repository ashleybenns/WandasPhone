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
    val isPrimary: Boolean,
    val contactType: ContactType,
    val createdAt: Long,
    val updatedAt: Long,
    
    // ========== BUTTON CONFIGURATION ==========
    // Each field is individually addressable for remote sync and paywall gating
    
    /** Button background color (ARGB), null = use theme default */
    val buttonColor: Long? = null,
    
    /** Whether this contact has auto-answer enabled */
    val autoAnswerEnabled: Boolean = false,
    
    /** Position on home screen (0 = top, higher = lower) */
    val buttonPosition: Int = 0,
    
    /** Whether button should be half-width (for split layouts) */
    val isHalfWidth: Boolean = false
) {
    /**
     * Whether this contact can be called by the user
     * (Only carers appear in the call-out UI)
     */
    val canCallOut: Boolean get() = contactType == ContactType.CARER
    
    /**
     * Whether missed calls from this contact trigger nag reminders
     */
    val triggersMissedCallNag: Boolean get() = contactType == ContactType.CARER
}
