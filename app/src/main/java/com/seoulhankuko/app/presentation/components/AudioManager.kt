package com.seoulhankuko.app.presentation.components

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import timber.log.Timber
import java.io.IOException

@Composable
fun rememberAudioManager(): AudioManager {
    val context = LocalContext.current
    return remember { AudioManager(context) }
}

class AudioManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentAudioUrl: String? = null
    
    fun playAudio(audioUrl: String?) {
        if (audioUrl.isNullOrBlank()) {
            Timber.w("Audio URL is null or blank")
            return
        }
        
        try {
            // Stop current audio if playing
            stopAudio()
            
            Timber.d("Playing audio: $audioUrl")
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioUrl)
                setOnPreparedListener { mp ->
                    Timber.d("Audio prepared, starting playback")
                    mp.start()
                }
                setOnCompletionListener { mp ->
                    Timber.d("Audio playback completed")
                    mp.release()
                    mediaPlayer = null
                }
                setOnErrorListener { mp, what, extra ->
                    Timber.e("Audio playback error: what=$what, extra=$extra")
                    mp.release()
                    mediaPlayer = null
                    true
                }
                prepareAsync()
            }
            
            currentAudioUrl = audioUrl
        } catch (e: IOException) {
            Timber.e(e, "Failed to play audio: $audioUrl")
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error while playing audio: $audioUrl")
        }
    }
    
    fun stopAudio() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            }
            mediaPlayer = null
            currentAudioUrl = null
        } catch (e: Exception) {
            Timber.e(e, "Error stopping audio")
        }
    }
    
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }
    
    fun getCurrentAudioUrl(): String? = currentAudioUrl
    
    fun cleanup() {
        stopAudio()
    }
}
