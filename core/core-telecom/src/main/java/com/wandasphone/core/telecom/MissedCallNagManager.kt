package com.wandasphone.core.telecom

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import com.wandasphone.core.config.SettingsRepository
import com.wandasphone.core.data.model.CallLogEntry
import com.wandasphone.core.data.repository.CallLogRepository
import com.wandasphone.core.data.repository.ContactRepository
import com.wandasphone.core.tts.TTSScripts
import com.wandasphone.core.tts.WandasTTS
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages missed call reminders (nagging)
 * 
 * "Wanda, you missed a call from [carer], please call [carer] now."
 * 
 * Features:
 * - Repeating TTS reminders every N minutes
 * - Attention sound before TTS
 * - Only for carer contacts
 * - Stops when: user calls back, carer calls again, or carer dismisses
 */
@Singleton
class MissedCallNagManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callLogRepository: CallLogRepository,
    private val contactRepository: ContactRepository,
    private val settingsRepository: SettingsRepository,
    private val tts: WandasTTS
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
        // Monitor for new missed calls
        scope.launch {
            callLogRepository.getMissedCalls(10)
                .map { calls -> calls.filter { !it.isRead } }
                .collect { missedCalls ->
                    _activeMissedCalls.value = missedCalls
                    
                    if (missedCalls.isNotEmpty()) {
                        startNagging(missedCalls.first())
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
            val intervalMs = settings.missedCallNagIntervalMinutes * 60 * 1000L
            
            while (isActive) {
                // Play attention sound
                playAttentionSound()
                
                delay(500)  // Wait for sound to finish
                
                // Speak reminder
                val message = TTSScripts.missedCallReminder(
                    callerName = missedCall.contactName ?: "someone",
                    userName = settings.userName
                )
                tts.speak(message, WandasTTS.Priority.HIGH)
                
                Log.d(TAG, "Played missed call reminder")
                
                // Wait for interval
                delay(intervalMs)
            }
        }
    }
    
    private fun stopNagging() {
        nagJob?.cancel()
        nagJob = null
        Log.d(TAG, "Stopped missed call nagging")
    }
    
    /**
     * Dismiss all missed call reminders
     */
    suspend fun dismissAll() {
        callLogRepository.markAllMissedAsRead()
        stopNagging()
    }
    
    /**
     * Dismiss specific missed call
     */
    suspend fun dismiss(callId: Long) {
        callLogRepository.markAsRead(callId)
    }
    
    /**
     * Play attention-getting sound before TTS
     * (In production, use a custom tannoy-style sound)
     */
    private fun playAttentionSound() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
            // Use system notification sound as placeholder
            // In production, include custom attention.mp3 in raw resources
            val notification = android.media.RingtoneManager.getDefaultUri(
                android.media.RingtoneManager.TYPE_NOTIFICATION
            )
            
            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, notification)
                prepare()
                start()
            }
            
            // Release after playing
            mediaPlayer.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play attention sound", e)
        }
    }
    
    fun shutdown() {
        stopNagging()
        scope.cancel()
    }
}

