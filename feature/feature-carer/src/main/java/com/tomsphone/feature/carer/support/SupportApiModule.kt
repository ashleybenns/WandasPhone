package com.tomsphone.feature.carer.support

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SupportApiModule {
    @Binds
    @Singleton
    abstract fun bindSupportApiClient(impl: SupportApiClientImpl): SupportApiClient
}
