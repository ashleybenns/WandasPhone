package com.tomsphone

import com.tomsphone.core.billing.BillingDebugConfig
import com.tomsphone.core.billing.BillingSkuConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppBillingModule {

    @Provides
    @Singleton
    fun provideBillingSkuConfig(): BillingSkuConfig = BillingSkuConfig(
        standardLifetimeProductId = BuildConfig.BILLING_PRODUCT_LIFETIME_STANDARD,
        earlyAdopterLifetimeProductId = BuildConfig.BILLING_PRODUCT_LIFETIME_EARLY
    )

    @Provides
    @Singleton
    fun provideBillingDebugConfig(): BillingDebugConfig = BillingDebugConfig(
        debugEntitlementBypass = BuildConfig.BILLING_DEBUG_ENTITLEMENT_BYPASS
    )

    @Provides
    @Named("play_review_license_fallback")
    fun providePlayReviewLicenseFallback(): String = BuildConfig.PLAY_REVIEW_LICENSE_FALLBACK.trim()
}
