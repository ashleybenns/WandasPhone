package com.tomsphone.core.billing

/**
 * Derived paywall / trial state for UI and gating.
 */
data class EntitlementSnapshot(
    /** Trial active, purchased, review bypass, or debug bypass. */
    val hasFullAccess: Boolean,
    /** True only during the free month (not after purchase). */
    val isTrialPeriod: Boolean,
    val trialEndsAtMillis: Long?,
    /** Whole days left including the current day (1 = last day). Null when not in trial. */
    val trialDaysRemainingInclusive: Int?,
    val ownedProductId: String?,
    val isReviewBypass: Boolean,
    val isDebugBypass: Boolean
) {
    val needsPaywall: Boolean get() = !hasFullAccess

    /** Show banners + optional TTS to assistants during the final week of trial. */
    val shouldNudgeAssistantsAboutTrial: Boolean
        get() {
            if (!isTrialPeriod) return false
            val d = trialDaysRemainingInclusive ?: return false
            return d in 1..7
        }
}
