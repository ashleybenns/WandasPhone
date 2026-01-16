package com.tomsphone.core.telecom

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.data.model.CallLogEntry
import com.tomsphone.core.data.model.CallType
import com.tomsphone.core.data.repository.CallLogRepository
import com.tomsphone.core.data.repository.ContactRepository
import com.tomsphone.core.tts.TTSScripts
import com.tomsphone.core.tts.WandasTTS
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import com.tomsphone.core.telecom.RingtonePlayer
import javax.inject.Inject

/**
 * CallScreeningService for auto-answer and call filtering
 * 
 * This service screens incoming calls before they reach the user:
 * - Auto-answer known contacts after N rings
 * - Silently reject unknown callers (optional)
 * - Log all calls
 * - Announce incoming calls via TTS
 */
@RequiresApi(Build.VERSION_CODES.N)
@AndroidEntryPoint
class WandasCallScreeningService : CallScreeningService() {
    
    private companion object {
        const val TAG = "CallScreeningService"
    }
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var contactRepository: ContactRepository
    
    @Inject
    lateinit var callLogRepository: CallLogRepository
    
    @Inject
    lateinit var tts: WandasTTS
    
    @Inject
    lateinit var callManager: CallManagerImpl
    
    @Inject
    lateinit var ringtonePlayer: RingtonePlayer
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onScreenCall(callDetails: Call.Details) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "onScreenCall START")
        
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: "Unknown"
        Log.d(TAG, "Phone number: $phoneNumber")
        
        // SYNCHRONOUS screening - must respond before returning
        val response = try {
            runBlocking {
                withTimeout(3000) { // 3 second timeout
                    screenCall(phoneNumber)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Screening error: ${e.message}", e)
            // On any error, allow call through
            CallScreeningService.CallResponse.Builder()
                .setRejectCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        }
        
        // Respond BEFORE returning
        respondToCall(callDetails, response)
        Log.d(TAG, "onScreenCall END - responded")
        Log.d(TAG, "========================================")
    }
    
    /**
     * Perform the actual screening logic
     */
    private suspend fun screenCall(phoneNumber: String): CallScreeningService.CallResponse {
        val settings = settingsRepository.getSettings().first()
        Log.d(TAG, "Settings: rejectUnknown=${settings.rejectUnknownCalls}")
        
        // Find contact
        val normalizedNumber = normalizePhoneNumber(phoneNumber)
        var contact = contactRepository.getContactByPhone(phoneNumber).first()
        if (contact == null && normalizedNumber != phoneNumber) {
            contact = contactRepository.getContactByPhone(normalizedNumber).first()
        }
        Log.d(TAG, "Contact: ${contact?.name ?: "NOT FOUND"}")
        
        val isKnownContact = contact != null
        val shouldReject = !isKnownContact && settings.rejectUnknownCalls
        
        Log.d(TAG, "Decision: known=$isKnownContact, reject=$shouldReject")
        
        return if (shouldReject) {
            Log.d(TAG, ">>> REJECTING unknown call")
            CallScreeningService.CallResponse.Builder()
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
                .build()
        } else {
            Log.d(TAG, ">>> ALLOWING call from ${contact?.name ?: phoneNumber}")
            CallScreeningService.CallResponse.Builder()
                .setRejectCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        }
    }
    
    private suspend fun logMissedCall(phoneNumber: String, contactName: String?) {
        val contact = contactRepository.getContactByPhone(phoneNumber).first()
        
        val entry = CallLogEntry(
            id = 0,
            contactId = contact?.id,
            phoneNumber = phoneNumber,
            contactName = contactName ?: contact?.name,
            type = CallType.REJECTED,
            timestamp = System.currentTimeMillis(),
            duration = 0,
            isRead = false
        )
        
        callLogRepository.logCall(entry)
    }
    
    /**
     * Normalize phone number for matching
     * Handles UK numbers: +44 prefix, leading 0, etc.
     */
    private fun normalizePhoneNumber(phone: String): String {
        // Remove all non-digit characters
        var digits = phone.replace(Regex("[^0-9]"), "")
        
        // Handle UK +44 prefix -> 0
        if (digits.startsWith("44") && digits.length > 10) {
            digits = "0" + digits.substring(2)
        }
        
        return digits
    }
}

