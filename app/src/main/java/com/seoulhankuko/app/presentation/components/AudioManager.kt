package com.seoulhankuko.app.presentation.components

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

@Composable
fun AudioButton(
    audioSrc: String?,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onPlayAudio: (String?) -> Unit = {}
) {
    val audioManager = rememberAudioManager()
    var isPlaying by remember { mutableStateOf(false) }
    
    // Check if this audio is currently playing
    LaunchedEffect(audioManager.getCurrentAudioUrl()) {
        isPlaying = audioManager.getCurrentAudioUrl() == audioSrc && audioManager.isPlaying()
    }
    
    // Cleanup when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            audioManager.cleanup()
        }
    }
    
    if (!audioSrc.isNullOrBlank()) {
        androidx.compose.material3.IconButton(
            onClick = {
                if (isPlaying) {
                    audioManager.stopAudio()
                    isPlaying = false
                } else {
                    audioManager.playAudio(audioSrc)
                    isPlaying = true
                }
            },
            modifier = modifier
        ) {
            androidx.compose.material3.Icon(
                imageVector = if (isPlaying) 
                    Icons.Default.Close 
                else 
                    Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Stop audio" else "Play audio",
                tint = androidx.compose.ui.graphics.Color(0xFF5EEAD4)
            )
        }
    }
}
