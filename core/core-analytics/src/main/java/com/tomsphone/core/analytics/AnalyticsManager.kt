package com.tomsphone.core.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for analytics tracking.
 * Allows for different implementations (Firebase, debug, etc.)
 */
interface AnalyticsManager {
    /** Log an analytics event */
    fun logEvent(event: AnalyticsEvent)
    
    /** Set a user property (anonymized) */
    fun setUserProperty(name: String, value: String)
    
    /** Log a non-fatal error */
    fun logError(throwable: Throwable, message: String? = null)
    
    /** Set custom keys for crash reports */
    fun setCrashlyticsKey(key: String, value: String)
    fun setCrashlyticsKey(key: String, value: Int)
    fun setCrashlyticsKey(key: String, value: Boolean)
}

/**
 * Firebase implementation of AnalyticsManager.
 * All data is anonymized - no PII is collected.
 */
@Singleton
class FirebaseAnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) : AnalyticsManager {
    
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }
    
    private val crashlytics: FirebaseCrashlytics by lazy {
        FirebaseCrashlytics.getInstance()
    }
    
    companion object {
        private const val TAG = "Analytics"
    }
    
    override fun logEvent(event: AnalyticsEvent) {
        try {
            val bundle = Bundle().apply {
                event.params.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Boolean -> putBoolean(key, value)
                        else -> putString(key, value.toString())
                    }
                }
            }
            
            firebaseAnalytics.logEvent(event.name, bundle)
            Log.d(TAG, "Event logged: ${event.name} with params: ${event.params}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log event: ${event.name}", e)
        }
    }
    
    override fun setUserProperty(name: String, value: String) {
        try {
            firebaseAnalytics.setUserProperty(name, value)
            Log.d(TAG, "User property set: $name = $value")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user property: $name", e)
        }
    }
    
    override fun logError(throwable: Throwable, message: String?) {
        try {
            message?.let { crashlytics.log(it) }
            crashlytics.recordException(throwable)
            Log.e(TAG, "Error logged: ${message ?: throwable.message}", throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log error", e)
        }
    }
    
    override fun setCrashlyticsKey(key: String, value: String) {
        try {
            crashlytics.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set crashlytics key: $key", e)
        }
    }
    
    override fun setCrashlyticsKey(key: String, value: Int) {
        try {
            crashlytics.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set crashlytics key: $key", e)
        }
    }
    
    override fun setCrashlyticsKey(key: String, value: Boolean) {
        try {
            crashlytics.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set crashlytics key: $key", e)
        }
    }
}

/**
 * Debug/No-op implementation for testing or when analytics is disabled.
 */
class DebugAnalyticsManager : AnalyticsManager {
    companion object {
        private const val TAG = "DebugAnalytics"
    }
    
    override fun logEvent(event: AnalyticsEvent) {
        Log.d(TAG, "Event: ${event.name} | Params: ${event.params}")
    }
    
    override fun setUserProperty(name: String, value: String) {
        Log.d(TAG, "User property: $name = $value")
    }
    
    override fun logError(throwable: Throwable, message: String?) {
        Log.e(TAG, "Error: ${message ?: "No message"}", throwable)
    }
    
    override fun setCrashlyticsKey(key: String, value: String) {
        Log.d(TAG, "Crashlytics key: $key = $value")
    }
    
    override fun setCrashlyticsKey(key: String, value: Int) {
        Log.d(TAG, "Crashlytics key: $key = $value")
    }
    
    override fun setCrashlyticsKey(key: String, value: Boolean) {
        Log.d(TAG, "Crashlytics key: $key = $value")
    }
}
