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
        Log.d(TAG, "Screening call from ${callDetails.handle}")
        
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: "Unknown"
        
        serviceScope.launch {
            try {
                val settings = settingsRepository.getSettings().first()
                
                // Try to find contact - also try normalized versions of the number
                val normalizedNumber = normalizePhoneNumber(phoneNumber)
                var contact = contactRepository.getContactByPhone(phoneNumber).first()
                if (contact == null && normalizedNumber != phoneNumber) {
                    contact = contactRepository.getContactByPhone(normalizedNumber).first()
                }
                
                Log.d(TAG, "Incoming: $phoneNumber, normalized: $normalizedNumber, contact: ${contact?.name}")
                
                val isKnownContact = contact != null
                val shouldAutoAnswer = settings.autoAnswerEnabled && isKnownContact
                // TODO: Re-enable rejection once phone number matching is fixed
                // For now, don't reject so we can test incoming calls
                val shouldReject = false // !isKnownContact && settings.rejectUnknownCalls
                
                // Build response
                val response = CallScreeningService.CallResponse.Builder()
                
                when {
                    shouldReject -> {
                        // Silently reject unknown calls
                        response
                            .setRejectCall(true)
                            .setSkipCallLog(false)
                            .setSkipNotification(true)
                        
                        Log.d(TAG, "Rejecting unknown call from $phoneNumber")
                        
                        // Log missed call
                        logMissedCall(phoneNumber, null)
                    }
                    shouldAutoAnswer -> {
                        // Accept call (will auto-answer after delay)
                        response
                            .setRejectCall(false)
                            .setSkipCallLog(false)
                            .setSkipNotification(false)
                        
                        Log.d(TAG, "Accepting known call from ${contact?.name}")
                        
                        // Announce caller - skip ringtone for now to avoid crashes
                        try {
                            TTSScripts.callerAnnouncement(contact?.name)?.let { announcement ->
                                tts.speak(announcement)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error announcing caller: ${e.message}")
                        }
                        
                        // Schedule auto-answer
                        val delayMs = settings.autoAnswerDelaySeconds * 1000L
                        launch {
                            delay(delayMs)
                            callManager.answerCall()
                            Log.d(TAG, "Auto-answered call from ${contact?.name}")
                        }
                    }
                    else -> {
                        // Let it ring normally
                        response
                            .setRejectCall(false)
                            .setSkipCallLog(false)
                            .setSkipNotification(false)
                        
                        Log.d(TAG, "Allowing call to ring from $phoneNumber")
                        
                        // Announce caller - skip ringtone for now to avoid crashes
                        try {
                            TTSScripts.callerAnnouncement(contact?.name)?.let { announcement ->
                                tts.speak(announcement)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error announcing caller: ${e.message}")
                        }
                    }
                }
                
                respondToCall(callDetails, response.build())
            } catch (e: Exception) {
                Log.e(TAG, "Error screening call: ${e.message}")
                // In case of error, allow the call through
                val response = CallScreeningService.CallResponse.Builder()
                    .setRejectCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()
                respondToCall(callDetails, response)
            }
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

