package com.tomsphone.core.config

import kotlinx.serialization.Serializable

/**
 * All carer-configurable settings
 * 
 * User never sees or changes these settings.
 * Only accessible via PIN-protected carer mode.
 * 
 * SECURITY: Default values are designed for FACTORY RESET scenarios.
 * When the app is reset or newly installed, these defaults ensure:
 * - No automatic call answering (privacy protection)
 * - No access to previous user's data
 * - Safe, accessible configuration
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
    // Default: Generic name, carer should personalize
    val userName: String = "User",
    // Default: Minimal features for safety
    val featureLevel: FeatureLevel = FeatureLevel.MINIMAL,
    
    // ========== CONTACTS ==========
    // Default: No primary contact (carer must set up)
    val primaryContactId: Long? = null,
    
    // ========== HOME SCREEN LAYOUT ==========
    // Each setting is discrete for individual remote sync and paywall gating
    val homeMaxButtons: Int = 4,                    // 1-6 contact buttons on home screen
    val homeShowEmergencyButton: Boolean = true,    // SAFE: Emergency always visible
    val homeShowMissedCallsButton: Boolean = false, // Level 2+: Show missed calls list button
    val homeShowContactsListButton: Boolean = false,// Level 2+: Show contacts list button
    val homeMissedCallsButtonColor: Long? = null,   // ARGB, null = theme default
    val homeContactsListButtonColor: Long? = null,  // ARGB, null = theme default
    
    // ========== CALL HANDLING ==========
    // SECURITY CRITICAL: Auto-answer MUST default to false
    // If enabled, anyone could listen without user consent
    val autoAnswerEnabled: Boolean = false,
    // Default: No contacts have auto-answer (even if enabled globally)
    val autoAnswerContacts: Set<Long> = emptySet(),
    val autoAnswerDelaySeconds: Int = 3,  // 1-10 seconds before auto-answer
    // ACCESSIBILITY: Speakerphone on by default for ease of use
    val speakerphoneAlwaysOn: Boolean = true,
    val speakerVolume: Int = 80,  // 0-100 percent
    // SAFETY: Reject unknown calls by default (scam protection)
    val rejectUnknownCalls: Boolean = true,
    
    // ========== MISSED CALL NAGGING ==========
    // SAFE: Reminders on - helps user return important calls
    val missedCallNagEnabled: Boolean = true,
    val missedCallNagInterval: MissedCallNagInterval = MissedCallNagInterval.IMMEDIATE_THEN_MINUTE,
    val missedCallNagOnlyCarers: Boolean = true,  // Only nag for carer contacts
    val missedCallNagSound: NagSound = NagSound.TANNOY_BINGBONG,
    
    // ========== INTERACTION CONFIG ==========
    // Default: Safe interaction settings (see InteractionConfig for details)
    val interaction: InteractionConfig = InteractionConfig(),
    
    // ========== UI APPEARANCE ==========
    // Default: High contrast for accessibility
    val ui: UIConfig = UIConfig(),
    
    // ========== AUDIO & TTS ==========
    // ACCESSIBILITY: TTS on by default for audio-first experience
    val ttsEnabled: Boolean = true,
    val ttsSpeed: Float = 1.0f,  // 0.5 - 2.0
    val ttsVolume: Int = 100,    // Full volume for hearing
    val ringtone: RingtoneOption = RingtoneOption.OLD_TWOBELL,
    val ringtoneVolume: Int = 100,  // Full volume
    
    // ========== SAFETY & SECURITY ==========
    // SECURITY: No PIN set - carer must create one on first access
    val carerPin: String = "",
    // Hidden access: 7 taps on clock (not obvious to user)
    val settingsAccessTapCount: Int = 7,
    val inactivityTimeoutSeconds: Int = 120,  // Return to home after inactivity
    
    // ========== EMERGENCY SETTINGS ==========
    // UK default emergency number
    val emergencyNumber: String = "999",
    // SAFETY: 3 taps required for emergency (prevents accidental calls)
    val emergencyTapCount: Int = 3,
    // SAFETY: Test mode ON by default - prevents accidental real calls during setup
    val emergencyTestMode: Boolean = true,
    
    // ========== USER EMERGENCY INFO ==========
    // Displayed during emergency call for attending EMTs
    val userPhotoUri: String? = null,       // Photo for EMT verification
    val userSurname: String = "",            // Surname for ID verification
    val userAddress: String = "",            // Where to find the user
    val userBloodType: String = "",          // A+, B-, O+, etc.
    val userAllergies: String = "",          // Drug/food allergies
    val userMedications: String = "",        // Current medications
    val userMedicalConditions: String = "",  // Relevant conditions (dementia, diabetes, etc.)
    val userEmergencyNotes: String = "",     // Any other info for EMTs
    
    // Emergency contacts - people to notify in an emergency (not 999)
    val emergencyContact1Name: String = "",
    val emergencyContact1Phone: String = "",
    val emergencyContact2Name: String = "",
    val emergencyContact2Phone: String = "",
    
    // ========== KIOSK MODE ==========
    // SAFE: Kiosk OFF by default (requires device owner setup)
    val kioskModeEnabled: Boolean = false,
    val allowStatusBar: Boolean = false,
    val allowNavigationBar: Boolean = false,
    
    // ========== ALWAYS ON MODE ==========
    // For use on charging stand - phone is always visible and ready
    // SAFE: Pinned mode OFF by default (carer must enable)
    val pinnedModeEnabled: Boolean = false,
    // ACCESSIBILITY: Screen stays on when configured for stand
    val screenAlwaysOn: Boolean = true,
    // SAFE: Volume lock ON to prevent accidental muting
    val lockVolumeButtons: Boolean = true,
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
    
    // ===== USER TEXT SIZE =====
    // Controls text/button scaling on USER-facing screens (Home, Call screens)
    // Carer settings screens always use normal size for readability
    val userTextSize: UserTextSize = UserTextSize.NORMAL,
    
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

/**
 * Predefined button colors for contact buttons.
 * 
 * All colors are high-contrast for white text.
 * Reserved (not in this enum):
 * - Black: calling fade animation
 * - Red: emergency button  
 * - Green: answer call button
 */
enum class ButtonColor(val argb: Long, val displayName: String) {
    DEFAULT(0L, "Default"),           // Use theme primary
    BLUE(0xFF1976D2, "Blue"),         // Primary, trustworthy
    PURPLE(0xFF7B1FA2, "Purple"),     // Distinct, memorable
    ORANGE(0xFFF57C00, "Orange"),     // Warm, attention-getting
    TEAL(0xFF00796B, "Teal"),         // Distinct from blue
    INDIGO(0xFF303F9F, "Indigo"),     // Deep, professional
    BROWN(0xFF5D4037, "Brown");       // Warm, earthy
    
    companion object {
        /**
         * Find ButtonColor by ARGB value, or DEFAULT if not found
         */
        fun fromArgb(argb: Long?): ButtonColor {
            if (argb == null || argb == 0L) return DEFAULT
            return entries.find { it.argb == argb } ?: DEFAULT
        }
    }
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
 * User-facing text size presets
 * 
 * Controls scaling of text AND containers on user screens.
 * Carer settings screens are NOT affected (always normal size).
 */
enum class UserTextSize(
    val scale: Float,
    val displayName: String
) {
    COMPACT(0.8f, "Compact (80%)"),        // For crowded screens with many buttons
    SMALL(0.9f, "Small (90%)"),            // Slightly reduced
    NORMAL(1.0f, "Normal (100%)"),         // Default - comfortable size
    MEDIUM(1.05f, "Medium (105%)"),        // Slightly larger
    LARGE(1.1f, "Large (110%)"),           // Max width for S22
    EXTRA_LARGE(1.15f, "Extra Large (115%)")  // May overflow on smaller screens
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
