package com.tomsphone.core.analytics

/**
 * Sealed class defining all trackable analytics events.
 * Events are anonymized - no PII (names, phone numbers, locations) is collected.
 */
sealed class AnalyticsEvent(
    val name: String,
    val params: Map<String, Any> = emptyMap()
) {
    // ===== CARER UX EVENTS =====
    
    /** Carer entered settings mode */
    object CarerSettingsOpened : AnalyticsEvent("carer_settings_opened")
    
    /** A setting was changed */
    data class SettingChanged(
        val settingName: String,
        val oldValue: String,
        val newValue: String
    ) : AnalyticsEvent(
        "setting_changed",
        mapOf(
            "setting_name" to settingName,
            "old_value" to oldValue,
            "new_value" to newValue
        )
    )
    
    /** Contact was added */
    data class ContactAdded(
        val contactType: String // "carer" or "grey_list"
    ) : AnalyticsEvent(
        "contact_added",
        mapOf("contact_type" to contactType)
    )
    
    /** Contact was edited */
    data class ContactEdited(
        val contactType: String
    ) : AnalyticsEvent(
        "contact_edited",
        mapOf("contact_type" to contactType)
    )
    
    /** Contact was deleted */
    data class ContactDeleted(
        val contactType: String
    ) : AnalyticsEvent(
        "contact_deleted",
        mapOf("contact_type" to contactType)
    )
    
    /** Feature level was changed */
    data class FeatureLevelChanged(
        val fromLevel: Int,
        val toLevel: Int
    ) : AnalyticsEvent(
        "feature_level_changed",
        mapOf(
            "from_level" to fromLevel,
            "to_level" to toLevel
        )
    )
    
    /** Onboarding tip was viewed */
    data class OnboardingTipViewed(
        val tipId: String
    ) : AnalyticsEvent(
        "onboarding_tip_viewed",
        mapOf("tip_id" to tipId)
    )
    
    // ===== USER BEHAVIOR EVENTS =====
    
    /** Call was initiated by user */
    data class CallInitiated(
        val contactType: String // "carer" or "grey_list"
    ) : AnalyticsEvent(
        "call_initiated",
        mapOf("contact_type" to contactType)
    )
    
    /** Call was completed */
    data class CallCompleted(
        val durationSeconds: Int,
        val wasAnswered: Boolean,
        val wasOutgoing: Boolean
    ) : AnalyticsEvent(
        "call_completed",
        mapOf(
            "duration_seconds" to durationSeconds,
            "was_answered" to wasAnswered,
            "was_outgoing" to wasOutgoing
        )
    )
    
    /** Call was missed */
    data class CallMissed(
        val contactType: String, // "carer", "grey_list", "unknown"
        val wasNagged: Boolean
    ) : AnalyticsEvent(
        "call_missed",
        mapOf(
            "contact_type" to contactType,
            "was_nagged" to wasNagged
        )
    )
    
    /** Missed call was returned */
    object MissedCallReturned : AnalyticsEvent("missed_call_returned")
    
    /** Button was tapped on home screen */
    data class ButtonTap(
        val buttonType: String, // "contact", "emergency", "missed_calls", "contacts_list", "screen_off"
        val activationMethod: String // "on_release", "on_press", "accumulated_tap"
    ) : AnalyticsEvent(
        "button_tap",
        mapOf(
            "button_type" to buttonType,
            "activation_method" to activationMethod
        )
    )
    
    /** Screen was viewed */
    data class ScreenViewed(
        val screenName: String
    ) : AnalyticsEvent(
        "screen_viewed",
        mapOf("screen_name" to screenName)
    )
    
    // ===== TECHNICAL EVENTS =====
    
    /** App was launched */
    data class AppLaunched(
        val featureLevel: Int,
        val contactCount: Int
    ) : AnalyticsEvent(
        "app_launched",
        mapOf(
            "feature_level" to featureLevel,
            "contact_count" to contactCount
        )
    )
    
    /** Auto-answer was triggered */
    object AutoAnswerTriggered : AnalyticsEvent("auto_answer_triggered")
    
    /** Emergency call was attempted */
    data class EmergencyCallAttempted(
        val wasTestMode: Boolean
    ) : AnalyticsEvent(
        "emergency_call_attempted",
        mapOf("was_test_mode" to wasTestMode)
    )
    
    /** TTS announcement was made */
    data class TtsAnnouncement(
        val announcementType: String // "greeting", "calling", "incoming_call", "missed_call", etc.
    ) : AnalyticsEvent(
        "tts_announcement",
        mapOf("announcement_type" to announcementType)
    )
    
    /** Call was answered (incoming) */
    data class CallAnswered(
        val wasAutoAnswer: Boolean
    ) : AnalyticsEvent(
        "call_answered",
        mapOf("was_auto_answer" to wasAutoAnswer)
    )
    
    /** Call was rejected */
    data class CallRejected(
        val reason: String // "user_rejected", "blocked", "unknown_caller"
    ) : AnalyticsEvent(
        "call_rejected",
        mapOf("reason" to reason)
    )
}
