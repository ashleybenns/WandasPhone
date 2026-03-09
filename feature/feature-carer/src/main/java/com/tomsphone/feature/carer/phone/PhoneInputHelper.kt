package com.tomsphone.feature.carer.phone

import android.content.Context
import android.telephony.TelephonyManager
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.tomsphone.core.data.util.PhoneNumberUtils
import java.util.Locale

/**
 * Country entry for phone number country selector.
 */
data class PhoneCountry(
    val regionCode: String,
    val countryCode: Int,
    val displayName: String,
    val callingCodeDisplay: String
) {
    val displayLabel: String get() = "$callingCodeDisplay $displayName"
}

/**
 * Default region from device: SIM country ISO if available, else locale.
 */
fun getDefaultPhoneRegion(context: Context): String {
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    val simCountry = tm?.simCountryIso?.takeIf { it?.length == 2 }?.uppercase(Locale.US)
    if (!simCountry.isNullOrBlank()) return simCountry
    return Locale.getDefault().country.takeIf { it.length == 2 }?.uppercase(Locale.US) ?: "GB"
}

/**
 * List of countries for the phone country selector (region code, calling code, display name).
 * Sorted by display name; current/default region can be moved to top in the UI.
 */
fun getPhoneCountries(): List<PhoneCountry> {
    val util = PhoneNumberUtil.getInstance()
    return util.supportedRegions
        .mapNotNull { regionCode ->
            val countryCode = util.getCountryCodeForRegion(regionCode)
            if (countryCode == 0) return@mapNotNull null
            val displayName = Locale("", regionCode).getDisplayCountry(Locale.getDefault())
            PhoneCountry(
                regionCode = regionCode,
                countryCode = countryCode,
                displayName = displayName,
                callingCodeDisplay = "+$countryCode"
            )
        }
        .sortedBy { it.displayName.lowercase(Locale.getDefault()) }
}

/**
 * Validate national number for the given region and return E.164 for storage, or null if invalid.
 */
fun validateAndToE164(nationalNumber: String, regionCode: String): String? {
    return PhoneNumberUtils.formatToE164(nationalNumber, regionCode)
}
