package com.wandasphone.core.config

import kotlinx.serialization.Serializable

/**
 * All carer-configurable settings
 * 
 * User never sees or changes these settings.
 * Only accessible via PIN-protected carer mode.
 */
@Serializable
data class CarerSettings(
    // Feature level
    val featureLevel: FeatureLevel = FeatureLevel.MINIMAL,
    
    // User
    val userName: String = "Wanda",
    
    // Security
    val carerPin: String = "",  // Hashed
    val settingsTapCount: Int = 7,
    
    // Contacts
    val primaryContactId: Long? = null,
    
    // Call handling
    val autoAnswerEnabled: Boolean = false,
    val autoAnswerRings: Int = 3,
    val answerUnknownCalls: Boolean = false,
    val speakerVolume: Int = 8,  // 1-10
    val missedCallNagIntervalMinutes: Int = 5,
    
    // Touch configuration
    val touchActivation: TouchActivation = TouchActivation.ON_PRESS,
    val touchMinHoldMs: Int = 0,
    val touchDebounceMs: Int = 300,
    val endCallProtection: ButtonProtection = ButtonProtection.DOUBLE_TAP,
    val inertBorderDp: Int = 28,
    
    // Audio
    val ttsSpeed: Float = 1.0f,  // 0.5 - 2.0
    
    // Safety
    val inactivityTimeoutSeconds: Int = 120,  // 2 minutes
    val emergencyTapCount: Int = 3,
    val emergencyNumber: String = "911",
    
    // Theme
    val themeOption: Int = 0  // 0=Light, 1=Dark, 2=Yellow, 3=Soft
)

enum class TouchActivation {
    ON_PRESS,   // Wanda-type: activates on touch
    ON_RELEASE  // Traditional: activates on release
}

enum class ButtonProtection {
    SINGLE_TAP,
    DOUBLE_TAP,
    HOLD_TO_ACTIVATE,
    TAP_AND_CONFIRM
}

