package com.tomsphone.feature.home

import android.util.Base64
import java.nio.charset.StandardCharsets

/**
 * Encodes phone + optional suggested name into a single navigation path segment
 * (Base64 URL-safe, no padding).
 */
object AddBlockedCallerRoute {
    private const val SEP = "\u001e"

    fun encode(phoneNumber: String, suggestedName: String?): String {
        val payload = "$phoneNumber$SEP${suggestedName.orEmpty()}"
        return Base64.encodeToString(
            payload.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    fun decode(token: String): Pair<String, String?> {
        val decoded = String(
            Base64.decode(token, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
            StandardCharsets.UTF_8
        )
        val parts = decoded.split(SEP, limit = 2)
        val phone = parts[0].trim()
        val name = parts.getOrNull(1)?.trim().orEmpty()
        return phone to name.ifEmpty { null }
    }
}
