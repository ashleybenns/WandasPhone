package com.wandasphone.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android TextToSpeech implementation
 */
@Singleton
class AndroidTTSImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WandasTTS {
    
    private companion object {
        const val TAG = "WandasTTS"
    }
    
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking
    
    private val utteranceQueue = Channel<UtteranceItem>(Channel.UNLIMITED)
    
    init {
        initializeTTS()
    }
    
    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.apply {
                    language = Locale.getDefault()
                    setSpeechRate(1.0f)
                    setPitch(1.0f)
                    
                    setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                            Log.d(TAG, "Started speaking: $utteranceId")
                        }
                        
                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                            Log.d(TAG, "Finished speaking: $utteranceId")
                        }
                        
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                            Log.e(TAG, "Error speaking: $utteranceId")
                        }
                    })
                }
                isInitialized = true
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }
    
    override fun speak(message: String, priority: WandasTTS.Priority) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized, cannot speak: $message")
            return
        }
        
        val queueMode = when (priority) {
            WandasTTS.Priority.IMMEDIATE -> TextToSpeech.QUEUE_FLUSH
            else -> TextToSpeech.QUEUE_ADD
        }
        
        textToSpeech?.speak(
            message,
            queueMode,
            null,
            "utterance_${System.currentTimeMillis()}"
        )
        
        Log.d(TAG, "Speaking ($priority): $message")
    }
    
    override fun speakNow(message: String) {
        speak(message, WandasTTS.Priority.IMMEDIATE)
    }
    
    override fun stop() {
        textToSpeech?.stop()
        _isSpeaking.value = false
        Log.d(TAG, "Stopped speaking")
    }
    
    override fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking ?: false
    }
    
    override fun setSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.5f, 2.0f)
        textToSpeech?.setSpeechRate(clampedSpeed)
        Log.d(TAG, "Set speed to $clampedSpeed")
    }
    
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        Log.d(TAG, "TTS shutdown")
    }
    
    private data class UtteranceItem(
        val message: String,
        val priority: WandasTTS.Priority
    )
}

