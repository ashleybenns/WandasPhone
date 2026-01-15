package com.tomsphone.core.data.util

/**
 * Utility functions for phone number matching
 * 
 * Handles UK phone number formats:
 * - +44 prefix (international)
 * - 0 prefix (national)
 * - Various spacing/punctuation
 */
object PhoneNumberUtils {
    
    /**
     * Normalize a phone number by removing non-digits and handling UK +44 prefix
     */
    fun normalize(phoneNumber: String?): String {
        if (phoneNumber == null) return ""
        
        // Remove all non-digit characters
        var digits = phoneNumber.replace(Regex("[^0-9]"), "")
        
        // Handle UK +44 prefix -> 0
        if (digits.startsWith("44") && digits.length > 10) {
            digits = "0" + digits.substring(2)
        }
        
        return digits
    }
    
    /**
     * Check if two phone numbers match
     * Compares last 10 digits for flexibility with international prefixes
     */
    fun isMatch(number1: String?, number2: String?): Boolean {
        val clean1 = normalize(number1)
        val clean2 = normalize(number2)
        
        if (clean1.isEmpty() || clean2.isEmpty()) {
            return false
        }
        
        // Compare last 10 digits for flexibility
        val suffix1 = clean1.takeLast(10)
        val suffix2 = clean2.takeLast(10)
        
        return suffix1.endsWith(suffix2) || suffix2.endsWith(suffix1)
    }
    
    /**
     * Get the last N digits of a phone number for matching
     */
    fun getMatchSuffix(phoneNumber: String?, length: Int = 10): String {
        return normalize(phoneNumber).takeLast(length)
    }
}
