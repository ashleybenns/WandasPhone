package com.tomsphone.core.tts

/**
 * Predefined TTS messages for common events
 * 
 * Design principle: "Give instructions, not choices"
 * - Simple language, short sentences
 * - Name people (use contact names)
 * - Confirm actions
 * - No jargon
 */
object TTSScripts {
    
    fun greeting(userName: String?): String {
        val name = userName ?: "there"
        return "Hello $name. Tap a name to call them."
    }
    
    fun calling(contactName: String): String {
        return "Calling $contactName."
    }
    
    fun callConnected(contactName: String): String {
        return "$contactName answered."
    }
    
    fun callEnded(): String {
        return "Call ended."
    }
    
    fun incomingCall(callerName: String?, userName: String = "Wanda"): String {
        return if (callerName != null) {
            "$userName, $callerName is calling."
        } else {
            "$userName, that's your phone ringing."
        }
    }
    
    /**
     * Caller announcement only - used after ringtone audio plays.
     * The ringtone already contains "User, that's your phone ringing..."
     * so we just announce who is calling.
     */
    fun callerAnnouncement(callerName: String?): String? {
        return if (callerName != null) {
            "$callerName is calling."
        } else {
            null  // No announcement needed - ringtone already said "phone ringing"
        }
    }
    
    fun missedCallReminder(callerName: String, userName: String = "Wanda"): String {
        return "$userName, you missed a call. Please call $callerName now."
    }
    
    fun batteryLow(percent: Int): String {
        return "Battery is low. Please charge the phone."
    }
    
    fun batteryCharging(): String {
        return "The phone is charging."
    }
    
    fun speakerOn(): String {
        return "Speaker on."
    }
    
    fun speakerOff(): String {
        return "Speaker off."
    }
    
    fun muted(): String {
        return "Muted."
    }
    
    fun unmuted(): String {
        return "Unmuted."
    }
    
    fun volumeLevel(level: Int): String {
        return "Volume $level."
    }
    
    fun emergencyCallStarting(): String {
        return "Calling emergency services."
    }
    
    fun returningHome(): String {
        return "Going back to home screen."
    }
}

