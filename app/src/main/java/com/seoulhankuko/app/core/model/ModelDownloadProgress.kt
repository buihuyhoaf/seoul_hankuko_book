package com.seoulhankuko.app.core.model

/**
 * Sealed class representing the download progress state of the ML model
 */
sealed class ModelDownloadProgress {
    object Idle : ModelDownloadProgress()
    object Checking : ModelDownloadProgress()
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercent: Int
    ) : ModelDownloadProgress() {
        init {
            require(progressPercent in 0..100) { "Progress must be between 0 and 100" }
        }
    }
    object Loading : ModelDownloadProgress()
    object Completed : ModelDownloadProgress()
    data class Error(val message: String) : ModelDownloadProgress()
    
    val isInProgress: Boolean
        get() = this is Checking || this is Downloading || this is Loading
    
    val isCompleted: Boolean
        get() = this is Completed
}

