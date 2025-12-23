package com.wandasphone.core.tts

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
    
    fun missedCallReminder(callerName: String, userName: String = "Wanda"): String {
        return "$userName, you missed a call from $callerName. Please call $callerName now."
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

