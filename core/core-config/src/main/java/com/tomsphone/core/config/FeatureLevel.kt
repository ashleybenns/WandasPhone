package com.tomsphone.core.config

/**
 * Feature levels based on interaction complexity
 * 
 * Levels are defined by HOW the user interacts, not just what features are available.
 * 
 * Level 1: One Touch - simplest interface
 * Level 2: Two Touch - navigation with lists
 */
enum class FeatureLevel(val level: Int) {
    MINIMAL(1),     // Level 1: One Touch
    BASIC(2);       // Level 2: Two Touch
    
    companion object {
        fun fromInt(value: Int): FeatureLevel {
            return entries.firstOrNull { it.level == value } ?: MINIMAL
        }
    }
}

/**
 * Individual features that can be enabled/disabled based on feature level
 */
enum class Feature(val requiredLevel: FeatureLevel) {
    // Level 1 - One Touch
    PRIMARY_CONTACT(FeatureLevel.MINIMAL),
    AUTO_ANSWER(FeatureLevel.MINIMAL),
    CLOCK(FeatureLevel.MINIMAL),
    BATTERY_TTS(FeatureLevel.MINIMAL),
    EMERGENCY_BUTTON(FeatureLevel.MINIMAL),
    END_CALL_BUTTON(FeatureLevel.MINIMAL),
    
    // Level 2 - Two Touch
    CONTACT_GRID_4(FeatureLevel.BASIC),
    SPEAKER_TOGGLE(FeatureLevel.BASIC),
    VOLUME_CONTROLS(FeatureLevel.BASIC),
    MUTE_TOGGLE(FeatureLevel.BASIC)
}

