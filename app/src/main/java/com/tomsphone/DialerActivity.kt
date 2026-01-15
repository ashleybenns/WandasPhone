package com.tomsphone

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Dialer Activity - Required for default phone app recognition
 * 
 * This activity handles DIAL intents and redirects to MainActivity.
 * It's transparent (Theme.NoDisplay) so the user sees MainActivity immediately.
 */
class DialerActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Redirect to MainActivity
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            // Pass along any dial data
            data = intent.data
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(mainIntent)
        
        // Finish this transparent activity
        finish()
    }
}
