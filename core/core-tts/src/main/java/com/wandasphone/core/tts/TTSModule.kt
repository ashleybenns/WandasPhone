package com.wandasphone.core.tts

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TTSModule {
    
    @Binds
    @Singleton
    abstract fun bindWandasTTS(
        impl: AndroidTTSImpl
    ): WandasTTS
}

