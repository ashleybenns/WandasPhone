package com.tomsphone

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tomsphone.core.analytics.RemoteConfigManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class TomsPhoneApplication : Application() {
    
    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        private const val TAG = "TomsPhoneApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase initialized")
            
            // Configure Crashlytics
            FirebaseCrashlytics.getInstance().apply {
                // Disable collection in debug builds if needed
                // setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
                Log.d(TAG, "Crashlytics configured")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase", e)
        }
        
        // Fetch remote config in background
        applicationScope.launch {
            try {
                remoteConfigManager.fetchAndActivate()
                Log.d(TAG, "Remote config fetched")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch remote config", e)
            }
        }
    }
}

