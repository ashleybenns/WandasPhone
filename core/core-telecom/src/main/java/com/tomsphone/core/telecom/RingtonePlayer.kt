package com.tomsphone.core.telecom

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Plays ringtones and audio alerts from raw resources.
 * 
 * Designed for incoming call announcements where the audio file
 * contains both the ringtone sound and TTS announcement baked in.
 */
@Singleton
class RingtonePlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private companion object {
        const val TAG = "RingtonePlayer"
    }
    
    private var mediaPlayer: MediaPlayer? = null
    
    /**
     * Available ringtones and audio alerts
     */
    enum class Ringtone(@RawRes val resId: Int) {
        /** Main incoming call ringtone with baked-in TTS (legacy) */
        OLD_TWOBELL(R.raw.old_twobell_ringtone),
        
        /** Short two-bell sound only (no TTS) - used with dynamic TTS */
        SHORT_TWOBELL(R.raw.short_twobell_ringtone),
        
        /** Tannoy-style bing-bong for missed call nag attention (original) */
        TANNOY_BINGBONG(R.raw.tannoy_bingbong),
        
        /** Tannoy-style bing-bong - trimmed version for nag */
        TANNOY_SHORT(R.raw.tannoy_short)
    }
    
    /**
     * Play a ringtone asynchronously. Returns immediately.
     * Use [playAndWait] if you need to wait for completion.
     */
    fun play(ringtone: Ringtone) {
        play(ringtone.resId)
    }
    
    /**
     * Play a raw resource asynchronously.
     */
    fun play(@RawRes resId: Int) {
        stop() // Stop any currently playing ringtone
        
        try {
            mediaPlayer = MediaPlayer.create(context, resId)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setOnCompletionListener { 
                    Log.d(TAG, "Ringtone playback complete")
                    release()
                    mediaPlayer = null
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    release()
                    mediaPlayer = null
                    true
                }
                start()
                Log.d(TAG, "Started ringtone playback")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play ringtone", e)
        }
    }
    
    /**
     * Play a ringtone and suspend until playback completes.
     * Useful for chaining: play ringtone, then speak caller name.
     */
    suspend fun playAndWait(ringtone: Ringtone): Boolean {
        return playAndWait(ringtone.resId)
    }
    
    /**
     * Play a raw resource and suspend until playback completes.
     */
    suspend fun playAndWait(@RawRes resId: Int): Boolean = suspendCancellableCoroutine { cont ->
        stop()
        
        try {
            mediaPlayer = MediaPlayer.create(context, resId)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setOnCompletionListener {
                    Log.d(TAG, "Ringtone playback complete")
                    release()
                    mediaPlayer = null
                    if (cont.isActive) cont.resume(true)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    release()
                    mediaPlayer = null
                    if (cont.isActive) cont.resume(false)
                    true
                }
                start()
                Log.d(TAG, "Started ringtone playback (awaiting completion)")
            }
            
            if (mediaPlayer == null) {
                Log.e(TAG, "Failed to create MediaPlayer")
                cont.resume(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play ringtone", e)
            cont.resume(false)
        }
        
        cont.invokeOnCancellation {
            stop()
        }
    }
    
    /**
     * Stop any currently playing ringtone.
     */
    fun stop() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                Log.d(TAG, "Stopped ringtone")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping ringtone", e)
            }
        }
        mediaPlayer = null
    }
    
    /**
     * Check if a ringtone is currently playing.
     */
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
}
