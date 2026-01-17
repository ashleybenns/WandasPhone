package com.tomsphone.core.config

/**
 * Runtime UI model for home screen buttons.
 * 
 * IMPORTANT: This is NOT stored directly - it's built by ViewModel from:
 * - Contact data (stored in Room DB)
 * - CarerSettings (stored in DataStore)
 * 
 * Each underlying setting is discrete and individually addressable for:
 * - Database storage
 * - Remote carer portal modification
 * - Subscription tier / paywall gating
 */
sealed class HomeButtonConfig {
    
    /**
     * A contact call button - tapping calls this contact
     */
    data class ContactButton(
        val contactId: Long,
        val name: String,
        val phoneNumber: String,
        /** Button background color (ARGB), null = use theme default */
        val color: Long?,
        /** Show "Auto-Answer" warning at bottom of button */
        val showAutoAnswerWarning: Boolean,
        /** Half-width for split button layouts (Level 2+) */
        val isHalfWidth: Boolean
    ) : HomeButtonConfig()
    
    /**
     * A menu navigation button - tapping navigates to a list screen
     * Level 2+ feature
     */
    data class MenuButton(
        /** Unique identifier: "missed_calls", "contacts_list", etc. */
        val id: String,
        val label: String,
        /** Button background color (ARGB), null = use theme default */
        val color: Long?,
        /** Half-width for split button layouts */
        val isHalfWidth: Boolean
    ) : HomeButtonConfig() {
        companion object {
            const val ID_MISSED_CALLS = "missed_calls"
            const val ID_CONTACTS_LIST = "contacts_list"
        }
    }
    
    /**
     * Emergency button - always at bottom, distinct styling
     */
    data class EmergencyButton(
        val label: String = "Emergency"
    ) : HomeButtonConfig()
}
