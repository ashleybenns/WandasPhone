package com.tomsphone.core.telecom

import android.media.AudioManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import com.tomsphone.core.config.HomeSlotAssignments
import com.tomsphone.core.config.SettingsRepository
import com.tomsphone.core.data.model.CallLogEntry
import com.tomsphone.core.data.model.CallType
import com.tomsphone.core.data.repository.CallLogRepository
import com.tomsphone.core.data.repository.ContactRepository
import com.tomsphone.core.tts.TTSScripts
import com.tomsphone.core.tts.WandasTTS
import com.tomsphone.core.analytics.AnalyticsManager
import com.tomsphone.core.analytics.AnalyticsEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * InCallService for handling active phone calls
 * 
 * This is the heart of the phone functionality.
 * It receives all call state changes from Android system.
 * 
 * Speakerphone behavior:
 * - Level 1 (MINIMAL): Always on speaker, no toggle
 * - Level 2+ (BASIC+): Toggle available, default from settings
 */
@AndroidEntryPoint
class WandasInCallService : InCallService(), CallManagerImpl.InCallServiceBridge {
    
    private companion object {
        const val TAG = "WandasInCallService"
    }
    
    @Inject
    lateinit var callManager: CallManagerImpl
    
    @Inject
    lateinit var tts: WandasTTS
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var contactRepository: ContactRepository
    
    @Inject
    lateinit var missedCallNagManager: dagger.Lazy<MissedCallNagManager>

    @Inject
    lateinit var callLogRepository: dagger.Lazy<CallLogRepository>
    
    @Inject
    lateinit var ringtonePlayer: RingtonePlayer
    
    @Inject
    lateinit var analytics: AnalyticsManager
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var currentCall: Call? = null
    private var isSpeakerOn = false
    private var isMuted = false
    private var wasCallActive = false  // Track if call was ever connected
    /** Wall clock when we first entered ACTIVE; used for log duration */
    private var connectedAtMillis: Long = 0L
    private var wasIncomingCall = false  // Track if this was an incoming call
    private var wasAutoAnswered = false  // Track if call was auto-answered
    private var lastIncomingPhoneNumber: String? = null  // For missed call nag
    private var lastIncomingContactName: String? = null
    private var isRegisteredWithCallManager = false
    private var currentContactName: String? = null  // Preserve contact name across state updates
    private var autoAnswerJob: Job? = null  // Job for delayed auto-answer
    
    /**
     * Speak TTS if announcements are enabled.
     * Checks setting synchronously - may block briefly on first call.
     */
    private fun speakIfEnabled(message: String) {
        serviceScope.launch {
            val settings = settingsRepository.getSettings().first()
            if (settings.ttsAnnouncementsEnabled) {
                tts.speak(message)
            }
        }
    }
    
    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            handleCallStateChange(call, state)
        }
        
        override fun onDetailsChanged(call: Call, details: Call.Details) {
            super.onDetailsChanged(call, details)
            Log.d(TAG, "Call details changed")
        }
    }
    
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        
        // Register with CallManager for audio controls
        if (!isRegisteredWithCallManager) {
            callManager.registerInCallService(this)
            isRegisteredWithCallManager = true
            Log.d(TAG, "Registered with CallManager")
        }
        
        val isIncoming = call.details.callDirection == Call.Details.DIRECTION_INCOMING
        val phoneNumber = call.details.handle?.schemeSpecificPart ?: "Unknown"
        
        Log.d(TAG, "========================================")
        Log.d(TAG, "onCallAdded: $phoneNumber, incoming=$isIncoming, state=${call.state}")
        
        // PROTECTION: Reject if there's already an active call
        // User cannot juggle multiple calls - this is by design for accessibility
        currentCall?.let { existingCall ->
            val existingState = existingCall.state
            if (existingState == Call.STATE_ACTIVE || 
                existingState == Call.STATE_DIALING || 
                existingState == Call.STATE_RINGING ||
                existingState == Call.STATE_CONNECTING) {
                
                Log.d(TAG, ">>> REJECTING second call - already have call in state $existingState")
                
                // If the rejected call is from a known contact, save as missed call
                // This allows it to appear in missed calls list (Level 2+)
                if (isIncoming) {
                    serviceScope.launch {
                        try {
                            val contactName = findContactByPhone(phoneNumber)
                            if (contactName != null) {
                                Log.d(TAG, "Saving rejected second call as missed: $contactName")
                                missedCallNagManager.get().onMissedCall(phoneNumber, contactName)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving missed call: ${e.message}")
                        }
                    }
                }
                
                call.reject(false, null)
                Log.d(TAG, "========================================")
                return // Don't process further
            }
        }
        
        // BACKUP: If CallScreeningService didn't reject unknown caller, check here
        if (isIncoming) {
            // Synchronously check if caller is allowed
            val isAllowed = runBlocking {
                try {
                    val settings = settingsRepository.getSettings().first()
                    if (!settings.rejectUnknownCalls) {
                        Log.d(TAG, "rejectUnknownCalls is disabled, allowing")
                        true
                    } else {
                        val contact = findContactByPhone(phoneNumber)
                        Log.d(TAG, "Contact lookup: ${contact ?: "NOT FOUND"}")
                        contact != null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking caller: ${e.message}")
                    true // Allow on error
                }
            }
            
            if (!isAllowed) {
                Log.d(TAG, ">>> REJECTING unknown caller in InCallService")
                call.reject(false, null)
                Log.d(TAG, "========================================")
                return // Don't process further
            }
            Log.d(TAG, ">>> Caller is allowed")
        }
        
        Log.d(TAG, "========================================")
        
        currentCall?.unregisterCallback(callCallback)
        currentCall = call
        call.registerCallback(callCallback)
        wasCallActive = false  // Reset for new call
        connectedAtMillis = 0L
        wasAutoAnswered = false  // Reset for new call
        wasIncomingCall = isIncoming
        lastIncomingPhoneNumber = if (isIncoming) phoneNumber else null
        lastIncomingContactName = null  // Will be set during contact lookup
        currentContactName = null  // Reset for new call - will be set when contact is looked up
        cancelAutoAnswer()  // Cancel any pending auto-answer from previous call
        
        // Enable speakerphone when call is added
        serviceScope.launch {
            enableSpeakerBasedOnSettings()
            
            // Look up contact name for missed call tracking
            if (isIncoming) {
                lastIncomingContactName = findContactByPhone(phoneNumber)
            }
        }
        
        handleCallStateChange(call, call.state)
    }
    
    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed, wasActive: $wasCallActive, wasIncoming: $wasIncomingCall")
        
        val phoneForLog = call.details.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() }
        val wasIn = wasIncomingCall
        val wasAct = wasCallActive
        val nameHintForLog = currentContactName ?: lastIncomingContactName
        val connectedAtForLog = connectedAtMillis
        
        call.unregisterCallback(callCallback)
        if (currentCall == call) {
            currentCall = null
        }
        
        callManager.updateCallState(null)
        
        // Handle call end based on type
        if (wasCallActive) {
            // Call was connected - announce "call ended"
            speakIfEnabled(TTSScripts.callEnded())
            // Restore call volume to carer default (in case user adjusted during call)
            serviceScope.launch {
                try {
                    val settings = settingsRepository.getSettings().first()
                    setCallVolume(settings.speakerVolume)
                    Log.d(TAG, "Restored call volume to ${settings.speakerVolume}%")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore volume: ${e.message}")
                }
            }
        } else if (wasIncomingCall && lastIncomingPhoneNumber != null) {
            if (callManager.consumeIncomingRingingDeclinedByUser()) {
                Log.d(TAG, "Incoming declined by user — REJECTED already logged, skip MISSED")
            } else {
                // Incoming call was NOT answered → log missed + nag for carers
                Log.d(TAG, "Unanswered incoming from ${lastIncomingContactName ?: lastIncomingPhoneNumber}")
                missedCallNagManager.get().onMissedCall(
                    lastIncomingPhoneNumber!!,
                    lastIncomingContactName
                )
            }
        }
        
        // Recent calls log: answered incoming + all user-placed outgoing (answered or not)
        if (phoneForLog != null && phoneForLog != "Unknown") {
            val shouldLog = wasIn && wasAct || !wasIn
            if (shouldLog) {
                serviceScope.launch(Dispatchers.IO) {
                    try {
                        logCompletedCallToRecentCalls(
                            call = call,
                            phoneNumber = phoneForLog,
                            wasIncoming = wasIn,
                            wasActive = wasAct,
                            nameHint = nameHintForLog,
                            connectedAtMs = connectedAtForLog
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Recent calls log failed: ${e.message}", e)
                    }
                }
            }
        }
        
        // Reset state
        wasCallActive = false
        connectedAtMillis = 0L
        wasAutoAnswered = false
        wasIncomingCall = false
        lastIncomingPhoneNumber = null
        lastIncomingContactName = null
        currentContactName = null
        cancelAutoAnswer()
    }
    
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        audioState?.let {
            isSpeakerOn = it.route == CallAudioState.ROUTE_SPEAKER
            Log.d(TAG, "Audio state changed - Speaker: $isSpeakerOn")
            updateCallInfo()
        }
    }
    
    /**
     * Default speaker route at call start from carer settings.
     * (Feature tier is unified; [CarerSettings.featureLevel] is reserved for future use.)
     */
    private suspend fun enableSpeakerBasedOnSettings() {
        val settings = settingsRepository.getSettings().first()

        val shouldEnableSpeaker = if (settings.showSpeakerButton) {
            Log.d(TAG, "Speaker button on: speakerDefaultOn=${settings.speakerDefaultOn}")
            settings.speakerDefaultOn
        } else {
            Log.d(TAG, "No speaker button: speakerphoneAlwaysOn=${settings.speakerphoneAlwaysOn}")
            settings.speakerphoneAlwaysOn
        }
        
        // Always explicitly set the speaker state (on OR off)
        // Use silent version - don't announce default state at call start
        setSpeakerSilent(shouldEnableSpeaker)
        Log.d(TAG, "Speaker set to default: $shouldEnableSpeaker")
        
        // Set volume based on settings
        setCallVolume(settings.speakerVolume)
    }
    
    /**
     * Set speaker state without TTS announcement
     * Used for initial call setup where we don't want to announce the default
     */
    private fun setSpeakerSilent(enabled: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val route = if (enabled) {
                    CallAudioState.ROUTE_SPEAKER
                } else {
                    CallAudioState.ROUTE_EARPIECE
                }
                setAudioRoute(route)
                isSpeakerOn = enabled
                Log.d(TAG, "Speakerphone set silently to: $enabled")
                updateCallInfo()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting speakerphone: ${e.message}")
        }
    }
    
    /**
     * Set call volume (0-100 percent)
     */
    private fun setCallVolume(volumePercent: Int) {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            val targetVolume = (maxVolume * (volumePercent / 100f)).toInt().coerceIn(0, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, targetVolume, 0)
            Log.d(TAG, "Volume set to $volumePercent% ($targetVolume/$maxVolume)")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume: ${e.message}")
        }
    }
    
    /**
     * Answer the current call
     */
    fun answerCall() {
        currentCall?.let { call ->
            if (call.state == Call.STATE_RINGING) {
                call.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
                Log.d(TAG, "Answered call")
                // Track manual call answer
                analytics.logEvent(AnalyticsEvent.CallAnswered(wasAutoAnswer = false))
            }
        }
    }
    
    /**
     * End the current call
     */
    fun endCall() {
        currentCall?.let { call ->
            call.disconnect()
            Log.d(TAG, "Ended call")
        }
    }
    
    /**
     * Check if auto-answer should be triggered for this incoming call
     * and start the delayed auto-answer if eligible.
     */
    private fun checkAndStartAutoAnswer(phoneNumber: String) {
        // Cancel any existing auto-answer job
        autoAnswerJob?.cancel()
        autoAnswerJob = null
        
        serviceScope.launch {
            try {
                // Check global auto-answer setting
                val isAutoAnswerAllowed = settingsRepository.isAutoAnswerAllowed().first()
                if (!isAutoAnswerAllowed) {
                    Log.d(TAG, "Auto-answer: globally disabled")
                    return@launch
                }
                
                // Check if this contact has auto-answer enabled
                val contact = contactRepository.getContactByPhone(phoneNumber).first()
                    ?: contactRepository.getContactByPhone(normalizePhoneNumber(phoneNumber)).first()
                
                if (contact == null) {
                    Log.d(TAG, "Auto-answer: contact not found for $phoneNumber")
                    return@launch
                }
                
                if (!contact.autoAnswerEnabled) {
                    Log.d(TAG, "Auto-answer: disabled for contact ${contact.name}")
                    return@launch
                }

                val settings = settingsRepository.getSettings().first()
                val onHome = contact.id in HomeSlotAssignments.contactIdsOnHome(settings.homeSlotAssignments)
                if (!onHome) {
                    Log.d(TAG, "Auto-answer: contact not on a home button slot ${contact.name}")
                    return@launch
                }

                val delaySeconds = settings.autoAnswerDelaySeconds
                Log.d(TAG, "Auto-answer: starting ${delaySeconds}s delay for ${contact.name}")
                
                // Start delayed auto-answer
                autoAnswerJob = serviceScope.launch answerDelay@{
                    // Wait for the configured delay
                    delay(delaySeconds * 1000L)
                    
                    // Check if call is still ringing
                    val call = currentCall
                    if (call == null || call.state != Call.STATE_RINGING) {
                        Log.d(TAG, "Auto-answer: call no longer ringing, cancelling")
                        return@answerDelay
                    }
                    
                    Log.d(TAG, "Auto-answer: triggering for ${contact.name}")
                    
                    // PRIVACY: Play alert sound and announce BEFORE answering
                    // This is critical - user MUST know the call is being answered
                    ringtonePlayer.playAndWait(RingtonePlayer.Ringtone.TANNOY_SHORT)
                    tts.speakAndWait(TTSScripts.autoAnswerNotification(contact.name))
                    
                    // Final check - still ringing?
                    if (currentCall?.state != Call.STATE_RINGING) {
                        Log.d(TAG, "Auto-answer: call ended during notification, cancelling")
                        return@answerDelay
                    }
                    
                    // Answer the call
                    wasAutoAnswered = true
                    currentCall?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
                    Log.d(TAG, "Auto-answer: call answered")
                    
                    // Track auto-answer
                    analytics.logEvent(AnalyticsEvent.AutoAnswerTriggered)
                    analytics.logEvent(AnalyticsEvent.CallAnswered(wasAutoAnswer = true))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Auto-answer error: ${e.message}")
            }
        }
    }
    
    /**
     * Cancel any pending auto-answer
     */
    private fun cancelAutoAnswer() {
        autoAnswerJob?.cancel()
        autoAnswerJob = null
    }
    
    private fun handleCallStateChange(call: Call, state: Int) {
        val wandasState = when (state) {
            Call.STATE_DIALING -> CallState.DIALING
            Call.STATE_RINGING -> CallState.RINGING
            Call.STATE_CONNECTING -> CallState.CONNECTING
            Call.STATE_ACTIVE -> CallState.ACTIVE
            Call.STATE_HOLDING -> CallState.HOLDING
            Call.STATE_DISCONNECTING -> CallState.DISCONNECTING
            Call.STATE_DISCONNECTED -> CallState.DISCONNECTED
            else -> CallState.IDLE
        }
        
        val phoneNumber = call.details.handle?.schemeSpecificPart ?: "Unknown"
        val direction = if (call.details.callDirection == Call.Details.DIRECTION_INCOMING) {
            CallDirection.INCOMING
        } else {
            CallDirection.OUTGOING
        }
        
        // Handle auto-answer for incoming RINGING calls
        if (wandasState == CallState.RINGING && direction == CallDirection.INCOMING) {
            checkAndStartAutoAnswer(phoneNumber)
        } else if (wandasState != CallState.RINGING) {
            // Cancel auto-answer if call is no longer ringing
            cancelAutoAnswer()
        }
        
        // Emit state IMMEDIATELY so UI can react (especially for incoming calls)
        // Contact name will be updated in a follow-up emission
        val immediateCallInfo = CallInfo(
            callId = call.details.handle.toString(),
            phoneNumber = phoneNumber,
            contactName = null,  // Will be updated shortly
            contactId = null,
            state = wandasState,
            direction = direction,
            startTime = System.currentTimeMillis(),
            isSpeakerOn = isSpeakerOn,
            isMuted = isMuted,
            wasAutoAnswered = wasAutoAnswered
        )
        callManager.updateCallState(immediateCallInfo)
        Log.d(TAG, "Call state (immediate): $wandasState, direction: $direction, autoAnswered: $wasAutoAnswered")
        
        // Then look up contact name and update again
        serviceScope.launch {
            val contactName = try {
                findContactByPhone(phoneNumber)
            } catch (e: Exception) {
                Log.e(TAG, "Error looking up contact: ${e.message}")
                null
            }
            
            if (contactName != null) {
                // Store contact name for use in updateCallInfo() when speaker/mute changes
                currentContactName = contactName
                val updatedCallInfo = immediateCallInfo.copy(contactName = contactName)
                callManager.updateCallState(updatedCallInfo)
                Log.d(TAG, "Call state (with contact): $wandasState, contact: $contactName")
            }
        }
        
        // When call becomes active, ensure speaker is set correctly
        // No "answered" announcement needed - carer is talking or voicemail is playing
        if (wandasState == CallState.ACTIVE) {
            if (!wasCallActive) {
                connectedAtMillis = System.currentTimeMillis()
            }
            wasCallActive = true
            serviceScope.launch {
                enableSpeakerBasedOnSettings()
            }
        }
    }
    
    /**
     * Persist completed calls for Recent calls (user + carer).
     * Incoming unanswered → [MissedCallNagManager.onMissedCall] / reject paths; not here.
     */
    private suspend fun logCompletedCallToRecentCalls(
        call: Call,
        phoneNumber: String,
        wasIncoming: Boolean,
        wasActive: Boolean,
        nameHint: String?,
        connectedAtMs: Long
    ) {
        val type: CallType = when {
            wasIncoming && wasActive -> CallType.INCOMING
            wasIncoming && !wasActive -> return
            !wasIncoming && wasActive -> CallType.OUTGOING
            else -> CallType.OUTGOING_UNANSWERED
        }
        val endWallClock = System.currentTimeMillis()
        val durationMs = if (wasActive && connectedAtMs > 0L) {
            (endWallClock - connectedAtMs).coerceAtLeast(0L)
        } else {
            0L
        }
        val timestamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val created = call.details.creationTimeMillis
            if (created > 0L) created else endWallClock
        } else {
            endWallClock
        }
        var resolvedContact = contactRepository.getContactByPhone(phoneNumber).first()
        if (resolvedContact == null) {
            resolvedContact = contactRepository.getContactByPhone(normalizePhoneNumber(phoneNumber)).first()
        }
        val entry = CallLogEntry(
            id = 0L,
            contactId = resolvedContact?.id,
            phoneNumber = phoneNumber,
            contactName = resolvedContact?.name ?: nameHint,
            type = type,
            timestamp = timestamp,
            duration = durationMs,
            isRead = true
        )
        callLogRepository.get().logCall(entry)
    }
    
    /**
     * Find contact by phone number (with flexible matching)
     */
    private suspend fun findContactByPhone(phoneNumber: String): String? {
        // Try exact match first
        var contact = contactRepository.getContactByPhone(phoneNumber).first()
        if (contact != null) return contact.name
        
        // Try normalized match
        val normalizedNumber = normalizePhoneNumber(phoneNumber)
        if (normalizedNumber != phoneNumber) {
            contact = contactRepository.getContactByPhone(normalizedNumber).first()
            if (contact != null) return contact.name
        }
        
        return null
    }
    
    /**
     * Normalize phone number for matching (E.164; default region GB for national format).
     */
    private fun normalizePhoneNumber(phone: String): String {
        return com.tomsphone.core.data.util.PhoneNumberUtils.normalizeToE164(phone, "GB")
    }
    
    private fun updateCallInfo() {
        currentCall?.let { call ->
            val phoneNumber = call.details.handle?.schemeSpecificPart ?: "Unknown"
            val state = when (call.state) {
                Call.STATE_ACTIVE -> CallState.ACTIVE
                Call.STATE_RINGING -> CallState.RINGING
                Call.STATE_DIALING -> CallState.DIALING
                else -> CallState.IDLE
            }
            
            // FIX: Use actual direction from call details, not hardcoded OUTGOING
            val direction = if (call.details.callDirection == Call.Details.DIRECTION_INCOMING) {
                CallDirection.INCOMING
            } else {
                CallDirection.OUTGOING
            }
            
            val callInfo = CallInfo(
                callId = call.details.handle.toString(),
                phoneNumber = phoneNumber,
                contactName = currentContactName,  // Preserve contact name across speaker/mute changes
                contactId = null,
                state = state,
                direction = direction,
                startTime = System.currentTimeMillis(),
                isSpeakerOn = isSpeakerOn,
                isMuted = isMuted,
                wasAutoAnswered = wasAutoAnswered
            )
            callManager.updateCallState(callInfo)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Unregister from CallManager
        if (isRegisteredWithCallManager) {
            callManager.unregisterInCallService()
            isRegisteredWithCallManager = false
            Log.d(TAG, "Unregistered from CallManager")
        }
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }
    
    // ========== InCallServiceBridge Implementation ==========
    
    override fun toggleSpeaker() {
        setSpeaker(!isSpeakerOn)
    }
    
    override fun setSpeaker(enabled: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val route = if (enabled) {
                    CallAudioState.ROUTE_SPEAKER
                } else {
                    CallAudioState.ROUTE_EARPIECE
                }
                setAudioRoute(route)
                isSpeakerOn = enabled
                Log.d(TAG, "Speakerphone set to: $enabled")
                updateCallInfo()
                
                // Announce change (only if user toggled it at Level 2+ and TTS enabled)
                serviceScope.launch {
                    val settings = settingsRepository.getSettings().first()
                    if (settings.ttsAnnouncementsEnabled) {
                        if (enabled) {
                            tts.speak(TTSScripts.speakerOn())
                        } else {
                            tts.speak(TTSScripts.speakerOff())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting speakerphone: ${e.message}")
        }
    }
    
    override fun toggleMute() {
        setMute(!isMuted)
    }
    
    override fun setMute(muted: Boolean) {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.isMicrophoneMute = muted
            isMuted = muted
            Log.d(TAG, "Mute set to: $muted")
            updateCallInfo()
            
            // Announce mute state if TTS enabled
            speakIfEnabled(if (muted) TTSScripts.muted() else TTSScripts.unmuted())
        } catch (e: Exception) {
            Log.e(TAG, "Error setting mute: ${e.message}")
        }
    }
}
