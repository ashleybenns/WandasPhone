package com.wandasphone

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WandasPhoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Application initialization will go here
    }
}

