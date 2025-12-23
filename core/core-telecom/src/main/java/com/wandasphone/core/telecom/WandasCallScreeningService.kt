package com.wandasphone.core.telecom

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import com.wandasphone.core.config.SettingsRepository
import com.wandasphone.core.data.model.CallLogEntry
import com.wandasphone.core.data.model.CallType
import com.wandasphone.core.data.repository.CallLogRepository
import com.wandasphone.core.data.repository.ContactRepository
import com.wandasphone.core.tts.TTSScripts
import com.wandasphone.core.tts.WandasTTS
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onScreenCall(callDetails: Call.Details) {
        Log.d(TAG, "Screening call from ${callDetails.handle}")
        
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: "Unknown"
        
        serviceScope.launch {
            val settings = settingsRepository.getSettings().first()
            val contact = contactRepository.getContactByPhone(phoneNumber).first()
            
            val isKnownContact = contact != null
            val shouldAutoAnswer = settings.autoAnswerEnabled && isKnownContact
            val shouldReject = !isKnownContact && !settings.answerUnknownCalls
            
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
                    
                    // Announce incoming call
                    tts.speak(TTSScripts.incomingCall(contact?.name, settings.userName))
                    
                    // Schedule auto-answer
                    val delayMs = settings.autoAnswerRings * 1000L  // Rough estimate
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
                    
                    // Announce incoming call
                    tts.speak(TTSScripts.incomingCall(contact?.name, settings.userName))
                }
            }
            
            respondToCall(callDetails, response.build())
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
}

