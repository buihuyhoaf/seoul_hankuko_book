package com.seoulhankuko.app.core.model

import android.content.Context
import com.seoulhankuko.app.ml.HangulTFLiteClassifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloadProgressRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _downloadProgress = MutableStateFlow<ModelDownloadProgress>(ModelDownloadProgress.Idle)
    val downloadProgress: StateFlow<ModelDownloadProgress> = _downloadProgress.asStateFlow()
    
    /**
     * Preload model with progress tracking
     */
    fun preloadModelWithProgress() {
        if (_downloadProgress.value is ModelDownloadProgress.Downloading ||
            _downloadProgress.value is ModelDownloadProgress.Loading ||
            _downloadProgress.value is ModelDownloadProgress.Completed) {
            Timber.d("Model preload skipped - already in progress or completed")
            return
        }
        
        serviceScope.launch {
            try {
                _downloadProgress.value = ModelDownloadProgress.Checking
                
                // Check if model exists locally
                val modelFile = File(context.filesDir, "hangul_stroke_model.tflite")
                if (modelFile.exists() && modelFile.length() > 0) {
                    Timber.d("Model already exists, loading into memory...")
                    _downloadProgress.value = ModelDownloadProgress.Loading
                    HangulTFLiteClassifier.createWithSupabase(context)
                    _downloadProgress.value = ModelDownloadProgress.Completed
                    return@launch
                }
                
                // Download with progress tracking
                _downloadProgress.value = ModelDownloadProgress.Downloading(0, 0, 0)
                
                HangulTFLiteClassifier.createWithSupabase(
                    context = context,
                    progressCallback = { bytesRead, totalBytes ->
                        val progress = if (totalBytes > 0) {
                            ((bytesRead * 100) / totalBytes).toInt()
                        } else {
                            0
                        }
                        _downloadProgress.value = ModelDownloadProgress.Downloading(
                            bytesDownloaded = bytesRead,
                            totalBytes = totalBytes,
                            progressPercent = progress
                        )
                    }
                )
                
                _downloadProgress.value = ModelDownloadProgress.Completed
                Timber.d("✅ Model preloaded successfully")
                
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to preload model")
                _downloadProgress.value = ModelDownloadProgress.Error(
                    e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    /**
     * Reset progress state
     */
    fun resetProgress() {
        _downloadProgress.value = ModelDownloadProgress.Idle
    }
}

