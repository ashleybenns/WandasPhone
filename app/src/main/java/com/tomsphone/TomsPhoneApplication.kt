package com.tomsphone

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TomsPhoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Application initialization will go here
    }
}

