package com.tomsphone.core.analytics

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for analytics dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    
    @Binds
    @Singleton
    abstract fun bindAnalyticsManager(
        firebaseAnalyticsManager: FirebaseAnalyticsManager
    ): AnalyticsManager
}
