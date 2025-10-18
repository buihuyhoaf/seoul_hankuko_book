package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.repository.EntryTestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EntryTestFlowViewModel @Inject constructor(
    private val entryTestRepository: EntryTestRepository
) : ViewModel() {

    val hasCompletedEntryTestFlow = flow {
        emit(entryTestRepository.hasCompletedEntryTest())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    suspend fun hasCompletedEntryTest(): Boolean {
        return entryTestRepository.hasCompletedEntryTest()
    }

    suspend fun getCurrentCourseId(): Int? {
        return entryTestRepository.getCurrentCourseId()
    }

    suspend fun saveEntryTestResult(
        hasCompletedEntryTest: Boolean,
        currentCourseId: Int?,
        currentCourseName: String?,
        entryTestScore: Int?
    ) {
        entryTestRepository.saveEntryTestResult(
            hasCompletedEntryTest = hasCompletedEntryTest,
            currentCourseId = currentCourseId,
            currentCourseName = currentCourseName,
            entryTestScore = entryTestScore
        )
    }
    
    suspend fun syncUserDataFromBackend(): Boolean {
        return entryTestRepository.syncUserDataFromBackend()
    }
    
    // ========== OFFLINE ENTRY TEST METHODS ==========
    
    suspend fun needsEntryTest(): Boolean {
        return entryTestRepository.needsEntryTest()
    }
    
    suspend fun hasCompletedEntryTestOffline(): Boolean {
        return entryTestRepository.hasCompletedEntryTestOffline()
    }
    
    suspend fun saveEntryTestResultOffline(
        score: Int,
        courseId: Int?,
        courseName: String?
    ) {
        entryTestRepository.saveEntryTestResultOffline(
            score = score,
            courseId = courseId,
            courseName = courseName
        )
    }
    
    suspend fun entryTestNeedsSync(): Boolean {
        return entryTestRepository.entryTestNeedsSync()
    }
    
    suspend fun syncOfflineEntryTestToServer(): Boolean {
        return entryTestRepository.syncOfflineEntryTestToServer()
    }
    
    // ========== ENTRY TEST POPUP TRACKING ==========
    
    suspend fun shouldShowEntryTestPopup(): Boolean {
        return entryTestRepository.shouldShowEntryTestPopup()
    }
    
    suspend fun dismissEntryTestPopup() {
        entryTestRepository.dismissEntryTestPopup()
    }
    
    suspend fun resetEntryTestPopupDismissal() {
        entryTestRepository.resetEntryTestPopupDismissal()
    }
}
