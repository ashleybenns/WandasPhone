package com.tomsphone.feature.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Boot receiver to auto-start WandasPhone after device restart
 * 
 * Ensures user always returns to the app, even after power cycle
 */
class BootReceiver : BroadcastReceiver() {
    
    private companion object {
        const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed, starting WandasPhone")
            
            // Start MainActivity
            val startIntent = Intent(context, Class.forName("com.tomsphone.MainActivity")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            try {
                context.startActivity(startIntent)
                Log.d(TAG, "Started MainActivity after boot")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start app after boot", e)
            }
        }
    }
}

