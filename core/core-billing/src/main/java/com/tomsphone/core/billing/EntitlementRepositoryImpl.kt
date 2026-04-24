package com.tomsphone.core.billing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

private val Context.entitlementDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "wandas_entitlements"
)

private val TRIAL_START_MS = longPreferencesKey("trial_start_ms")
private val OWNED_PRODUCT_ID = stringPreferencesKey("owned_product_id")
private val REVIEW_BYPASS = booleanPreferencesKey("review_bypass")
private val LAST_TRIAL_NUDGE_DAY = stringPreferencesKey("last_trial_nudge_day")

@Singleton
class EntitlementRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val billingDebugConfig: BillingDebugConfig
) : EntitlementRepository {

    private val dataStore = context.entitlementDataStore

    companion object {
        const val TRIAL_DURATION_MS: Long = 30L * 24L * 60L * 60L * 1000L
    }

    override val snapshot: Flow<EntitlementSnapshot> = dataStore.data.map { prefs ->
        buildSnapshot(
            nowMillis = System.currentTimeMillis(),
            trialStartMs = prefs[TRIAL_START_MS],
            ownedProductId = prefs[OWNED_PRODUCT_ID]?.takeIf { it.isNotBlank() },
            reviewBypass = prefs[REVIEW_BYPASS] == true,
            debugBypass = billingDebugConfig.debugEntitlementBypass
        )
    }

    override suspend fun ensureTrialStarted(nowMillis: Long) {
        dataStore.edit { prefs ->
            val current = prefs[TRIAL_START_MS]
            if (current == null || current == 0L) {
                prefs[TRIAL_START_MS] = nowMillis
            }
        }
    }

    override suspend fun setPurchasedProductId(productId: String?) {
        dataStore.edit { prefs ->
            if (productId.isNullOrBlank()) {
                prefs.remove(OWNED_PRODUCT_ID)
            } else {
                prefs[OWNED_PRODUCT_ID] = productId
            }
        }
    }

    override suspend fun setReviewBypass(granted: Boolean) {
        dataStore.edit { prefs ->
            prefs[REVIEW_BYPASS] = granted
        }
    }

    override suspend fun shouldPlayTrialAssistantNudge(nowMillis: Long): Boolean {
        val prefs = dataStore.data.first()
        val snap = buildSnapshot(
            nowMillis = nowMillis,
            trialStartMs = prefs[TRIAL_START_MS],
            ownedProductId = prefs[OWNED_PRODUCT_ID]?.takeIf { it.isNotBlank() },
            reviewBypass = prefs[REVIEW_BYPASS] == true,
            debugBypass = billingDebugConfig.debugEntitlementBypass
        )
        if (!snap.shouldNudgeAssistantsAboutTrial) return false
        val today = utcDayKey(nowMillis)
        val last = prefs[LAST_TRIAL_NUDGE_DAY]
        return last != today
    }

    override suspend fun markTrialAssistantNudgePlayed(nowMillis: Long) {
        dataStore.edit { p ->
            p[LAST_TRIAL_NUDGE_DAY] = utcDayKey(nowMillis)
        }
    }

    private fun buildSnapshot(
        nowMillis: Long,
        trialStartMs: Long?,
        ownedProductId: String?,
        reviewBypass: Boolean,
        debugBypass: Boolean
    ): EntitlementSnapshot {
        if (debugBypass || reviewBypass) {
            return EntitlementSnapshot(
                hasFullAccess = true,
                isTrialPeriod = false,
                trialEndsAtMillis = null,
                trialDaysRemainingInclusive = null,
                ownedProductId = ownedProductId,
                isReviewBypass = reviewBypass,
                isDebugBypass = debugBypass
            )
        }
        if (!ownedProductId.isNullOrBlank()) {
            return EntitlementSnapshot(
                hasFullAccess = true,
                isTrialPeriod = false,
                trialEndsAtMillis = null,
                trialDaysRemainingInclusive = null,
                ownedProductId = ownedProductId,
                isReviewBypass = false,
                isDebugBypass = false
            )
        }
        val start = trialStartMs
        if (start == null || start == 0L) {
            return EntitlementSnapshot(
                hasFullAccess = true,
                isTrialPeriod = true,
                trialEndsAtMillis = null,
                trialDaysRemainingInclusive = null,
                ownedProductId = null,
                isReviewBypass = false,
                isDebugBypass = false
            )
        }
        val end = start + TRIAL_DURATION_MS
        if (nowMillis < end) {
            val daysLeft = inclusiveDaysRemaining(nowMillis, end)
            return EntitlementSnapshot(
                hasFullAccess = true,
                isTrialPeriod = true,
                trialEndsAtMillis = end,
                trialDaysRemainingInclusive = daysLeft,
                ownedProductId = null,
                isReviewBypass = false,
                isDebugBypass = false
            )
        }
        return EntitlementSnapshot(
            hasFullAccess = false,
            isTrialPeriod = false,
            trialEndsAtMillis = end,
            trialDaysRemainingInclusive = 0,
            ownedProductId = null,
            isReviewBypass = false,
            isDebugBypass = false
        )
    }

    private fun inclusiveDaysRemaining(nowMillis: Long, trialEndMillis: Long): Int {
        if (nowMillis >= trialEndMillis) return 0
        val calNow = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = nowMillis }
        val calEnd = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = trialEndMillis }
        calNow.set(Calendar.HOUR_OF_DAY, 0)
        calNow.set(Calendar.MINUTE, 0)
        calNow.set(Calendar.SECOND, 0)
        calNow.set(Calendar.MILLISECOND, 0)
        calEnd.set(Calendar.HOUR_OF_DAY, 0)
        calEnd.set(Calendar.MINUTE, 0)
        calEnd.set(Calendar.SECOND, 0)
        calEnd.set(Calendar.MILLISECOND, 0)
        val diff = calEnd.timeInMillis - calNow.timeInMillis
        return (diff / (24L * 60L * 60L * 1000L)).toInt() + 1
    }

    private fun utcDayKey(nowMillis: Long): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = nowMillis }
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }
}
