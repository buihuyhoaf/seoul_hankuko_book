package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.core.model.ModelDownloadProgress
import com.seoulhankuko.app.core.model.ModelDownloadProgressRepository
import com.seoulhankuko.app.data.repository.HangulRepository
import com.seoulhankuko.app.domain.model.HangulGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Hangul Alphabet Selection Screen
 * Exposes StateFlow<List<HangulGroup>> from repository and model download progress
 */
@HiltViewModel
class HangulSelectionViewModel @Inject constructor(
    private val hangulRepository: HangulRepository,
    private val modelDownloadProgressRepository: ModelDownloadProgressRepository
) : ViewModel() {

    /**
     * StateFlow exposing list of Hangul groups
     */
    val hangulGroups: StateFlow<List<HangulGroup>> = hangulRepository
        .getHangulGroups()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * StateFlow exposing model download progress
     */
    val downloadProgress: StateFlow<ModelDownloadProgress> = 
        modelDownloadProgressRepository.downloadProgress

    init {
        // Load data when ViewModel is created
        loadHangulGroups()
        // Start preloading model when ViewModel is created
        modelDownloadProgressRepository.preloadModelWithProgress()
    }

    /**
     * Load Hangul groups from repository
     */
    private fun loadHangulGroups() {
        viewModelScope.launch {
            // Data is automatically exposed via StateFlow
            // No additional action needed as repository provides Flow
        }
    }
}

