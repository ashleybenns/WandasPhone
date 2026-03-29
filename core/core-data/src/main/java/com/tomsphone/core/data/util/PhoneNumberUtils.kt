package com.tomsphone.core.data.util

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.ShortNumberInfo

/**
 * Utility functions for phone number matching and normalization.
 *
 * Uses libphonenumber for international support:
 * - Normalizes to E.164 for storage and matching
 * - Supports any country; default region used when number is entered in national format
 */
object PhoneNumberUtils {

    private val phoneUtil: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }

    /**
     * Normalize a phone number to E.164 (e.g. +447911123456).
     * If the number is in national format, [defaultRegion] (e.g. "GB") is used.
     * Returns empty string if parsing fails.
     */
    fun normalizeToE164(phoneNumber: String?, defaultRegion: String = "GB"): String {
        if (phoneNumber.isNullOrBlank()) return ""
        val trimmed = phoneNumber.trim()
        if (trimmed.isEmpty()) return ""
        return try {
            val parsed = phoneUtil.parse(trimmed, defaultRegion)
            if (phoneUtil.isValidNumber(parsed)) {
                phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
            } else {
                fallbackDigits(trimmed)
            }
        } catch (_: NumberParseException) {
            fallbackDigits(trimmed)
        }
    }

    /**
     * Legacy normalize: returns E.164 when possible, otherwise digits-only for backward compatibility.
     * Prefer [normalizeToE164] when you have a default region.
     */
    fun normalize(phoneNumber: String?): String {
        if (phoneNumber == null) return ""
        val e164 = normalizeToE164(phoneNumber, "GB")
        if (e164.isNotEmpty()) return e164
        return phoneNumber.replace(Regex("[^0-9]"), "")
    }

    private fun fallbackDigits(phoneNumber: String): String {
        val digits = phoneNumber.replace(Regex("[^0-9]"), "")
        if (digits.isEmpty()) return ""
        // If it looks like UK with +44, convert to 0-prefix for legacy
        if (digits.startsWith("44") && digits.length > 10) {
            return "0" + digits.substring(2)
        }
        return digits
    }

    /**
     * Check if two phone numbers match (after normalizing to E.164 with [defaultRegion]).
     */
    fun isMatch(number1: String?, number2: String?, defaultRegion: String = "GB"): Boolean {
        val n1 = normalizeToE164(number1, defaultRegion)
        val n2 = normalizeToE164(number2, defaultRegion)
        if (n1.isEmpty() || n2.isEmpty()) return false
        if (n1 == n2) return true
        // Fallback: last 10 digits for legacy compatibility
        val suffix1 = n1.replace(Regex("[^0-9]"), "").takeLast(10)
        val suffix2 = n2.replace(Regex("[^0-9]"), "").takeLast(10)
        return suffix1 == suffix2
    }

    /**
     * Get the last N digits of a phone number for matching (legacy helper).
     */
    fun getMatchSuffix(phoneNumber: String?, length: Int = 10): String {
        return normalize(phoneNumber).replace(Regex("[^0-9]"), "").takeLast(length)
    }

    /**
     * Validate a number for a given region. Returns true if valid.
     */
    fun isValid(phoneNumber: String?, regionCode: String): Boolean {
        if (phoneNumber.isNullOrBlank()) return false
        return try {
            val parsed = phoneUtil.parse(phoneNumber.trim(), regionCode)
            phoneUtil.isValidNumber(parsed)
        } catch (_: NumberParseException) {
            false
        }
    }

    /**
     * Format a number to E.164 for storage. Returns null if invalid.
     */
    fun formatToE164(nationalNumber: String, regionCode: String): String? {
        if (nationalNumber.isBlank()) return null
        return try {
            val parsed = phoneUtil.parse(nationalNumber.trim(), regionCode)
            if (phoneUtil.isValidNumber(parsed)) {
                phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
            } else null
        } catch (_: NumberParseException) {
            null
        }
    }

    /**
     * Parse an E.164 or national number and return region code and national significant number (digits only, no leading 0).
     * Used to populate country selector + national number field when editing a contact.
     */
    fun parseToRegionAndNational(e164OrNational: String?, defaultRegion: String = "GB"): Pair<String, String>? {
        if (e164OrNational.isNullOrBlank()) return null
        return try {
            val parsed = phoneUtil.parse(e164OrNational.trim(), defaultRegion)
            if (!phoneUtil.isValidNumber(parsed)) return null
            val region = phoneUtil.getRegionCodeForNumber(parsed)
            val national = phoneUtil.getNationalSignificantNumber(parsed)
            region to national
        } catch (_: NumberParseException) {
            null
        }
    }

    private val shortNumberInfo: ShortNumberInfo by lazy { ShortNumberInfo.getInstance() }

    /**
     * Dialer UX: input is a **complete, dialable** national/international subscriber number, or a known
     * **emergency / short** code for [regionCode] (e.g. UK 999, 112).
     *
     * When true: enable Call; [dialerInputToE164OrNull] should return non-null.
     *
     * Strings containing `*` or `#` are never "complete valid" here (USSD/MMI); user clears digits to edit.
     */
    fun isDialerInputCompleteAndValid(rawInput: String?, regionCode: String): Boolean {
        if (rawInput.isNullOrBlank()) return false
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.any { it == '*' || it == '#' }) return false

        return try {
            val parsed = phoneUtil.parse(trimmed, regionCode)
            phoneUtil.isValidNumber(parsed)
        } catch (_: NumberParseException) {
            val digitsOnly = trimmed.filter { it.isDigit() }
            digitsOnly.isNotEmpty() && shortNumberInfo.isEmergencyNumber(digitsOnly, regionCode)
        }
    }

    /**
     * E.164 for placing a call from the dialer when [isDialerInputCompleteAndValid] is true.
     * Returns null if the number cannot be normalized (should not happen when paired with the check above).
     */
    fun dialerInputToE164OrNull(rawInput: String?, regionCode: String): String? {
        if (rawInput.isNullOrBlank()) return null
        val trimmed = rawInput.trim()
        if (trimmed.any { it == '*' || it == '#' }) return null

        return try {
            val parsed = phoneUtil.parse(trimmed, regionCode)
            if (!phoneUtil.isValidNumber(parsed)) return null
            phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (_: NumberParseException) {
            val digitsOnly = trimmed.filter { it.isDigit() }
            if (digitsOnly.isEmpty() || !shortNumberInfo.isEmergencyNumber(digitsOnly, regionCode)) return null
            try {
                val parsed = phoneUtil.parse(digitsOnly, regionCode)
                if (phoneUtil.isValidNumber(parsed)) {
                    phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
                } else {
                    // tel: URI — use national short code (e.g. 999), not a fake +999 country code.
                    digitsOnly
                }
            } catch (_: NumberParseException) {
                digitsOnly
            }
        }
    }
}
