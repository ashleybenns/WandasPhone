package com.wandasphone.core.telecom

/**
 * Simplified call states for WandasPhone
 * 
 * Based on Android Telecom API but simplified for user comprehension
 */
enum class CallState {
    IDLE,           // No active call
    DIALING,        // Outgoing call being placed
    RINGING,        // Incoming call ringing
    CONNECTING,     // Call connecting
    ACTIVE,         // Call connected and active
    HOLDING,        // Call on hold
    DISCONNECTING,  // Call ending
    DISCONNECTED    // Call ended
}

/**
 * Call direction
 */
enum class CallDirection {
    INCOMING,
    OUTGOING
}

/**
 * Complete call information
 */
data class CallInfo(
    val callId: String,
    val phoneNumber: String,
    val contactName: String?,
    val contactId: Long?,
    val state: CallState,
    val direction: CallDirection,
    val startTime: Long,
    val isSpeakerOn: Boolean,
    val isMuted: Boolean
)

