package com.wandasphone.core.tts

/**
 * Text-to-speech interface for WandasPhone
 * 
 * Provides spoken feedback for all user interactions.
 * Priority system ensures important messages are not interrupted.
 */
interface WandasTTS {
    
    /**
     * Speak a message with given priority
     */
    fun speak(message: String, priority: Priority = Priority.NORMAL)
    
    /**
     * Speak immediately, interrupting current speech
     */
    fun speakNow(message: String)
    
    /**
     * Stop current speech
     */
    fun stop()
    
    /**
     * Check if currently speaking
     */
    fun isSpeaking(): Boolean
    
    /**
     * Set speech rate (0.5 - 2.0, 1.0 is normal)
     */
    fun setSpeed(speed: Float)
    
    /**
     * Speech priority levels
     */
    enum class Priority {
        LOW,        // Background information
        NORMAL,     // Standard feedback
        HIGH,       // Important information
        IMMEDIATE   // Critical - interrupts everything
    }
}

