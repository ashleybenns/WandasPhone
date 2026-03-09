package com.tomsphone.core.analytics

import android.content.Context
import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Firebase Remote Config for dynamic onboarding content.
 * Provides contextual tips and guidance that can be updated without app releases.
 */
@Singleton
class RemoteConfigManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // 1 hour in production
                .build()
            setConfigSettingsAsync(configSettings)
            setDefaultsAsync(defaultValues)
        }
    }
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    companion object {
        private const val TAG = "RemoteConfig"
        
        // Config keys
        private const val KEY_ONBOARDING_TIPS = "onboarding_tips"
        private const val KEY_FEATURE_LEVEL_GUIDE = "feature_level_guide"
        private const val KEY_SETTING_DESCRIPTIONS = "setting_descriptions"
        private const val KEY_RECOMMENDED_DEFAULTS = "recommended_defaults"
        
        // Default JSON content (declared BEFORE defaultValues map)
        private const val DEFAULT_ONBOARDING_TIPS = """
        {
            "auto_answer": "Best for users who cannot reliably press buttons. The phone will ring briefly and announce the caller before answering automatically. Requires explicit consent for privacy.",
            "button_activation": "Choose how buttons respond to touch:\n• On Release: Standard behavior, activates when finger lifts\n• On Press: Activates immediately on touch - good for decisive users\n• Accumulated Tap: For shaky hands - accumulates touch time across multiple taps",
            "feature_level": "Start at Level 1 (Simple) with maximum simplicity. Only increase if the user needs more features and can handle them comfortably.",
            "emergency_button": "The emergency button can be configured to call a designated emergency contact or emergency services. Test mode is available to verify it works without making actual calls.",
            "missed_call_return": "Adds a button that shows the most recent missed call from non-carer contacts. Tap to call them back. Great for users who want to return calls but don't need a full contacts list."
        }
        """
        
        private const val DEFAULT_FEATURE_LEVEL_GUIDE = """
        {
            "level_1": {
                "name": "Simple",
                "description": "Maximum simplicity - up to 4 carer contacts on the home screen. Perfect for users who need only essential calling features with minimal choices.",
                "recommended_for": "Users with significant cognitive impairment, first-time setup, or those who prefer extreme simplicity."
            },
            "level_2": {
                "name": "Comfortable",
                "description": "Adds missed calls list, contacts list, and screen-off button. Good for users comfortable with a few more options.",
                "recommended_for": "Users who can navigate simple menus and want to return missed calls or call occasional contacts."
            },
            "level_3": {
                "name": "Standard",
                "description": "More contact management options and customization. Suitable for users who can handle moderate complexity.",
                "recommended_for": "Users with mild cognitive challenges or elderly users who are fairly tech-comfortable."
            },
            "level_4": {
                "name": "Full",
                "description": "All features available including extended contact lists and advanced settings.",
                "recommended_for": "Users who need full functionality or carers setting up for different user profiles."
            }
        }
        """
        
        private const val DEFAULT_SETTING_DESCRIPTIONS = """
        {
            "pinned_mode": "Keeps the app locked on screen. User cannot accidentally exit to other apps. On Samsung devices, manual pinning may be required via recent apps menu.",
            "tts_enabled": "Text-to-speech announces caller names, button actions, and system status. Helpful for users with vision impairment or who benefit from audio confirmation.",
            "speaker_default": "Calls start with speakerphone on. Good for users who have trouble holding the phone to their ear.",
            "text_alignment": "Left alignment helps users scan lists more easily. Center alignment looks nicer but is harder to scan.",
            "shake_protection": "Ignores very brief touches that may be accidental. Increase delay for users with tremors."
        }
        """
        
        private const val DEFAULT_RECOMMENDED_DEFAULTS = """
        {
            "dementia_mild": {
                "feature_level": 2,
                "tts_enabled": true,
                "speaker_default": true,
                "button_activation": "on_release",
                "shake_protection_ms": 100
            },
            "dementia_moderate": {
                "feature_level": 1,
                "tts_enabled": true,
                "speaker_default": true,
                "button_activation": "accumulated_tap",
                "shake_protection_ms": 200,
                "auto_answer": true
            },
            "elderly_independent": {
                "feature_level": 2,
                "tts_enabled": false,
                "speaker_default": false,
                "button_activation": "on_release",
                "shake_protection_ms": 50
            },
            "motor_impairment": {
                "feature_level": 1,
                "tts_enabled": true,
                "speaker_default": true,
                "button_activation": "accumulated_tap",
                "shake_protection_ms": 300,
                "accumulated_threshold_ms": 800
            }
        }
        """
        
        // Default values map (using the constants above)
        private val defaultValues = mapOf(
            KEY_ONBOARDING_TIPS to DEFAULT_ONBOARDING_TIPS,
            KEY_FEATURE_LEVEL_GUIDE to DEFAULT_FEATURE_LEVEL_GUIDE,
            KEY_SETTING_DESCRIPTIONS to DEFAULT_SETTING_DESCRIPTIONS,
            KEY_RECOMMENDED_DEFAULTS to DEFAULT_RECOMMENDED_DEFAULTS
        )
    }
    
    /**
     * Fetch and activate remote config.
     * Call this on app startup.
     */
    suspend fun fetchAndActivate(): Boolean {
        return try {
            val success = remoteConfig.fetchAndActivate().await()
            _isInitialized.value = true
            Log.d(TAG, "Remote config fetched and activated: $success")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch remote config", e)
            _isInitialized.value = true // Still mark as initialized with defaults
            false
        }
    }
    
    /**
     * Get onboarding tip for a specific setting.
     */
    fun getOnboardingTip(settingId: String): String? {
        return try {
            val json = remoteConfig.getString(KEY_ONBOARDING_TIPS)
            val tips = JSONObject(json)
            if (tips.has(settingId)) tips.getString(settingId) else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get onboarding tip for: $settingId", e)
            null
        }
    }
    
    /**
     * Get all onboarding tips.
     */
    fun getAllOnboardingTips(): Map<String, String> {
        return try {
            val json = remoteConfig.getString(KEY_ONBOARDING_TIPS)
            val tips = JSONObject(json)
            val result = mutableMapOf<String, String>()
            tips.keys().forEach { key ->
                result[key] = tips.getString(key)
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all onboarding tips", e)
            emptyMap()
        }
    }
    
    /**
     * Get feature level guide information.
     */
    fun getFeatureLevelGuide(level: Int): FeatureLevelInfo? {
        return try {
            val json = remoteConfig.getString(KEY_FEATURE_LEVEL_GUIDE)
            val guide = JSONObject(json)
            val levelKey = "level_$level"
            if (guide.has(levelKey)) {
                val levelJson = guide.getJSONObject(levelKey)
                FeatureLevelInfo(
                    name = levelJson.getString("name"),
                    description = levelJson.getString("description"),
                    recommendedFor = levelJson.getString("recommended_for")
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get feature level guide for: $level", e)
            null
        }
    }
    
    /**
     * Get setting description.
     */
    fun getSettingDescription(settingId: String): String? {
        return try {
            val json = remoteConfig.getString(KEY_SETTING_DESCRIPTIONS)
            val descriptions = JSONObject(json)
            if (descriptions.has(settingId)) descriptions.getString(settingId) else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get setting description for: $settingId", e)
            null
        }
    }
    
    /**
     * Get recommended defaults for a user profile.
     */
    fun getRecommendedDefaults(profileId: String): RecommendedDefaults? {
        return try {
            val json = remoteConfig.getString(KEY_RECOMMENDED_DEFAULTS)
            val defaults = JSONObject(json)
            if (defaults.has(profileId)) {
                val profile = defaults.getJSONObject(profileId)
                RecommendedDefaults(
                    featureLevel = profile.optInt("feature_level", 1),
                    ttsEnabled = profile.optBoolean("tts_enabled", true),
                    speakerDefault = profile.optBoolean("speaker_default", true),
                    buttonActivation = profile.optString("button_activation", "on_release"),
                    shakeProtectionMs = profile.optInt("shake_protection_ms", 100),
                    autoAnswer = profile.optBoolean("auto_answer", false),
                    accumulatedThresholdMs = profile.optInt("accumulated_threshold_ms", 500)
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recommended defaults for: $profileId", e)
            null
        }
    }
    
    /**
     * Get all available user profile IDs.
     */
    fun getAvailableProfiles(): List<String> {
        return try {
            val json = remoteConfig.getString(KEY_RECOMMENDED_DEFAULTS)
            val defaults = JSONObject(json)
            defaults.keys().asSequence().toList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get available profiles", e)
            emptyList()
        }
    }
}

/**
 * Feature level information from Remote Config.
 */
data class FeatureLevelInfo(
    val name: String,
    val description: String,
    val recommendedFor: String
)

/**
 * Recommended default settings for a user profile.
 */
data class RecommendedDefaults(
    val featureLevel: Int,
    val ttsEnabled: Boolean,
    val speakerDefault: Boolean,
    val buttonActivation: String,
    val shakeProtectionMs: Int,
    val autoAnswer: Boolean = false,
    val accumulatedThresholdMs: Int = 500
)
