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
    val updatedAt: Long
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

