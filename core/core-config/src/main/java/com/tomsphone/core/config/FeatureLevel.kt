package com.tomsphone.core.config

/**
 * Feature tiers for future gating (e.g. SMS, corporate remote).
 *
 * The shipped app uses a single tier; see [EFFECTIVE_PRODUCT_TIER].
 * [CarerSettings.featureLevel] remains in storage for future sync / remote assignment
 * but must not drive current product behavior—use [EFFECTIVE_PRODUCT_TIER] instead.
 */
enum class FeatureLevel(val level: Int) {
    MINIMAL(1),
    BASIC(2);

    companion object {
        /**
         * Tier applied everywhere in the current product (UI, calls, carer tools).
         * When additional tiers ship, switch call sites from this constant to the stored/synced level.
         */
        val EFFECTIVE_PRODUCT_TIER: FeatureLevel = BASIC

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

