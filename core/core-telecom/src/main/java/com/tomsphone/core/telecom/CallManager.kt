package com.tomsphone.core.telecom

import kotlinx.coroutines.flow.StateFlow

/**
 * Main interface for call management
 * 
 * Abstracts Android Telecom API complexities
 */
interface CallManager {
    
    /**
     * Current call state
     */
    val currentCall: StateFlow<CallInfo?>
    
    /**
     * Place an outgoing call
     */
    fun placeCall(phoneNumber: String): Result<Unit>
    
    /**
     * Answer an incoming call
     */
    fun answerCall(): Result<Unit>
    
    /**
     * Reject an incoming call
     */
    fun rejectCall(): Result<Unit>
    
    /**
     * End the current call
     */
    fun endCall(): Result<Unit>
    
    /**
     * Toggle speakerphone
     */
    fun toggleSpeaker(): Result<Unit>
    
    /**
     * Set speaker state
     */
    fun setSpeaker(enabled: Boolean): Result<Unit>
    
    /**
     * Toggle mute
     */
    fun toggleMute(): Result<Unit>
    
    /**
     * Set mute state
     */
    fun setMute(muted: Boolean): Result<Unit>
    
    /**
     * Set audio volume (1-10)
     */
    fun setVolume(level: Int): Result<Unit>
    
    /**
     * Check if we have required permissions
     */
    fun hasPhonePermissions(): Boolean
    
    /**
     * Check if app is default dialer
     */
    fun isDefaultDialer(): Boolean
}

