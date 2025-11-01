package com.seoulhankuko.app.presentation.components

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.Locale

/**
 * TTS Manager for clean Text-to-Speech integration
 * Handles Korean language TTS with playback control
 */
class TTSManager(private val context: Context) {
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _playbackPosition = MutableStateFlow(0)
    val playbackPosition: StateFlow<Int> = _playbackPosition.asStateFlow()
    
    private var currentText: String? = null
    private var estimatedDuration = 0
    private var startTime = 0L
    private var currentSpeed = 1.0f
    
    init {
        initializeTTS()
    }
    
    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale("ko", "KR"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Timber.w("Korean TTS language not supported, trying default")
                    textToSpeech?.setLanguage(Locale.getDefault())
                } else {
                    Timber.d("TTS initialized successfully with Korean language")
                }
                
                // Set up utterance progress listener
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isPlaying.value = true
                        startTime = System.currentTimeMillis()
                        Timber.d("TTS started: $utteranceId")
                    }
                    
                    override fun onDone(utteranceId: String?) {
                        _isPlaying.value = false
                        _playbackPosition.value = estimatedDuration
                        startTime = 0L
                        Timber.d("TTS completed: $utteranceId")
                    }
                    
                    override fun onError(utteranceId: String?) {
                        _isPlaying.value = false
                        Timber.e("TTS error: $utteranceId")
                    }
                })
                
                isInitialized = true
            } else {
                Timber.e("TTS initialization failed")
            }
        }
    }
    
    /**
     * Play text using TTS
     */
    fun speak(text: String, speed: Float = 1.0f) {
        if (text.isBlank()) {
            Timber.w("Cannot speak empty text")
            return
        }
        
        if (!isInitialized) {
            Timber.w("TTS not initialized yet")
            return
        }
        
        // Stop any ongoing speech
        stop()
        
        currentText = text
        currentSpeed = speed
        textToSpeech?.let { tts ->
            tts.setSpeechRate(speed)
            
            // Split text by underscores to insert pauses
            val parts = text.split(Regex("_+"))
            
            // Calculate estimated duration
            val charsPerWord = 4f // Korean average
            val totalWords = text.length / charsPerWord
            val minutes = totalWords / 150f
            val pauseTimeMs = 1500 // 1.5 seconds pause
            val totalPauses = parts.size - 1
            estimatedDuration = ((minutes * 60f * 1000f / speed) + (totalPauses * pauseTimeMs)).toInt()
            
            Timber.d("Speaking text (${text.length} chars, ${totalWords.toInt()} words, ${totalPauses} pauses, est. ${estimatedDuration}ms)")
            
            val utteranceId = "tts_${System.currentTimeMillis()}"
            
            // Speak each part with pauses between them
            parts.forEachIndexed { index, part ->
                if (part.isNotBlank()) {
                    val params = android.os.Bundle().apply {
                        putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "${utteranceId}_part$index")
                    }
                    
                    // First part uses QUEUE_FLUSH, rest use QUEUE_ADD
                    val queueMode = if (index == 0) {
                        TextToSpeech.QUEUE_FLUSH
                    } else {
                        TextToSpeech.QUEUE_ADD
                    }
                    
                    val result = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        tts.speak(part, queueMode, params, "${utteranceId}_part$index")
                    } else {
                        @Suppress("DEPRECATION")
                        tts.speak(part, queueMode, null)
                    }
                    
                    if (result == TextToSpeech.ERROR) {
                        Timber.e("Failed to speak part $index")
                    }
                }
                
                // Add pause after each part except the last one
                if (index < parts.size - 1) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        tts.playSilentUtterance(pauseTimeMs.toLong(), TextToSpeech.QUEUE_ADD, null)
                        Timber.d("Added ${pauseTimeMs}ms pause after part $index")
                    }
                }
            }
        }
    }
    
    /**
     * Stop TTS playback
     */
    fun stop() {
        textToSpeech?.stop()
        _isPlaying.value = false
        _playbackPosition.value = 0
        currentText = null
        Timber.d("TTS stopped")
    }
    
    /**
     * Pause TTS (TTS doesn't support pause, so we stop)
     */
    fun pause() {
        stop()
    }
    
    /**
     * Resume TTS (replay from beginning)
     */
    fun resume() {
        currentText?.let { text ->
            speak(text)
        }
    }
    
    /**
     * Update playback speed
     */
    fun setSpeed(speed: Float) {
        currentSpeed = speed
        if (_isPlaying.value && currentText != null) {
            // Stop and restart with new speed
            val wasPlaying = _isPlaying.value
            stop()
            if (wasPlaying) {
                currentText?.let { speak(it, speed) }
            }
        } else {
            textToSpeech?.setSpeechRate(speed)
        }
    }
    
    /**
     * Update playback position (for UI sync)
     */
    fun updatePosition() {
        if (_isPlaying.value && startTime > 0) {
            val elapsed = (System.currentTimeMillis() - startTime).toInt()
            _playbackPosition.value = elapsed.coerceAtMost(estimatedDuration)
        }
    }
    
    /**
     * Check if TTS is available
     */
    fun isAvailable(): Boolean {
        return isInitialized && textToSpeech != null
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
        Timber.d("TTS cleaned up")
    }
}

/**
 * Remember TTSManager instance
 */
@Composable
fun rememberTTSManager(): TTSManager {
    val context = LocalContext.current
    return remember { TTSManager(context) }
}

