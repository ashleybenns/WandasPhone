package com.tomsphone.core.telecom

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log

private const val TAG = "BatteryAlertSms"

/**
 * Logs actual SMS queue/delivery results from [SmsManager]. Without sent/delivery [PendingIntent]s,
 * sendTextMessage returning does not mean the carrier accepted the message.
 */
class SmsSendStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val dest = intent.getStringExtra(EXTRA_DEST) ?: "?"
        val part = intent.getIntExtra(EXTRA_PART, 0)
        val totalParts = intent.getIntExtra(EXTRA_PARTS_TOTAL, 1)
        val segmentLabel =
            if (totalParts > 1) " seg ${part + 1}/$totalParts" else ""

        when (intent.action) {
            ACTION_SENT -> {
                val desc = describeSmsSentResult(resultCode)
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "SMS_SENT OK$segmentLabel → $dest ($desc)")
                } else {
                    Log.e(TAG, "SMS_SENT FAILED$segmentLabel → $dest: resultCode=$resultCode ($desc)")
                }
            }
            ACTION_DELIVERED -> {
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "SMS_DELIVERED$segmentLabel → $dest (handset confirmed)")
                } else {
                    Log.w(TAG, "SMS_DELIVERED$segmentLabel → $dest resultCode=$resultCode")
                }
            }
            else -> Log.w(TAG, "Unknown SMS intent action: ${intent.action}")
        }
    }

    companion object {
        const val ACTION_SENT = "com.tomsphone.core.telecom.SMS_SENT"
        const val ACTION_DELIVERED = "com.tomsphone.core.telecom.SMS_DELIVERED"
        const val EXTRA_DEST = "dest"
        const val EXTRA_PART = "part"
        const val EXTRA_PARTS_TOTAL = "partsTotal"
    }
}

private fun describeSmsSentResult(resultCode: Int): String =
    when (resultCode) {
        Activity.RESULT_OK -> "queued/accepted by radio"
        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "GENERIC_FAILURE (often balance/APN/blocked — check carrier)"
        SmsManager.RESULT_ERROR_RADIO_OFF -> "RADIO_OFF"
        SmsManager.RESULT_ERROR_NULL_PDU -> "NULL_PDU"
        SmsManager.RESULT_ERROR_NO_SERVICE -> "NO_SERVICE"
        else -> "unknown code $resultCode"
    }
