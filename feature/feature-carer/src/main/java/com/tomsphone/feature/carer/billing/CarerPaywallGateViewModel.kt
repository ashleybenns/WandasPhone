package com.tomsphone.feature.carer.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomsphone.core.billing.EntitlementRepository
import com.tomsphone.core.billing.EntitlementSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CarerPaywallGateViewModel @Inject constructor(
    entitlementRepository: EntitlementRepository
) : ViewModel() {

    val snapshot = entitlementRepository.snapshot.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        EntitlementSnapshot(
            hasFullAccess = true,
            isTrialPeriod = false,
            trialEndsAtMillis = null,
            trialDaysRemainingInclusive = null,
            ownedProductId = null,
            isReviewBypass = false,
            isDebugBypass = false
        )
    )
}
