package com.tomsphone.core.telecom

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TelecomModule {
    
    @Binds
    @Singleton
    abstract fun bindCallManager(
        impl: CallManagerImpl
    ): CallManager
}

