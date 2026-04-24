package com.tomsphone.core.billing

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {

    @Binds
    @Singleton
    abstract fun bindEntitlementRepository(
        impl: EntitlementRepositoryImpl
    ): EntitlementRepository
}
