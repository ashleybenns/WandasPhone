package com.tomsphone.core.telecom

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.emergency.EmergencyNumber
import androidx.annotation.RequiresApi
import java.util.Locale

/** ITU-style GSM default when the device cannot supply a regional code. */
const val DEFAULT_EMERGENCY_FALLBACK = "112"

private val UNVERIFIED_ASSISTANT_NOTICE =
    "Could not confirm the emergency number from this phone or SIM. The app is using 112, which works in many countries. Please confirm the correct local number and enter it in User Profile if needed."

private val GB_ISLES = setOf("GB", "GG", "IM", "JE")

/**
 * Result of resolving which digits to dial for the home emergency action.
 *
 * @param dialDigits Digits only, suitable for [CallManager.placeCall].
 * @param regionSourceVerified True if from the platform emergency list or SIM/network country.
 * @param assistantNotice Shown to carers when [regionSourceVerified] is false.
 * @param suggestedPersistDigits When the stored value is still the generic default (empty or 112) and
 * the device suggests a different code, persist this value (optional sync).
 */
data class EmergencyNumberResolution(
    val dialDigits: String,
    val regionSourceVerified: Boolean,
    val assistantNotice: String?,
    val suggestedPersistDigits: String?
)

object EmergencyNumberResolver {

    fun resolve(context: Context, storedEmergencyRaw: String): EmergencyNumberResolution {
        val storedDigits = storedEmergencyRaw.filter { it.isDigit() }
        val usesRegionalDefault =
            storedDigits.isEmpty() || storedDigits == DEFAULT_EMERGENCY_FALLBACK
        if (!usesRegionalDefault) {
            return EmergencyNumberResolution(
                dialDigits = storedDigits,
                regionSourceVerified = true,
                assistantNotice = null,
                suggestedPersistDigits = null
            )
        }
        val device = resolveFromTelephony(context)
        val effectiveStored =
            if (storedDigits.isEmpty()) DEFAULT_EMERGENCY_FALLBACK else storedDigits
        val suggested =
            if (device.dialDigits != effectiveStored) device.dialDigits else null
        return EmergencyNumberResolution(
            dialDigits = device.dialDigits,
            regionSourceVerified = device.regionSourceVerified,
            assistantNotice = device.assistantNotice,
            suggestedPersistDigits = suggested
        )
    }

    private data class TelephonyResolution(
        val dialDigits: String,
        val regionSourceVerified: Boolean,
        val assistantNotice: String?
    )

    private fun resolveFromTelephony(context: Context): TelephonyResolution {
        val tm = context.getSystemService(TelephonyManager::class.java)
            ?: return unverifiedFallback()
        val platformCodes =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                emergencyNumbersFromPlatform(context, tm)
            } else {
                emptySet()
            }
        val iso = primaryCountryIso(tm)
        pickFromPlatformList(platformCodes, iso)?.let { digits ->
            return TelephonyResolution(
                dialDigits = digits,
                regionSourceVerified = true,
                assistantNotice = null
            )
        }
        val fromIso = iso?.let { shortCodeForCountry(it) }
        if (fromIso != null) {
            return TelephonyResolution(
                dialDigits = fromIso,
                regionSourceVerified = true,
                assistantNotice = null
            )
        }
        return unverifiedFallback()
    }

    private fun unverifiedFallback() = TelephonyResolution(
        dialDigits = DEFAULT_EMERGENCY_FALLBACK,
        regionSourceVerified = false,
        assistantNotice = UNVERIFIED_ASSISTANT_NOTICE
    )

    @RequiresApi(Build.VERSION_CODES.R)
    private fun emergencyNumbersFromPlatform(context: Context, tm: TelephonyManager): Set<String> {
        return try {
            val subId = SubscriptionManager.getDefaultSubscriptionId()
            val map = tm.getEmergencyNumberList(subId)
            map.values.flatten()
                .mapNotNull { en: EmergencyNumber ->
                    en.number.filter { it.isDigit() }.takeIf { it.isNotEmpty() }
                }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun pickFromPlatformList(codes: Set<String>, iso: String?): String? {
        if (codes.isEmpty()) return null
        val upper = iso?.uppercase(Locale.US)
        if (upper != null) {
            when {
                upper in GB_ISLES && "999" in codes -> return "999"
                upper == "US" && "911" in codes -> return "911"
                upper == "CA" && "911" in codes -> return "911"
                upper == "AU" && "000" in codes -> return "000"
                upper == "NZ" && "111" in codes -> return "111"
            }
        }
        val preferOrder =
            listOf("911", "999", "112", "000", "111", "110", "118", "119")
        for (p in preferOrder) {
            if (p in codes) return p
        }
        return codes.minByOrNull { it.length } ?: codes.firstOrNull()
    }

    private fun primaryCountryIso(tm: TelephonyManager): String? {
        tm.simCountryIso?.takeIf { it.isNotBlank() }?.let {
            return it.uppercase(Locale.US)
        }
        tm.networkCountryIso?.takeIf { it.isNotBlank() }?.let {
            return it.uppercase(Locale.US)
        }
        return null
    }

    /** National short code when we know the SIM/network country but not the platform list. */
    private fun shortCodeForCountry(iso: String): String {
        return when (iso.uppercase(Locale.US)) {
            "US", "CA" -> "911"
            in GB_ISLES -> "999"
            "AU" -> "000"
            "NZ" -> "111"
            "JP" -> "119"
            "IN" -> "112"
            else -> DEFAULT_EMERGENCY_FALLBACK
        }
    }
}
