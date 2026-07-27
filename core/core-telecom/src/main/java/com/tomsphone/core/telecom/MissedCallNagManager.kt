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
 * - **Only for contacts assigned a home call button** (home slots), not answer-only list contacts
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
    private val ringtonePlayer: RingtonePlayer,
    // Lazy to break the DI cycle (CallManagerImpl injects Lazy<MissedCallNagManager>).
    private val callManager: dagger.Lazy<CallManager>
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
    
    // Auto-expire: missed calls older than 7 days are automatically deleted
    private val MISSED_CALL_EXPIRY_DAYS = 7
    
    init {
        // Auto-expire old missed calls on startup
        scope.launch {
            val expiryTimestamp = System.currentTimeMillis() - (MISSED_CALL_EXPIRY_DAYS * 24 * 60 * 60 * 1000L)
            val result = callLogRepository.deleteMissedCallsOlderThan(expiryTimestamp)
            if (result.isSuccess) {
                Log.d(TAG, "Auto-expired missed calls older than $MISSED_CALL_EXPIRY_DAYS days")
            } else {
                Log.e(TAG, "Failed to auto-expire old missed calls: ${result.exceptionOrNull()}")
            }
        }
        
        // Authoritative call-in-progress tracking from the real call state. onCallStarted()/
        // onCallEnded() still give immediate dial-time suppression and the normal reset, but a
        // rung-out call-back that is interrupted before the app returns home can miss onCallEnded
        // and leave callInProgress stuck true — silently suppressing a helper's reassurance nag.
        // Observing currentCall guarantees the flag is released whenever a call that WAS ongoing
        // ends, regardless of whether anyone calls onCallEnded. It never undoes the dial-time
        // suppression: while no call has yet appeared on the flow it leaves callInProgress alone.
        scope.launch {
            var wasOngoing = false
            callManager.get().currentCall.collect { call ->
                val ongoing = call != null &&
                    call.state != CallState.DISCONNECTED &&
                    call.state != CallState.IDLE
                if (ongoing) {
                    wasOngoing = true
                    callInProgress = true
                } else if (wasOngoing) {
                    // A call that was ongoing has ended — release suppression even if onCallEnded was
                    // never delivered. Brief nag-suppression window to let the DB Flow settle, matching
                    // onCallEnded (and keeping a permanent on-connect dismissal permanent).
                    wasOngoing = false
                    callInProgress = false
                    nagSuppressedUntil = if (nagSuppressedUntil == Long.MAX_VALUE) {
                        System.currentTimeMillis() + 1000
                    } else {
                        System.currentTimeMillis() + 3000
                    }
                    Log.d(TAG, "currentCall ended - callInProgress released (authoritative)")
                }
                // ongoing == false && wasOngoing == false: no call has begun on the flow yet — leave
                // callInProgress alone so onCallStarted()'s dial-time suppression is not undone.
            }
        }

        // Monitor unread missed + declined; nag only for contacts with a home call slot
        scope.launch {
            combine(
                callLogRepository.getCallsForNagReminder(10).map { calls -> calls.filter { !it.isRead } },
                settingsRepository.getSettings(),
                contactRepository.getContacts(200)
            ) { missedCalls, settings, contacts -> Triple(missedCalls, settings, contacts) }
                .collect { (missedCalls, settings, contacts) ->
                    Log.d(TAG, "getCallsForNagReminder returned ${missedCalls.size} unread calls: ${missedCalls.map { "${it.id}:${it.contactName}" }}")
                    val onHomeIds = contactIdsWithHomeCallButton(settings)
                    val assistantMissedCalls = mutableListOf<CallLogEntry>()
                    for (call in missedCalls) {
                        val contact = when {
                            call.contactId != null -> contactRepository.getContactById(call.contactId!!).first()
                            else -> contactRepository.getContactByPhone(call.phoneNumber).first()
                        }
                        if (contact != null && contact.id in onHomeIds) {
                            assistantMissedCalls.add(call)
                        }
                    }

                    _activeMissedCalls.value = assistantMissedCalls

                    val now = System.currentTimeMillis()

                    if (callInProgress) {
                        Log.d(TAG, "Nag suppressed - call in progress")
                        return@collect
                    }

                    if (now < nagSuppressedUntil) {
                        Log.d(TAG, "Nag suppressed for ${nagSuppressedUntil - now}ms more")
                        return@collect
                    }

                    if (assistantMissedCalls.isNotEmpty() && settings.missedCallNagEnabled) {
                        startNagging(assistantMissedCalls.first())
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
     * Dismiss nag if now CONNECTED to the missed caller (call is ACTIVE)
     * Works for both incoming and outgoing calls - the nag is dismissed
     * when a conversation happens, regardless of who initiated or who ends it.
     * 
     * Returns true if nag was dismissed, false if talking to someone else
     */
    suspend fun dismissIfTalkingToMissedCaller(phoneNumber: String): Boolean {
        val talkingToNormalized = normalizePhoneNumber(phoneNumber)

        // Match ALL active (helper) misses from this caller, not just the most recent, and tolerant
        // of number format. A helper's miss is now cleared ONLY here — on a real connection (it is
        // deliberately NOT cleared at dial time) — so we must clear every outstanding row for them:
        //  - connecting to an OLDER outstanding helper must clear them too, not only whoever is newest;
        //  - a helper can have several unread rows stored in DIFFERENT formats (+4477… and 077…), and
        //    if any survives the nag resumes ~1s after hanging up with someone just spoken to.
        val matches = _activeMissedCalls.value.filter {
            normalizePhoneNumber(it.phoneNumber) == talkingToNormalized
        }

        if (matches.isEmpty()) {
            Log.d(TAG, "Call ACTIVE with $phoneNumber - not one of the active missed callers; any nag resumes after the call")
            return false
        }

        Log.d(TAG, "Now connected to missed caller (${matches.first().contactName}) - DISMISSING ${matches.size} row(s) permanently")

        // Suppress nag restart permanently for this call (until next missed call)
        nagSuppressedUntil = Long.MAX_VALUE

        // IMMEDIATELY clear state to prevent race conditions with Room Flow
        _activeMissedCalls.value = emptyList()

        stopAllAudio()
        stopNagging()

        // Mark every matching row read by id — exact and format-independent, so no sibling row in a
        // different stored format survives. Number-based clear as a backstop for any same-format rows
        // not currently tracked in _activeMissedCalls.
        matches.forEach { call ->
            val result = callLogRepository.markAsRead(call.id)
            if (result.isFailure) {
                Log.e(TAG, "FAILED to mark call ${call.id} as read: ${result.exceptionOrNull()}")
            }
        }
        callLogRepository.markMissedCallsFromNumberAsRead(phoneNumber)

        return true
    }
    
    /**
     * @deprecated Use dismissIfTalkingToMissedCaller when call becomes ACTIVE
     */
    suspend fun dismissIfCallingMissedCaller(phoneNumber: String): Boolean {
        // Just stop audio when placing a call - actual dismissal happens when call connects
        stopAllAudio()
        Log.d(TAG, "Call placed to $phoneNumber - audio stopped, nag will be dismissed if call connects")
        return false
    }
    
    /**
     * Mark ALL missed calls from a phone number as read and stop nagging
     * Called when user places a call to that number (they're calling back)
     */
    suspend fun markMissedCallsAsReadAndDismiss(phoneNumber: String) {
        // Stop all audio immediately (don't nag over the call the senior is placing).
        stopAllAudio()

        // Normalize the phone number for matching
        val normalizedPhone = normalizePhoneNumber(phoneNumber)

        // Helper reassurance: a home-slot helper's miss is NOT cleared just because the senior
        // tried calling back — only a genuine two-way connection clears it (that happens in
        // dismissIfTalkingToMissedCaller when the call becomes ACTIVE). So if the call-back rings
        // out, the helper stays outstanding and the nag resumes after the call — never leave a
        // helper unsure the senior is OK. A non-helper (other contact / unknown) DOES clear on the
        // attempt: they've done their part.
        //
        // "Is this a home-slot helper miss?" is answered from _activeMissedCalls — the SAME set the
        // nag itself is built from (unread home-slot misses, resolved by contactId in the init flow)
        // — rather than re-resolving the number via getContactByPhone. That keeps the dial-time
        // decision exactly consistent with the nag's own decision and avoids a resolution asymmetry
        // that could wrongly clear a real helper. If the number isn't a currently-tracked helper
        // miss, we clear (safe default).
        val isTrackedHelperMiss = _activeMissedCalls.value.any {
            normalizePhoneNumber(it.phoneNumber) == normalizedPhone
        }
        if (isTrackedHelperMiss) {
            Log.d(TAG, "Calling back tracked helper miss $phoneNumber - NOT marking read; clears only on connect")
            stopNagging() // quiet the nag for the attempt; it resumes if the call doesn't connect
            return
        }

        // Non-helper: mark all missed calls from this number as read (clears on the attempt).
        // Try both the original format and normalized format.
        callLogRepository.markMissedCallsFromNumberAsRead(phoneNumber)
        if (normalizedPhone != phoneNumber) {
            callLogRepository.markMissedCallsFromNumberAsRead(normalizedPhone)
        }

        Log.d(TAG, "Marked all missed calls from $phoneNumber as read")
        // No tracked helper miss matched, so there is no home-slot nag to stop beyond the audio above.
    }
    
    /**
     * Normalize phone number for comparison (E.164; default region GB for national format).
     */
    private fun normalizePhoneNumber(phone: String): String {
        return com.tomsphone.core.data.util.PhoneNumberUtils.normalizeToE164(phone, "GB")
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
     * Does NOT reset the suppression if the nag was permanently dismissed (talked to missed caller)
     */
    fun onCallEnded() {
        callInProgress = false
        
        // If suppression is permanent (Long.MAX_VALUE), don't reduce it
        // This means we talked to the missed caller and the nag was properly dismissed
        if (nagSuppressedUntil == Long.MAX_VALUE) {
            // Reset to a short window now that call is over, DB should be synced
            nagSuppressedUntil = System.currentTimeMillis() + 1000
            Log.d(TAG, "Call ended - nag was permanently dismissed, brief suppression to confirm DB sync")
        } else {
            // Extend suppression for 3 seconds after call ends to allow database sync
            nagSuppressedUntil = System.currentTimeMillis() + 3000
            Log.d(TAG, "Call ended - nag suppressed for 3s to allow DB sync")
        }
    }
    
    /**
     * Dismiss specific missed call
     */
    suspend fun dismiss(callId: Long) {
        callLogRepository.markAsRead(callId)
    }
    
    /**
     * Log a missed call and optionally trigger nag.
     *
     * One database row per event (no deduplication) so repeat callers show multiple entries.
     * Only contacts with a home call button trigger the nagging reminder.
     */
    fun onMissedCall(phoneNumber: String, contactName: String?) {
        scope.launch {
            val now = System.currentTimeMillis()
            // Look up contact (may be null for unknown callers)
            val contact = contactRepository.getContactByPhone(phoneNumber).first()
            
            // Log as missed call - ALL missed calls go in the log for the list
            val entry = CallLogEntry(
                id = 0,
                contactId = contact?.id,
                phoneNumber = phoneNumber,
                contactName = contactName ?: contact?.name,
                type = com.tomsphone.core.data.model.CallType.MISSED,
                timestamp = now,
                duration = 0,
                isRead = false
            )
            
            callLogRepository.logCall(entry)
            
            val settings = settingsRepository.getSettings().first()
            val contacts = contactRepository.getContacts(200).first()
            val onHome = contact != null && contact.id in contactIdsWithHomeCallButton(settings)
            if (onHome && contact != null) {
                Log.d(TAG, "Logged missed call from home-slot contact ${contact.name} - nag will start")
            } else {
                Log.d(TAG, "Logged missed call from ${contact?.name ?: phoneNumber} (no home button) - no nag")
            }

            // Flow above will pick up the missed call and start nagging for home-slot contacts only
        }
    }
    
    fun shutdown() {
        stopNagging()
        scope.cancel()
    }
}

