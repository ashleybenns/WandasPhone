package com.tomsphone.feature.carer.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.analytics.RemoteConfigManager
import com.tomsphone.core.billing.BillingCoordinator
import com.tomsphone.core.billing.BillingSkuConfig
import com.tomsphone.core.billing.EntitlementRepository
import com.tomsphone.core.billing.EntitlementSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val entitlementRepository: EntitlementRepository,
    private val billingCoordinator: BillingCoordinator,
    private val skuConfig: BillingSkuConfig,
    private val remoteConfigManager: RemoteConfigManager,
    @Named("play_review_license_fallback") private val reviewLicenseFallback: String
) : ViewModel() {

    val entitlement: StateFlow<EntitlementSnapshot> = entitlementRepository.snapshot.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        EntitlementSnapshot(
            hasFullAccess = false,
            isTrialPeriod = false,
            trialEndsAtMillis = null,
            trialDaysRemainingInclusive = null,
            ownedProductId = null,
            isReviewBypass = false,
            isDebugBypass = false
        )
    )

    val productDetails = billingCoordinator.productDetailsById
    val billingReady = billingCoordinator.connectionReady

    val standardProductId: String = skuConfig.standardLifetimeProductId
    val earlyAdopterProductId: String = skuConfig.earlyAdopterLifetimeProductId

    fun launchStandardPurchase(activity: Activity) {
        billingCoordinator.launchPurchaseFlow(activity, skuConfig.standardLifetimeProductId)
    }

    fun launchEarlyAdopterPurchase(activity: Activity) {
        if (skuConfig.earlyAdopterLifetimeProductId.isBlank()) return
        billingCoordinator.launchPurchaseFlow(activity, skuConfig.earlyAdopterLifetimeProductId)
    }

    fun refreshPurchases() {
        billingCoordinator.syncOwnedPurchases()
    }

    fun submitReviewCode(code: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val trimmed = code.trim()
            val fromRc = remoteConfigManager.getPlayReviewLicenseCode()
            val ok = (fromRc.isNotEmpty() && fromRc.equals(trimmed, ignoreCase = true)) ||
                (reviewLicenseFallback.isNotEmpty() && reviewLicenseFallback.equals(trimmed, ignoreCase = true))
            if (ok) {
                entitlementRepository.setReviewBypass(true)
            }
            onResult(ok)
        }
    }
}
