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
    
    // Suppress nag restart briefly after user dismisses (prevents race with Room Flow)
    private var nagSuppressedUntil: Long = 0
    
    // Track if a call is in progress - completely suppress nag while calling
    private var callInProgress: Boolean = false
    
    init {
        // Monitor for new missed calls - only nag for CARER contacts
        scope.launch {
            callLogRepository.getMissedCalls(10)
                .map { calls -> calls.filter { !it.isRead } }
                .collect { missedCalls ->
                    Log.d(TAG, "getMissedCalls returned ${missedCalls.size} unread calls: ${missedCalls.map { "${it.id}:${it.contactName}" }}")
                    // Filter to only carer contacts for nagging
                    val carerMissedCalls = missedCalls.filter { call ->
                        val contact = call.contactId?.let { id ->
                            contactRepository.getContactById(id).first()
                        }
                        contact?.contactType == ContactType.CARER
                    }
                    
                    _activeMissedCalls.value = carerMissedCalls
                    
                    // Check if nagging is enabled and not suppressed
                    val settings = settingsRepository.getSettings().first()
                    val now = System.currentTimeMillis()
                    
                    // Don't restart nag while a call is in progress
                    if (callInProgress) {
                        Log.d(TAG, "Nag suppressed - call in progress")
                        return@collect
                    }
                    
                    if (now < nagSuppressedUntil) {
                        Log.d(TAG, "Nag suppressed for ${nagSuppressedUntil - now}ms more")
                        return@collect
                    }
                    
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
                // Play tannoy-style bing-bong attention sound (trimmed version)
                ringtonePlayer.playAndWait(RingtonePlayer.Ringtone.TANNOY_SHORT)
                
                delay(150)  // Brief pause after bing-bong
                
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
     * Dismiss nag only if calling the most recent missed caller
     * Returns true if nag was dismissed, false if calling someone else
     */
    suspend fun dismissIfCallingMissedCaller(phoneNumber: String): Boolean {
        val mostRecentMissedCall = _activeMissedCalls.value.firstOrNull()
        
        if (mostRecentMissedCall == null) {
            Log.d(TAG, "No active missed calls to dismiss")
            return false
        }
        
        // Normalize both numbers for comparison
        val callingNormalized = normalizePhoneNumber(phoneNumber)
        val missedNormalized = normalizePhoneNumber(mostRecentMissedCall.phoneNumber)
        
        Log.d(TAG, "Comparing: calling='$callingNormalized' vs missed='$missedNormalized'")
        
        if (callingNormalized == missedNormalized) {
            Log.d(TAG, "Calling the missed caller (${mostRecentMissedCall.contactName}) - dismissing nag, id=${mostRecentMissedCall.id}")
            
            // Suppress nag restart for 5 seconds to prevent race with Room Flow
            nagSuppressedUntil = System.currentTimeMillis() + 5000
            
            // IMMEDIATELY clear state to prevent race conditions with Room Flow
            _activeMissedCalls.value = emptyList()
            
            stopAllAudio()
            stopNagging()
            
            // Mark as read in database (Room Flow will confirm the state)
            val result = callLogRepository.markAsRead(mostRecentMissedCall.id)
            if (result.isSuccess) {
                Log.d(TAG, "Successfully marked call ${mostRecentMissedCall.id} as read")
            } else {
                Log.e(TAG, "FAILED to mark call ${mostRecentMissedCall.id} as read: ${result.exceptionOrNull()}")
            }
            
            return true
        } else {
            Log.d(TAG, "Calling ${phoneNumber}, but missed call is from ${mostRecentMissedCall.contactName} - nag continues")
            // Still stop audio briefly so user can make the call, but nag will resume
            stopAllAudio()
            return false
        }
    }
    
    /**
     * Normalize phone number for comparison (UK format)
     */
    private fun normalizePhoneNumber(phone: String): String {
        var digits = phone.replace(Regex("[^0-9]"), "")
        // Handle UK +44 prefix -> 0
        if (digits.startsWith("44") && digits.length > 10) {
            digits = "0" + digits.substring(2)
        }
        return digits
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
     * Notify that a call has started - suppresses all nagging until call ends
     */
    fun onCallStarted() {
        callInProgress = true
        stopAllAudio()
        stopNagging()
        Log.d(TAG, "Call started - nag fully suppressed")
    }
    
    /**
     * Notify that a call has ended - allows nag to resume if needed
     * Sets a brief suppression window to allow database to sync
     */
    fun onCallEnded() {
        callInProgress = false
        // Extend suppression for 3 seconds after call ends to allow database sync
        nagSuppressedUntil = System.currentTimeMillis() + 3000
        Log.d(TAG, "Call ended - nag suppressed for 3s to allow DB sync")
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

