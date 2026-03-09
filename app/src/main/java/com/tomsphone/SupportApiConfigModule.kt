package com.tomsphone

import com.tomsphone.feature.carer.support.SupportApiBaseUrl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupportApiConfigModule {
    @Provides
    @Singleton
    @SupportApiBaseUrl
    fun provideSupportApiBaseUrl(): String = BuildConfig.SUPPORT_API_BASE_URL
}
