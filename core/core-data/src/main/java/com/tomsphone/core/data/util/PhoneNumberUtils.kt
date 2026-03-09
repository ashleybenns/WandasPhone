package com.tomsphone.core.data.util

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber

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
}
