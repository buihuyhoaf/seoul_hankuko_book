package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.repository.HangulRepository
import com.seoulhankuko.app.domain.model.HangulGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Hangul Alphabet Selection Screen
 * Exposes StateFlow<List<HangulGroup>> from repository
 */
@HiltViewModel
class HangulSelectionViewModel @Inject constructor(
    private val hangulRepository: HangulRepository
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

    init {
        // Load data when ViewModel is created
        loadHangulGroups()
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

