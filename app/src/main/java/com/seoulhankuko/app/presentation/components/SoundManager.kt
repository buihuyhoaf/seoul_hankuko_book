package com.seoulhankuko.app.presentation.components

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import timber.log.Timber

/**
 * SoundManager for playing sound effects from res/raw resources
 */
class SoundManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    
    /**
     * Play a sound from res/raw resources
     * @param rawResourceId Resource ID from R.raw (e.g., R.raw.correct)
     */
    fun playSound(rawResourceId: Int) {
        try {
            // Stop and release current sound if playing
            stopSound()
            
            Timber.d("Playing sound resource: $rawResourceId")
            
            mediaPlayer = MediaPlayer.create(context, rawResourceId).apply {
                setOnCompletionListener { mp ->
                    Timber.d("Sound playback completed")
                    mp.release()
                    mediaPlayer = null
                }
                setOnErrorListener { mp, what, extra ->
                    Timber.e("Sound playback error: what=$what, extra=$extra")
                    mp.release()
                    mediaPlayer = null
                    true
                }
                start()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to play sound resource: $rawResourceId")
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
    
    /**
     * Play correct answer sound
     */
    fun playCorrect() {
        val correctResId = context.resources.getIdentifier(
            "correct",
            "raw",
            context.packageName
        )
        if (correctResId != 0) {
            playSound(correctResId)
        } else {
            Timber.w("correct.wav not found in res/raw")
        }
    }
    
    /**
     * Play incorrect answer sound
     */
    fun playIncorrect() {
        val incorrectResId = context.resources.getIdentifier(
            "incorrect",
            "raw",
            context.packageName
        )
        if (incorrectResId != 0) {
            playSound(incorrectResId)
        } else {
            Timber.w("incorrect.wav not found in res/raw")
        }
    }
    
    /**
     * Stop currently playing sound
     */
    fun stopSound() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Timber.e(e, "Error stopping sound")
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopSound()
    }
}

/**
 * Remember SoundManager instance
 */
@Composable
fun rememberSoundManager(): SoundManager {
    val context = LocalContext.current
    return remember { SoundManager(context) }
}

/**
 * Composable wrapper that automatically cleans up SoundManager
 */
@Composable
fun SoundManagerEffect(
    onSoundManagerReady: (SoundManager) -> Unit
) {
    val soundManager = rememberSoundManager()
    
    DisposableEffect(Unit) {
        onSoundManagerReady(soundManager)
        onDispose {
            soundManager.cleanup()
        }
    }
}

