package com.tomsphone.core.telecom

import android.content.Context
import android.util.Log
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.data.model.CallLogEntry
import com.tomsphone.core.data.repository.CallLogRepository
import com.tomsphone.core.data.repository.ContactRepository
import com.tomsphone.core.tts.TTSScripts
import com.tomsphone.core.tts.WandasTTS
import dagger.hilt.android.qualifiers.ApplicationContext
import com.tomsphone.core.data.model.ContactType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages missed call reminders (nagging)
 * 
 * "Wanda, you missed a call. Please call [carer] now."
 * 
 * Features:
 * - Tannoy-style bing-bong attention sound before TTS
 * - Repeating TTS reminders at configurable intervals
 * - **Only for CARER contacts** (grey list friends/family do NOT trigger nag)
 * - Stops when: user calls back, carer calls again, or carer dismisses
 * - Enabled by default with "Immediate and every minute" interval
 */
@Singleton
class MissedCallNagManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callLogRepository: CallLogRepository,
    private val contactRepository: ContactRepository,
    private val settingsRepository: SettingsRepository,
    private val tts: WandasTTS,
    private val ringtonePlayer: RingtonePlayer
) {
    
    private companion object {
        const val TAG = "MissedCallNag"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var nagJob: Job? = null
    
    // Track which missed calls we're currently nagging about
    private val _activeMissedCalls = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val activeMissedCalls: StateFlow<List<CallLogEntry>> = _activeMissedCalls.asStateFlow()
    
    init {
        // Monitor for new missed calls - only nag for CARER contacts
        scope.launch {
            callLogRepository.getMissedCalls(10)
                .map { calls -> calls.filter { !it.isRead } }
                .collect { missedCalls ->
                    // Filter to only carer contacts for nagging
                    val carerMissedCalls = missedCalls.filter { call ->
                        val contact = call.contactId?.let { id ->
                            contactRepository.getContactById(id).first()
                        }
                        contact?.contactType == ContactType.CARER
                    }
                    
                    _activeMissedCalls.value = carerMissedCalls
                    
                    // Check if nagging is enabled
                    val settings = settingsRepository.getSettings().first()
                    
                    if (carerMissedCalls.isNotEmpty() && settings.missedCallNagEnabled) {
                        startNagging(carerMissedCalls.first())
                    } else {
                        stopNagging()
                    }
                }
        }
    }
    
    private fun startNagging(missedCall: CallLogEntry) {
        // Cancel existing nag job
        nagJob?.cancel()
        
        Log.d(TAG, "Starting missed call nag for ${missedCall.contactName}")
        
        nagJob = scope.launch {
            val settings = settingsRepository.getSettings().first()
            val nagInterval = settings.missedCallNagInterval
            
            // Initial delay before first nag
            delay(nagInterval.initialDelaySeconds * 1000L)
            
            while (isActive) {
                // Play tannoy-style bing-bong attention sound
                ringtonePlayer.playAndWait(RingtonePlayer.Ringtone.TANNOY_BINGBONG)
                
                delay(300)  // Brief pause after bing-bong
                
                // Speak reminder
                val message = TTSScripts.missedCallReminder(
                    callerName = missedCall.contactName ?: "someone",
                    userName = settings.userName
                )
                tts.speak(message, WandasTTS.Priority.HIGH)
                
                Log.d(TAG, "Played missed call reminder")
                
                // Wait for repeat interval
                delay(nagInterval.repeatIntervalSeconds * 1000L)
            }
        }
    }
    
    private fun stopNagging() {
        nagJob?.cancel()
        nagJob = null
        Log.d(TAG, "Stopped missed call nagging")
    }
    
    /**
     * Dismiss all missed call reminders and stop all audio immediately
     */
    suspend fun dismissAll() {
        // Stop all audio immediately
        stopAllAudio()
        callLogRepository.markAllMissedAsRead()
        stopNagging()
    }
    
    /**
     * Stop all audio (ringtone + TTS) immediately
     * Call this when user takes action like placing a call
     */
    fun stopAllAudio() {
        ringtonePlayer.stop()
        tts.stop()
        Log.d(TAG, "Stopped all nag audio")
    }
    
    /**
     * Dismiss specific missed call
     */
    suspend fun dismiss(callId: Long) {
        callLogRepository.markAsRead(callId)
    }
    
    /**
     * Manually trigger a missed call nag (for rejected calls)
     * Only triggers if the contact is a CARER
     */
    fun onMissedCall(phoneNumber: String, contactName: String?) {
        scope.launch {
            // Check if this is a carer contact
            val contact = contactRepository.getContactByPhone(phoneNumber).first()
            
            if (contact?.contactType != ContactType.CARER) {
                Log.d(TAG, "Missed call from non-carer - no nag")
                return@launch
            }
            
            // Log as missed call
            val entry = CallLogEntry(
                id = 0,
                contactId = contact.id,
                phoneNumber = phoneNumber,
                contactName = contactName ?: contact.name,
                type = com.tomsphone.core.data.model.CallType.MISSED,
                timestamp = System.currentTimeMillis(),
                duration = 0,
                isRead = false
            )
            
            callLogRepository.logCall(entry)
            Log.d(TAG, "Logged missed call from carer ${contact.name} - nag will start")
            
            // The existing flow will pick up the missed call and start nagging
        }
    }
    
    fun shutdown() {
        stopNagging()
        scope.cancel()
    }
}

