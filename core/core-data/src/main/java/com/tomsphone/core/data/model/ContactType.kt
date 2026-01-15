package com.tomsphone.core.data.model

/**
 * Contact relationship types
 * 
 * Determines call privileges and missed call behavior:
 * - CARER: Can call out, answer, triggers missed call nag
 * - GREY_LIST: Answer only, no outbound calls, no missed call nag
 */
enum class ContactType {
    /**
     * Primary caregivers (family or professional)
     * - User CAN call them
     * - Phone answers their calls
     * - Missed calls trigger nagging reminders
     */
    CARER,
    
    /**
     * Friends and family who know to contact carer if needed
     * - User CANNOT call them (not shown in call-out UI)
     * - Phone answers their calls
     * - Missed calls do NOT trigger nag (they'll call carer if urgent)
     */
    GREY_LIST
}
