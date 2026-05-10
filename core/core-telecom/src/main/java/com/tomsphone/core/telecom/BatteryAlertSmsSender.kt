package com.tomsphone.core.telecom

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.tomsphone.core.data.util.PhoneNumberUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BatteryAlertSms"

private val pendingIntentRequestCode = AtomicInteger(10_000)

/**
 * Sends optional SMS to carers for low battery and device connected after low battery.
 * Uses the phone numbers already stored for assistants (contacts with "Notify for battery alerts").
 * Numbers are normalized to E.164 so stored numbers in any format are used for texting.
 * Checks SEND_SMS permission before sending.
 */
@Singleton
class BatteryAlertSmsSender @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * Normalize and deduplicate recipient numbers so we use already-input (stored) numbers for texting.
     * Converts to E.164 so any stored format (E.164, national, with spaces) works with SmsManager.
     */
    private fun normalizeRecipients(recipientNumbers: List<String>): List<String> {
        return recipientNumbers
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { raw ->
                val e164 = PhoneNumberUtils.normalizeToE164(raw, "GB")
                if (e164.isNotEmpty()) e164 else null
            }
            .distinct()
    }

    /**
     * Send low battery alert to each stored assistant number (deduplicated, E.164). No-op if no permission or no valid numbers.
     */
    suspend fun sendLowBatteryAlert(recipientNumbers: List<String>, userName: String) {
        if (!hasSmsPermission()) return
        val numbers = normalizeRecipients(recipientNumbers)
        if (numbers.isEmpty()) {
            Log.w(TAG, "Low battery alert skipped: no valid recipient numbers (stored assistant numbers will be used when present)")
            return
        }
        val message = "Tom's Phone: $userName's phone battery is low (${BatteryMonitor.LOW_BATTERY_THRESHOLD}% or below). Please check in."
        sendToAll(numbers, message)
    }

    /**
     * Send "device connected after low battery" alert to each stored assistant number. No-op if no permission or no valid numbers.
     */
    suspend fun sendDeviceConnectedAlert(recipientNumbers: List<String>, userName: String) {
        if (!hasSmsPermission()) return
        val numbers = normalizeRecipients(recipientNumbers)
        if (numbers.isEmpty()) {
            Log.w(TAG, "Device connected alert skipped: no valid recipient numbers (stored assistant numbers will be used when present)")
            return
        }
        val message = "Tom's Phone: $userName's phone is now plugged in and charging (was low battery)."
        sendToAll(numbers, message)
    }

    /**
     * Sent when the user presses SOS (before/during emergency confirm). One SMS per assistant number.
     * [messageTemplate] empty = default text; otherwise `{userName}` / `{name}` are replaced with [userName].
     */
    suspend fun sendSosPressedAlertToAssistants(
        recipientNumbers: List<String>,
        userName: String,
        messageTemplate: String,
    ) {
        if (!hasSmsPermission()) {
            Log.w(TAG, "SOS assistant SMS skipped: SEND_SMS permission not granted")
            return
        }
        val numbers = normalizeRecipients(recipientNumbers)
        if (numbers.isEmpty()) {
            Log.w(TAG, "SOS assistant SMS skipped: no valid assistant numbers")
            return
        }
        val template = messageTemplate.trim()
        val body =
            if (template.isEmpty()) {
                "Tom's Phone: $userName pressed SOS. Please check on them — they may not have completed an emergency call."
            } else {
                template
                    .replace("{userName}", userName)
                    .replace("{name}", userName)
            }
        sendToAll(numbers, body)
        Log.d(TAG, "SOS SMS queued for ${numbers.size} recipient(s) — watch SMS_SENT lines for carrier result")
    }

    suspend fun sendTestBatteryAlert(recipientNumbers: List<String>, userName: String): String {
        if (!hasSmsPermission()) {
            Log.w(TAG, "Test SMS skipped: SEND_SMS permission not granted")
            return "No SMS permission. Allow Toms phone to send SMS in app settings."
        }
        val numbers = normalizeRecipients(recipientNumbers)
        if (numbers.isEmpty()) {
            Log.w(TAG, "Test SMS skipped: no valid recipient numbers (add assistants with \"Notify for battery alerts\" and a valid number)")
            return "No recipients. Add an assistant with \"Notify for battery alerts\" and a valid phone number."
        }
        val message = "Tom's Phone: Test battery alert. If you got this, battery alerts are working. ($userName's phone)"
        return withContext(Dispatchers.IO) {
            try {
                sendToAll(numbers, message)
                Log.d(TAG, "Test SMS sent to ${numbers.size} recipient(s)")
                "Test text sent to ${numbers.size} assistant(s)."
            } catch (e: Exception) {
                Log.e(TAG, "Test SMS failed: ${e.message}", e)
                "Send failed: ${e.message}"
            }
        }
    }

    private suspend fun sendToAll(numbers: List<String>, message: String) = withContext(Dispatchers.IO) {
        val smsManager = SmsManager.getDefault()
        for (number in numbers) {
            try {
                queueSms(smsManager, number, message)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to queue SMS to $number: ${e.message}", e)
            }
        }
    }

    private fun queueSms(smsManager: SmsManager, destination: String, body: String) {
        val parts = smsManager.divideMessage(body)
        val sentIntents = ArrayList<PendingIntent?>(parts.size)
        val deliveryIntents = ArrayList<PendingIntent?>(parts.size)
        for (i in parts.indices) {
            sentIntents.add(
                smsPendingIntent(
                    destination = destination,
                    partIndex = i,
                    totalParts = parts.size,
                    isDelivery = false,
                ),
            )
            deliveryIntents.add(
                smsPendingIntent(
                    destination = destination,
                    partIndex = i,
                    totalParts = parts.size,
                    isDelivery = true,
                ),
            )
        }
        if (parts.size == 1) {
            smsManager.sendTextMessage(destination, null, body, sentIntents[0], deliveryIntents[0])
            Log.d(TAG, "SMS queued (1 part) → $destination — awaiting SMS_SENT callback")
        } else {
            smsManager.sendMultipartTextMessage(destination, null, parts, sentIntents, deliveryIntents)
            Log.d(TAG, "SMS queued (${parts.size} parts) → $destination — awaiting SMS_SENT callbacks")
        }
    }

    private fun smsPendingIntent(
        destination: String,
        partIndex: Int,
        totalParts: Int,
        isDelivery: Boolean,
    ): PendingIntent {
        val req = pendingIntentRequestCode.getAndIncrement()
        val intent =
            Intent(context, SmsSendStatusReceiver::class.java).apply {
                action =
                    if (isDelivery) {
                        SmsSendStatusReceiver.ACTION_DELIVERED
                    } else {
                        SmsSendStatusReceiver.ACTION_SENT
                    }
                putExtra(SmsSendStatusReceiver.EXTRA_DEST, destination)
                putExtra(SmsSendStatusReceiver.EXTRA_PART, partIndex)
                putExtra(SmsSendStatusReceiver.EXTRA_PARTS_TOTAL, totalParts)
            }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, req, intent, flags)
    }
}
