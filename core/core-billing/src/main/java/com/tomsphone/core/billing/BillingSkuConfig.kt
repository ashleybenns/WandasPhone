package com.tomsphone.core.billing

/**
 * Play Console in-app product IDs (one-time / non-consumable).
 * Supplied from the app module (Hilt) so IDs match Play Console.
 *
 * [earlyAdopterLifetimeProductId] may be blank when only a single lifetime product exists in Play.
 */
data class BillingSkuConfig(
    val standardLifetimeProductId: String,
    val earlyAdopterLifetimeProductId: String
)

/**
 * Debug / internal-test flags supplied by the application module.
 */
data class BillingDebugConfig(
    val debugEntitlementBypass: Boolean
)
