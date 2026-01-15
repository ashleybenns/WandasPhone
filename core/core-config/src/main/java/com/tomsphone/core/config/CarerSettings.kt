package com.tomsphone.core.config

import kotlinx.serialization.Serializable

/**
 * All carer-configurable settings
 * 
 * User never sees or changes these settings.
 * Only accessible via PIN-protected carer mode.
 * 
 * Settings are organized into logical groups:
 * - User identity
 * - Call handling
 * - Interaction (tap modes, protection)
 * - UI appearance
 * - Safety & security
 * - Audio & TTS
 */
@Serializable
data class CarerSettings(
    // ========== USER IDENTITY ==========
    val userName: String = "Tom",
    val featureLevel: FeatureLevel = FeatureLevel.MINIMAL,
    
    // ========== CONTACTS ==========
    val primaryContactId: Long? = null,
    val maxContactsOnHome: Int = 2,  // 1-6 contacts on home screen
    
    // ========== CALL HANDLING ==========
    val autoAnswerEnabled: Boolean = false,
    val autoAnswerContacts: Set<Long> = emptySet(),  // Specific contacts, empty = all carers
    val autoAnswerDelaySeconds: Int = 3,  // 1-10 seconds before auto-answer
    val speakerphoneAlwaysOn: Boolean = true,
    val speakerVolume: Int = 80,  // 0-100 percent
    val rejectUnknownCalls: Boolean = true,  // Silently reject non-whitelist calls
    
    // ========== MISSED CALL NAGGING ==========
    val missedCallNagEnabled: Boolean = true,
    val missedCallNagInterval: MissedCallNagInterval = MissedCallNagInterval.IMMEDIATE_THEN_MINUTE,
    val missedCallNagOnlyCarers: Boolean = true,  // Only nag for carer contacts
    val missedCallNagSound: NagSound = NagSound.TANNOY_BINGBONG,
    
    // ========== INTERACTION CONFIG ==========
    val interaction: InteractionConfig = InteractionConfig(),
    
    // ========== UI APPEARANCE ==========
    val ui: UIConfig = UIConfig(),
    
    // ========== AUDIO & TTS ==========
    val ttsEnabled: Boolean = true,
    val ttsSpeed: Float = 1.0f,  // 0.5 - 2.0
    val ttsVolume: Int = 100,    // 0-100 percent
    val ringtone: RingtoneOption = RingtoneOption.OLD_TWOBELL,
    val ringtoneVolume: Int = 100,  // 0-100 percent
    
    // ========== SAFETY & SECURITY ==========
    val carerPin: String = "",  // Hashed
    val settingsAccessTapCount: Int = 7,  // Taps on clock to access settings
    val emergencyNumber: String = "999",  // UK default
    val emergencyTapCount: Int = 3,  // Required taps for emergency
    val inactivityTimeoutSeconds: Int = 120,  // Return to home after inactivity
    
    // ========== KIOSK MODE ==========
    val kioskModeEnabled: Boolean = false,  // Full kiosk (requires device owner)
    val allowStatusBar: Boolean = false,
    val allowNavigationBar: Boolean = false,
    
    // ========== ALWAYS ON MODE ==========
    // For use on charging stand - phone is always visible and ready
    val pinnedModeEnabled: Boolean = false,  // Simple app pinning (no device owner needed)
    val screenAlwaysOn: Boolean = true,      // Keep screen on (especially when charging)
    val lockVolumeButtons: Boolean = true,   // Prevent accidental volume changes
    val screenBrightness: Int = 80           // 0-100 percent (when controlled)
)

/**
 * Interaction configuration - how buttons respond to touch
 * 
 * Different users have different motor abilities:
 * - Some accidentally tap, need protection
 * - Some have tremors, need debounce
 * - Some need immediate response, others need confirmation
 */
@Serializable
data class InteractionConfig(
    // ===== GENERAL TAP BEHAVIOR =====
    val touchActivation: TouchActivation = TouchActivation.ON_RELEASE,
    val debounceMs: Int = 300,  // Ignore rapid repeated taps
    
    // ===== CONTACT BUTTON BEHAVIOR =====
    val contactTapMode: TapMode = TapMode.SINGLE_TAP,
    val contactLongPressMs: Int = 500,  // For LONG_PRESS mode
    val contactConfirmDialog: Boolean = false,  // Show "Call [name]?" dialog
    
    // ===== END CALL BUTTON =====
    val endCallTapMode: TapMode = TapMode.DOUBLE_TAP,  // Protect against accidental hang-up
    val endCallLongPressMs: Int = 1000,
    
    // ===== EMERGENCY BUTTON =====
    val emergencyTapMode: TapMode = TapMode.TRIPLE_TAP,
    val emergencyTapTimeoutMs: Int = 5000,  // Time window for multi-tap
    val emergencyConfirmScreen: Boolean = true,  // Show confirmation before dialing
    
    // ===== SCREEN EDGE PROTECTION =====
    val inertBorderEnabled: Boolean = true,
    val inertBorderWidthDp: Int = 28,  // Dead zone around screen edges
    
    // ===== IN-CALL PROTECTION =====
    val lockScreenDuringCall: Boolean = false,  // Prevent any button presses during call
    val proximityLockEnabled: Boolean = true   // Lock when phone near face
)

/**
 * UI appearance configuration
 */
@Serializable
data class UIConfig(
    // ===== THEME =====
    val theme: ThemeOption = ThemeOption.HIGH_CONTRAST_LIGHT,
    
    // ===== BUTTON SIZES =====
    val buttonSize: ButtonSize = ButtonSize.EXTRA_LARGE,
    val contactPhotoSize: PhotoSize = PhotoSize.LARGE,
    
    // ===== HOME SCREEN =====
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val clockFormat: ClockFormat = ClockFormat.TWELVE_HOUR,
    
    // ===== FONTS =====
    val fontScale: Float = 1.0f,  // 0.8 - 1.5 multiplier
    
    // ===== IN-CALL SCREEN =====
    val showCallDuration: Boolean = true,
    val showCallerPhoto: Boolean = true
)

// ========== ENUMS ==========

enum class TouchActivation {
    ON_PRESS,   // Activates immediately on touch (faster but accident-prone)
    ON_RELEASE  // Activates when finger lifts (traditional, safer)
}

enum class TapMode {
    SINGLE_TAP,      // One tap (default for most buttons)
    DOUBLE_TAP,      // Two taps within 500ms
    TRIPLE_TAP,      // Three taps within timeout
    LONG_PRESS,      // Hold for configurable duration
    TAP_AND_CONFIRM  // Tap, then confirm in dialog
}

enum class ButtonSize {
    LARGE,        // 72dp minimum
    EXTRA_LARGE,  // 96dp minimum
    MASSIVE       // 120dp+ for severe motor impairment
}

enum class PhotoSize {
    SMALL,   // 48dp
    MEDIUM,  // 72dp
    LARGE,   // 96dp
    HUGE     // 120dp
}

enum class ClockFormat {
    TWELVE_HOUR,   // 2:30 PM
    TWENTY_FOUR_HOUR  // 14:30
}

enum class ThemeOption {
    HIGH_CONTRAST_LIGHT,   // Black on white
    HIGH_CONTRAST_DARK,    // White on black
    YELLOW_BLACK,          // For visual impairment
    SOFT_CONTRAST          // Gentler colors
}

enum class RingtoneOption {
    OLD_TWOBELL,    // Classic phone ring with TTS
    CLASSIC,        // Simple ring
    GENTLE_CHIME,   // Softer for anxiety
    LOUD_ALERT      // For hearing impaired
}

enum class NagSound {
    TANNOY_BINGBONG,  // Attention-getting "bing bong"
    GENTLE_CHIME,     // Softer reminder
    SPOKEN_ONLY,      // Just TTS, no sound
    NONE              // Silent (just visual)
}

/**
 * Missed call nag interval options
 */
enum class MissedCallNagInterval(
    val initialDelaySeconds: Int,
    val repeatIntervalSeconds: Int,
    val displayName: String
) {
    IMMEDIATE_THEN_MINUTE(
        initialDelaySeconds = 5,
        repeatIntervalSeconds = 60,
        displayName = "Immediate, then every minute"
    ),
    EVERY_2_MINUTES(
        initialDelaySeconds = 120,
        repeatIntervalSeconds = 120,
        displayName = "Every 2 minutes"
    ),
    EVERY_5_MINUTES(
        initialDelaySeconds = 300,
        repeatIntervalSeconds = 300,
        displayName = "Every 5 minutes"
    ),
    EVERY_10_MINUTES(
        initialDelaySeconds = 600,
        repeatIntervalSeconds = 600,
        displayName = "Every 10 minutes"
    )
}
