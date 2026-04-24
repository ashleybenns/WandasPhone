package com.tomsphone.core.billing

import kotlinx.coroutines.flow.Flow

interface EntitlementRepository {

    val snapshot: Flow<EntitlementSnapshot>

    suspend fun ensureTrialStarted(nowMillis: Long = System.currentTimeMillis())

    suspend fun setPurchasedProductId(productId: String?)

    suspend fun setReviewBypass(granted: Boolean)

    /**
     * Whether we should speak the trial nudge today (at most once per calendar day in the warning window).
     */
    suspend fun shouldPlayTrialAssistantNudge(nowMillis: Long = System.currentTimeMillis()): Boolean

    suspend fun markTrialAssistantNudgePlayed(nowMillis: Long = System.currentTimeMillis())
}
