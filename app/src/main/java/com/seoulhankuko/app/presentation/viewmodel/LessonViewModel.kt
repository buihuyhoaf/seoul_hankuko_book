package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.api.model.LessonProgressUpdateRequest
import com.seoulhankuko.app.data.api.model.QuestionResponse
import com.seoulhankuko.app.data.audio.PronunciationRecorder
import com.seoulhankuko.app.data.audio.WavUtils
import com.seoulhankuko.app.data.local.UserPreferencesManager
import com.seoulhankuko.app.data.repository.AuthRepository
import com.seoulhankuko.app.data.repository.LessonRepository
import com.seoulhankuko.app.data.repository.AdditionalLessonChallenges
import com.seoulhankuko.app.domain.model.AnswerStatus
import com.seoulhankuko.app.domain.model.ChallengeWithOptions
import com.seoulhankuko.app.domain.model.LessonWithChallenges
import com.seoulhankuko.app.presentation.components.PronunciationEvaluationUiState
import com.seoulhankuko.app.presentation.components.TTSManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class LessonViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val authRepository: AuthRepository,
    private val userPreferencesManager: UserPreferencesManager,
    val ttsManager: TTSManager,
    private val pronunciationRecorder: PronunciationRecorder
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<LessonUiState>(LessonUiState.Loading)
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    private val _streakCelebration = MutableStateFlow<StreakCelebrationEvent?>(null)
    val streakCelebration: StateFlow<StreakCelebrationEvent?> = _streakCelebration.asStateFlow()
    private val _comboCelebration = MutableStateFlow<ComboCelebrationState?>(null)
    val comboCelebration: StateFlow<ComboCelebrationState?> = _comboCelebration.asStateFlow()

    private val _lessonExpProgress = MutableStateFlow(LessonExpProgress())
    private val recordedQuestionIds = mutableSetOf<String>()
    private val recordedExerciseIds = mutableSetOf<String>()
    private val recordedExerciseTypes = mutableSetOf<String>()
    private val _pronunciationEvaluations =
        MutableStateFlow<Map<String, PronunciationEvaluationUiState>>(emptyMap())
    val pronunciationEvaluations: StateFlow<Map<String, PronunciationEvaluationUiState>> =
        _pronunciationEvaluations.asStateFlow()
    private val _pronunciationProcessing = MutableStateFlow<Set<String>>(emptySet())
    val pronunciationProcessing: StateFlow<Set<String>> = _pronunciationProcessing.asStateFlow()
    private var consecutiveCorrectAnswers = 0
    
    fun loadLesson(lessonId: String, showLoading: Boolean = true) {
        if (showLoading) {
            _uiState.update { LessonUiState.Loading }
        }
        
        viewModelScope.launch {
            try {
                Timber.d("Loading lesson $lessonId")
                
                // Get authentication token
                val token = authRepository.getCurrentToken()
                val userId = "guest"
                
                Timber.d("Using token: ${token?.take(20)}..., userId: $userId")
                
                val lessonWithChallenges = lessonRepository.getLessonWithChallenges(lessonId, userId ?: "guest", token)
                if (lessonWithChallenges != null) {
                    Timber.d("Successfully loaded lesson with ${lessonWithChallenges.challenges.size} challenges")
                    Timber.d("Lesson title: ${lessonWithChallenges.lesson.title}")
                    Timber.d("Challenges: ${lessonWithChallenges.challenges.map { it.challenge.question }}")
                    
                    _uiState.update {
                        LessonUiState.Success(
                            lessonWithChallenges = lessonWithChallenges,
                            userProgress = null,
                            currentChallengeIndex = 0,
                            hasMoreQuestions = lessonWithChallenges.hasMoreQuestions
                        )
                    }

                    persistCurrentLesson(lessonWithChallenges)
                } else {
                    Timber.w("Lesson not found for ID: $lessonId")
                    if (showLoading) {
                        _uiState.update { 
                            LessonUiState.Error("Lesson not found. Please check your internet connection and try again.")
                        }
                    } else {
                        Timber.w("Skipping error state for missing lesson $lessonId during silent reload")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load lesson $lessonId")
                val errorMessage = when {
                    e.message?.contains("network", ignoreCase = true) == true -> 
                        "Network error. Please check your internet connection."
                    e.message?.contains("unauthorized", ignoreCase = true) == true -> 
                        "Authentication failed. Please sign in again."
                    e.message?.contains("timeout", ignoreCase = true) == true -> 
                        "Request timeout. Please try again."
                    else -> "Failed to load lesson: ${e.message ?: "Unknown error"}"
                }
                if (showLoading) {
                    _uiState.update { LessonUiState.Error(errorMessage) }
                } else {
                    Timber.w("Silent reload failed for lesson $lessonId with error: $errorMessage")
                }
            }
        }
    }

    private fun persistCurrentLesson(lessonWithChallenges: LessonWithChallenges) {
        viewModelScope.launch {
            try {
                val courseId = userPreferencesManager.getCurrentCourseId()
                userPreferencesManager.saveCurrentLesson(
                    lessonId = lessonWithChallenges.lesson.id,
                    lessonTitle = lessonWithChallenges.lesson.title,
                    unitId = lessonWithChallenges.lesson.unitId,
                    courseId = courseId
                )
            } catch (e: Exception) {
                Timber.w(e, "Failed to persist current lesson")
            }
        }
    }

    fun clearCurrentLesson() {
        viewModelScope.launch {
            try {
                userPreferencesManager.clearCurrentLesson()
                resetExpTracking()
            } catch (e: Exception) {
                Timber.w(e, "Failed to clear current lesson")
            }
        }
    }
    
    private fun getCurrentUserId(): String {
        // Try to get user ID from auth state or use default
        return try {
            // This is a simplified approach - in a real app you might want to observe auth state
            "default_user" // TODO: Get actual user ID from AuthState
        } catch (e: Exception) {
            Timber.w(e, "Failed to get user ID, using default")
            "default_user"
        }
    }
    
    fun selectOption(optionId: Int) {
        val currentState = _uiState.value
        if (currentState !is LessonUiState.Success) return
        
        // Only allow changing answer if status is NONE (before checking)
        if (currentState.status != AnswerStatus.NONE) {
            return
        }
        
        _uiState.update { 
            (it as? LessonUiState.Success)?.copy(selectedOption = optionId) ?: it
        }
    }

    fun submitPracticeCorrectAnswer(
        lessonId: String,
        questionId: String,
        selectedOptionId: String
    ) {
        viewModelScope.launch {
            try {
                val token = authRepository.getCurrentToken()
                if (!token.isNullOrBlank() && lessonId.isNotEmpty() && questionId.isNotEmpty()) {
                    val result = lessonRepository.submitPracticeQuestion(
                        lessonId = lessonId,
                        questionId = questionId,
                        token = token,
                        selectedOptionId = selectedOptionId
                    )
                    if (result.isSuccess) {
                        Timber.d("Practice submit API success for lesson=$lessonId question=$questionId")
                    } else {
                        result.exceptionOrNull()?.let { err ->
                            Timber.e(err, "Practice submit API failed")
                        }
                    }
                } else {
                    Timber.w("Skip practice submit: token=${token?.take(5)}..., lessonId=$lessonId, questionId=$questionId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to submit practice question to backend")
            }
        }
    }

    fun beginPronunciationRecording(questionId: String): Boolean {
        Timber.d("Begin pronunciation recording for question %s", questionId)
        pronunciationRecorder.reset()
        _pronunciationProcessing.update { it - questionId }
        _pronunciationEvaluations.update { it - questionId }
        val started = pronunciationRecorder.startRecording()
        if (!started) {
            _pronunciationEvaluations.update {
                it + (questionId to PronunciationEvaluationUiState(
                    transcript = null,
                    score = null,
                    passed = false,
                    isEvaluated = false,
                    errorMessage = "Không thể bắt đầu ghi âm. Vui lòng kiểm tra quyền truy cập micro."
                ))
            }
        }
        return started
    }

    suspend fun completePronunciationRecording(
        lessonId: String,
        questionId: String,
        sentence: String
    ): PronunciationEvaluationUiState? {
        Timber.d("Complete pronunciation recording for lesson=%s question=%s", lessonId, questionId)
        _pronunciationProcessing.update { it + questionId }
        return try {
            val pcmBytes = pronunciationRecorder.stopRecording()
            if (pcmBytes.isEmpty()) {
                Timber.w("Pronunciation recording for %s produced empty buffer", questionId)
                PronunciationEvaluationUiState(
                    transcript = "",
                    score = 0f,
                    passed = false,
                    isEvaluated = true,
                    errorMessage = "Không thu được âm thanh. Vui lòng thử lại."
                ).also { evaluation ->
                    _pronunciationEvaluations.update { it + (questionId to evaluation) }
                }
            } else {
                val wavBytes = WavUtils.pcmToWav(
                    pcmBytes,
                    PronunciationRecorder.SAMPLE_RATE,
                    PronunciationRecorder.CHANNEL_COUNT,
                    PronunciationRecorder.BITS_PER_SAMPLE
                )
                val token = authRepository.getCurrentToken()
                if (token.isNullOrBlank()) {
                    Timber.w("No auth token available, cannot evaluate pronunciation")
                    val evaluation = PronunciationEvaluationUiState(
                        transcript = "",
                        score = 0f,
                        passed = false,
                        isEvaluated = true,
                        errorMessage = "Không thể gửi lên máy chủ. Vui lòng đăng nhập lại."
                    )
                    _pronunciationEvaluations.update { it + (questionId to evaluation) }
                    evaluation
                } else {
                    val result = lessonRepository.evaluatePronunciation(
                        token = token,
                        audioBytes = wavBytes,
                        sentence = sentence,
                        fileName = "${questionId}.wav"
                    )
                    val evaluation = result.fold(
                        onSuccess = { data ->
                            PronunciationEvaluationUiState(
                                transcript = data.transcript,
                                score = data.score,
                                passed = data.passed,
                                isEvaluated = true
                            )
                        },
                        onFailure = { error ->
                            Timber.e(error, "Pronunciation evaluation failed for %s", questionId)
                            PronunciationEvaluationUiState(
                                transcript = "",
                                score = 0f,
                                passed = false,
                                isEvaluated = true,
                                errorMessage = "Đánh giá thất bại. Vui lòng thử lại."
                            )
                        }
                    )
                    _pronunciationEvaluations.update { it + (questionId to evaluation) }
                    evaluation
                }
            }
        } finally {
            _pronunciationProcessing.update { it - questionId }
        }
    }

    fun resetPronunciationAttempt(questionId: String) {
        Timber.d("Reset pronunciation attempt for question %s", questionId)
        pronunciationRecorder.reset()
        _pronunciationProcessing.update { it - questionId }
        _pronunciationEvaluations.update { it - questionId }
    }

    fun submitExercise(
        exerciseId: String,
        lessonId: String,
        selectedAnswers: Map<String, String>? = null,
        response: String? = null,
        audioUrl: String? = null
    ) {
        viewModelScope.launch {
            try {
                val token = authRepository.getCurrentToken()
                if (!token.isNullOrBlank() && exerciseId.isNotEmpty() && lessonId.isNotEmpty()) {
                    val result = lessonRepository.submitExercise(
                        exerciseId = exerciseId,
                        token = token,
                        response = response,
                        audioUrl = audioUrl,
                        selectedAnswers = selectedAnswers
                    )
                    val responseMap = result.getOrNull()
                    if (responseMap != null) {
                        Timber.d("Exercise submit API success for exercise=$exerciseId lesson=$lessonId")
                        handleStreakInfoPayload((responseMap as? Map<*, *>)?.get("streak_info"))
                        markExerciseCompletion(exerciseId)
                            loadLesson(lessonId, showLoading = false)
                    } else {
                        result.exceptionOrNull()?.let { err ->
                            Timber.e(err, "Exercise submit API failed")
                        }
                    }
                } else {
                    Timber.w("Skip exercise submit: token=${token?.take(5)}..., exerciseId=$exerciseId, lessonId=$lessonId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to submit exercise to backend")
            }
        }
    }

    suspend fun fetchAdditionalChallenges(
        lessonId: String,
        offset: Int,
        limit: Int = 10
    ): Result<AdditionalChallengesResult> {
        return try {
            val token = authRepository.getCurrentToken()
            lessonRepository.fetchAdditionalLessonChallenges(
                lessonId = lessonId,
                offset = offset,
                limit = limit,
                token = token
            ).map { additional ->
                AdditionalChallengesResult(
                    challenges = additional.challenges,
                    questionResponses = additional.questionResponses,
                    hasMore = additional.hasMore
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch additional challenges for lesson $lessonId")
            Result.failure(e)
        }
    }
    
    /**
     * Updates lesson progress when a quiz or exercise is completed
     * This method should be called from ExerciseScreen, etc.
     */
    fun updateProgress(lessonId: Int) {
        // No-op: backend progress update removed; just mark updated in UI
        _uiState.update { currentState ->
            if (currentState is LessonUiState.Success) {
                currentState.copy(progressUpdated = true)
            } else currentState
        }
    }
    
    /**
     * Updates lesson progress on backend and calls callback when done
     * Also reloads lesson data to get updated progress
     * Updates streak if lesson is newly completed
     */
    fun updateLessonProgress(
        lessonId: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            var celebrationScheduled = false
            try {
                val token = authRepository.getCurrentToken()
                val payload = buildLessonProgressPayload()
                val totalExpGain = payload.questionExp + payload.listeningExp + payload.speakingExp + payload.writingExp
                val result = lessonRepository.updateLessonProgress(lessonId, token, payload)

                val responseBody = result.getOrNull()
                if (responseBody != null) {
                    Timber.d("Successfully updated lesson progress for lesson $lessonId")
                    val celebrationFromPayload = handleStreakInfoPayload((responseBody as? Map<*, *>)?.get("streak_info"))
                    celebrationScheduled = celebrationScheduled || celebrationFromPayload

                    val lessonProgressMap = (responseBody as? Map<*, *>)?.get("lesson_progress") as? Map<*, *>
                    val progressPercent = (lessonProgressMap?.get("progress_percent") as? Number)?.toFloat() ?: 0f
                    val isNowCompleted = progressPercent >= 80f

                    // Reload lesson data to get updated progress
                    loadLesson(lessonId, showLoading = false)

                    _uiState.update { currentState ->
                        if (currentState is LessonUiState.Success) {
                            currentState.copy(progressUpdated = true)
                        } else currentState
                    }
                    resetExpTracking()
                    viewModelScope.launch {
                        authRepository.refreshCurrentUserData()
                    }
                } else {
                    result.exceptionOrNull()?.let { error ->
                        Timber.e(error, "Failed to update lesson progress for lesson $lessonId")
                    }
                    // Reload anyway to try to get latest progress
                    loadLesson(lessonId, showLoading = false)

                    // Still mark as updated in UI even if API call failed
                    _uiState.update { currentState ->
                        if (currentState is LessonUiState.Success) {
                            currentState.copy(progressUpdated = true)
                        } else currentState
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while updating lesson progress")
                // Reload anyway
                loadLesson(lessonId, showLoading = false)

                // Still mark as updated in UI even if exception occurs
                _uiState.update { currentState ->
                    if (currentState is LessonUiState.Success) {
                        currentState.copy(progressUpdated = true)
                    } else currentState
                }
            } finally {
                onComplete(celebrationScheduled)
            }
        }
    }
    
    /**
     * Updates user streak after completing activities
     */
    private fun Any?.asBooleanOrNull(): Boolean? = when (this) {
        is Boolean -> this
        is Number -> this.toInt() != 0
        is String -> when {
            equals("true", ignoreCase = true) || this == "1" -> true
            equals("false", ignoreCase = true) || this == "0" -> false
            else -> null
        }
        else -> null
    }

    private fun Any?.asIntOrNull(): Int? = when (this) {
        is Number -> this.toInt()
        is String -> this.toIntOrNull()
        else -> null
    }

    private suspend fun handleStreakInfoPayload(payload: Any?): Boolean {
        val infoMap = payload as? Map<*, *> ?: return false

        val updatedKeys = listOf(
            "streak_updated",
            "streakUpdated",
            "updated",
            "is_updated",
            "isUpdated",
            "should_show",
            "shouldShow",
            "force_show",
            "forceShow"
        )
        val updatedFlag = updatedKeys
            .asSequence()
            .mapNotNull { key -> infoMap[key]?.asBooleanOrNull() }
            .firstOrNull()
            ?: false

        val streakDayKeys = listOf(
            "new_streak",
            "newStreak",
            "current_streak",
            "currentStreak",
            "streak_days",
            "streakDays"
        )
        val streakDays = streakDayKeys
            .asSequence()
            .mapNotNull { key -> infoMap[key]?.asIntOrNull() }
            .firstOrNull()

        val bonusExpKeys = listOf(
            "streak_bonus_exp",
            "streakBonusExp",
            "bonus_exp",
            "bonusExp"
        )
        val bonusExp = bonusExpKeys
            .asSequence()
            .mapNotNull { key -> infoMap[key]?.asIntOrNull() }
            .firstOrNull()

        val currentStoredStreak = userPreferencesManager.userData.first().streakDays
        if (streakDays != null) {
            userPreferencesManager.updateStreakDays(streakDays)
        }

        val resolvedUpdated = when {
            updatedFlag -> true
            streakDays != null && streakDays > currentStoredStreak -> true
            else -> false
        }

        if (resolvedUpdated) {
            viewModelScope.launch {
                authRepository.refreshCurrentUserData()
            }
        }

        return maybeScheduleStreakCelebration(streakDays, bonusExp, resolvedUpdated)
    }

    private suspend fun maybeScheduleStreakCelebration(
        streakDays: Int?,
        bonusExp: Int?,
        updated: Boolean
    ): Boolean {
        if (!updated) return false
        val days = streakDays ?: return false
        if (days <= 0) return false

        val today = LocalDate.now()
        val lastShownIso = userPreferencesManager.getStreakScreenLastShownDate()
        if (lastShownIso == today.toString()) {
            Timber.d("Streak celebration already shown today ($today)")
            return false
        }

        val pendingCelebration = _streakCelebration.value
        if (pendingCelebration?.streakDays == days) {
            return true
        }

        _streakCelebration.value = StreakCelebrationEvent(
            streakDays = days,
            bonusExp = bonusExp ?: 0,
            celebrationDate = today
        )
        return true
    }

    fun markStreakCelebrationDisplayed() {
        viewModelScope.launch {
            val today = LocalDate.now()
            userPreferencesManager.setStreakScreenLastShownDate(today.toString())
        }
    }

    fun clearStreakCelebration() {
        _streakCelebration.value = null
    }
    
    private fun resetExpTracking() {
        recordedQuestionIds.clear()
        recordedExerciseIds.clear()
        recordedExerciseTypes.clear()
        _lessonExpProgress.value = LessonExpProgress()
        consecutiveCorrectAnswers = 0
        _comboCelebration.value = null
    }
    
    fun recordQuestionCompletion(questionId: String, expGained: Float) {
        if (questionId.isBlank() || !recordedQuestionIds.add(questionId)) {
            return
        }
        if (expGained <= 0f) {
            return
        }
        _lessonExpProgress.update { current ->
            current.copy(questionExp = current.questionExp + expGained)
        }
    }
    
    fun recordListeningExp(expGained: Int) {
        val normalized = expGained.coerceAtLeast(0)
        _lessonExpProgress.update { current ->
            current.copy(listeningExp = normalized.toFloat())
        }
    }

    fun onAnswerEvaluated(isCorrect: Boolean): ComboCelebrationState? {
        if (isCorrect) {
            consecutiveCorrectAnswers += 1
            val milestone = when {
                consecutiveCorrectAnswers % ComboMilestone.GOLD.threshold == 0 -> ComboMilestone.GOLD
                consecutiveCorrectAnswers % ComboMilestone.SILVER.threshold == 0 -> ComboMilestone.SILVER
                else -> null
            }
            return milestone?.let { comboMilestone ->
                ComboCelebrationState(
                    milestone = comboMilestone,
                    comboCount = consecutiveCorrectAnswers,
                    triggeredAt = System.currentTimeMillis()
                ).also { celebration ->
                    _comboCelebration.value = celebration
                }
            }
        } else {
            if (consecutiveCorrectAnswers != 0) {
                consecutiveCorrectAnswers = 0
            }
            _comboCelebration.value = null
        }
        return null
    }

    fun clearComboCelebration() {
        _comboCelebration.value = null
    }
 
    fun markExerciseCompletion(exerciseId: String?, explicitType: String? = null) {
        var effectiveType = explicitType?.lowercase()
        var alreadyRecorded = false
        if (!exerciseId.isNullOrBlank()) {
            alreadyRecorded = !recordedExerciseIds.add(exerciseId)
        } else if (effectiveType != null) {
            alreadyRecorded = !recordedExerciseTypes.add(effectiveType)
        }
        if (alreadyRecorded) return
        
        if (effectiveType == null) {
            val successState = _uiState.value as? LessonUiState.Success ?: return
            val lessonExercises = successState.lessonWithChallenges?.exercises.orEmpty()
            val rawType = lessonExercises.firstOrNull { it.id == exerciseId }?.type ?: return
            effectiveType = rawType.lowercase()
        }
        
        val updateApplied = when (effectiveType) {
            "speaking", "pronunciation" -> {
                _lessonExpProgress.update { current ->
                    current.copy(speakingExp = current.speakingExp + SKILL_EXERCISE_EXP_REWARD)
                }
                true
            }
            "writing", "writing_practice" -> {
                _lessonExpProgress.update { current ->
                    current.copy(writingExp = current.writingExp + SKILL_EXERCISE_EXP_REWARD)
                }
                true
            }
            else -> false
        }
        
        if (!updateApplied) {
            exerciseId?.let { recordedExerciseIds.remove(it) }
            effectiveType?.let { recordedExerciseTypes.remove(it) }
        }
    }
    
    private fun buildLessonProgressPayload(): LessonProgressUpdateRequest {
        val snapshot = _lessonExpProgress.value
        return LessonProgressUpdateRequest(
            questionExp = snapshot.questionExp.roundToInt().coerceAtLeast(0),
            listeningExp = snapshot.listeningExp.roundToInt().coerceAtLeast(0),
            speakingExp = snapshot.speakingExp.roundToInt().coerceAtLeast(0),
            writingExp = snapshot.writingExp.roundToInt().coerceAtLeast(0)
        )
    }
    
    companion object {
        private const val SKILL_EXERCISE_EXP_REWARD = 20
    }
}

data class LessonExpProgress(
    val questionExp: Float = 0f,
    val listeningExp: Float = 0f,
    val speakingExp: Float = 0f,
    val writingExp: Float = 0f
)

data class StreakCelebrationEvent(
    val streakDays: Int,
    val bonusExp: Int,
    val celebrationDate: LocalDate
)

data class ComboCelebrationState(
    val milestone: ComboMilestone,
    val comboCount: Int,
    val triggeredAt: Long = System.currentTimeMillis()
)

enum class ComboMilestone(val threshold: Int) {
    SILVER(5),
    GOLD(10)
}
 
data class AdditionalChallengesResult(
    val challenges: List<ChallengeWithOptions>,
    val questionResponses: Map<String, QuestionResponse>,
    val hasMore: Boolean
)

// UI State - exported for use in Composables
sealed class LessonUiState {
    object Loading : LessonUiState()
    
    data class Success(
        val lessonWithChallenges: LessonWithChallenges? = null,
        val userProgress: Any? = null, // Removed LocalUserProgressRepository dependency
        val currentChallengeIndex: Int = 0,
        val selectedOption: Int? = null,
        val status: AnswerStatus = AnswerStatus.NONE,
        val isLessonCompleted: Boolean = false,
        val heartsReduced: Boolean = false,
        val completedChallenges: Set<Int> = emptySet(),
        val canProceedAfterWrong: Boolean = false,
        val progressUpdated: Boolean = false,
        val hasMoreQuestions: Boolean = false
    ) : LessonUiState() {

        
        fun getTotalChallenges(): Int {
            return lessonWithChallenges?.challenges?.size ?: 0
        }
    }
    
    data class Error(val message: String) : LessonUiState()
}

