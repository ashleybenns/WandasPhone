package com.tomsphone.core.telecom

import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BatteryAlertSms"

/**
 * Sends optional SMS to carers for low battery and device connected after low battery.
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
     * Send low battery alert to each number (deduplicated). No-op if no permission or empty list.
     */
    suspend fun sendLowBatteryAlert(recipientNumbers: List<String>, userName: String) {
        if (!hasSmsPermission() || recipientNumbers.isEmpty()) return
        val numbers = recipientNumbers.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (numbers.isEmpty()) return
        val message = "WandasPhone: $userName's phone battery is low (${BatteryMonitor.LOW_BATTERY_THRESHOLD}% or below). Please check in."
        sendToAll(numbers, message)
    }

    /**
     * Send "device connected after low battery" alert. No-op if no permission or empty list.
     */
    suspend fun sendDeviceConnectedAlert(recipientNumbers: List<String>, userName: String) {
        if (!hasSmsPermission() || recipientNumbers.isEmpty()) return
        val numbers = recipientNumbers.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (numbers.isEmpty()) return
        val message = "WandasPhone: $userName's phone is now plugged in and charging (was low battery)."
        sendToAll(numbers, message)
    }

    private suspend fun sendToAll(numbers: List<String>, message: String) = withContext(Dispatchers.IO) {
        val smsManager = SmsManager.getDefault()
        for (number in numbers) {
            try {
                smsManager.sendTextMessage(number, null, message, null, null)
                Log.d(TAG, "SMS sent to $number")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send SMS to $number: ${e.message}")
            }
        }
    }
}
