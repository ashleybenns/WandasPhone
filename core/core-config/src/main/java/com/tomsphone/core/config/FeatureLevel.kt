package com.tomsphone.core.config

/**
 * Feature levels based on interaction complexity
 * 
 * Levels are defined by HOW the user interacts, not just what features are available.
 */
enum class FeatureLevel(val level: Int) {
    MINIMAL(1),     // One-touch only
    BASIC(2),       // + Toggle controls
    STANDARD(3),    // + Two-touch navigation
    EXTENDED(4);    // + Text input
    
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
    // Level 1 - One touch
    PRIMARY_CONTACT(FeatureLevel.MINIMAL),
    AUTO_ANSWER(FeatureLevel.MINIMAL),
    CLOCK(FeatureLevel.MINIMAL),
    BATTERY_TTS(FeatureLevel.MINIMAL),
    EMERGENCY_BUTTON(FeatureLevel.MINIMAL),
    END_CALL_BUTTON(FeatureLevel.MINIMAL),
    
    // Level 2 - Toggles
    CONTACT_GRID_4(FeatureLevel.BASIC),
    SPEAKER_TOGGLE(FeatureLevel.BASIC),
    VOLUME_CONTROLS(FeatureLevel.BASIC),
    MUTE_TOGGLE(FeatureLevel.BASIC),
    
    // Level 3 - Two-touch
    CONTACT_LIST_12(FeatureLevel.STANDARD),
    MISSED_CALLS(FeatureLevel.STANDARD),
    PHOTO_GALLERY(FeatureLevel.STANDARD),
    SMS_READING(FeatureLevel.STANDARD),
    SMS_QUICK_REPLY(FeatureLevel.STANDARD),
    CALENDAR(FeatureLevel.STANDARD),
    
    // Level 4 - Text input
    CONTACT_UNLIMITED(FeatureLevel.EXTENDED),
    SMS_COMPOSE(FeatureLevel.EXTENDED),
    CONTACT_SEARCH(FeatureLevel.EXTENDED),
    APP_LAUNCHER(FeatureLevel.EXTENDED),
    MUSIC_PLAYER(FeatureLevel.EXTENDED)
}

