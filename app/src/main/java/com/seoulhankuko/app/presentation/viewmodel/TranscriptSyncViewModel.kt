package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

data class TranscriptSegment(
    val index: Int,
    val text: String,
    val startMs: Long,
    val endMs: Long
)

data class TranscriptUiState(
    val segments: List<TranscriptSegment> = emptyList(),
    val currentPositionMs: Long = 0L,
    val highlightedIndex: Int = -1
)

class TranscriptSyncViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TranscriptUiState())
    val uiState: StateFlow<TranscriptUiState> = _uiState.asStateFlow()

    private var rawTranscript: String = ""
    private var mappedSegments: List<TranscriptSegment> = emptyList()
    private var fallbackTexts: List<String> = emptyList()
    private var totalDurationMs: Int = 0
    private var usingFallback = false

    fun updateTranscript(transcript: String?) {
        val normalized = transcript?.trim().orEmpty()
        if (normalized == rawTranscript) return

        rawTranscript = normalized
        usingFallback = false
        fallbackTexts = emptyList()

        val parsedSegments = parseSegmentsFromJson(normalized)
        if (parsedSegments.isNotEmpty()) {
            mappedSegments = parsedSegments.sortedBy { it.startMs }
            publishSegments(resetHighlight = true)
            return
        }

        fallbackTexts = splitTranscriptIntoSentences(normalized)
        usingFallback = fallbackTexts.isNotEmpty()

        if (usingFallback) {
            regenerateFallbackSegments()
        } else {
            mappedSegments = emptyList()
            publishSegments(resetHighlight = true)
        }
    }

    fun onPlaybackProgress(positionMs: Long, totalDurationMs: Int) {
        val position = positionMs.coerceAtLeast(0L)

        if (totalDurationMs >= 0 && totalDurationMs != this.totalDurationMs) {
            this.totalDurationMs = totalDurationMs.coerceAtLeast(0)
            if (usingFallback) {
                regenerateFallbackSegments()
            }
        }

        val segments = mappedSegments
        if (segments.isEmpty()) return

        val highlightedIndex = findHighlightedIndex(position)

        _uiState.update { state ->
            if (state.highlightedIndex == highlightedIndex &&
                state.currentPositionMs == position
            ) {
                state
            } else {
                state.copy(
                    currentPositionMs = position,
                    highlightedIndex = highlightedIndex
                )
            }
        }
    }

    fun onSeekToSegment(index: Int): Long? {
        val segment = mappedSegments.firstOrNull { it.index == index } ?: return null

        _uiState.update {
            it.copy(
                highlightedIndex = segment.index,
                currentPositionMs = segment.startMs
            )
        }

        return segment.startMs
    }

    private fun regenerateFallbackSegments() {
        if (!usingFallback) {
            mappedSegments = emptyList()
            publishSegments(resetHighlight = true)
            return
        }

        val texts = fallbackTexts
        if (texts.isEmpty()) {
            mappedSegments = emptyList()
            publishSegments(resetHighlight = true)
            return
        }

        val duration = totalDurationMs.takeIf { it > 0 }
            ?: (texts.size * 1200).coerceAtLeast(4000)

        mappedSegments = texts.mapIndexed { index, text ->
            val start = (index.toLong() * duration) / texts.size
            val end = (((index + 1).toLong() * duration) / texts.size)
                .coerceAtMost(duration.toLong())
            TranscriptSegment(
                index = index,
                text = text,
                startMs = start,
                endMs = end
            )
        }

        publishSegments(resetHighlight = true)
    }

    private fun publishSegments(resetHighlight: Boolean) {
        _uiState.update {
            val newHighlight = if (resetHighlight) {
                mappedSegments.firstOrNull()?.index ?: -1
            } else {
                it.highlightedIndex.coerceAtLeast(-1)
            }
            it.copy(
                segments = mappedSegments,
                highlightedIndex = newHighlight.coerceAtMost(mappedSegments.lastOrNull()?.index ?: -1)
            )
        }
    }

    private fun findHighlightedIndex(position: Long): Int {
        val segments = mappedSegments
        if (segments.isEmpty()) return -1

        val currentSegment = segments.lastOrNull { position in it.startMs..it.endMs }
            ?: when {
                position < segments.first().startMs -> segments.first()
                position > segments.last().endMs -> segments.last()
                else -> null
            }

        return currentSegment?.index ?: -1
    }

    private fun splitTranscriptIntoSentences(transcript: String): List<String> {
        if (transcript.isBlank()) return emptyList()

        val regex = Regex("(?<=[.!?])\\s+")
        val sentences = transcript
            .split(regex)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return if (sentences.isNotEmpty()) {
            sentences
        } else {
            transcript.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }

    private fun parseSegmentsFromJson(raw: String): List<TranscriptSegment> {
        if (raw.isBlank()) return emptyList()

        return try {
            val trimmed = raw.trim()
            val jsonArray = when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                trimmed.startsWith("{") -> {
                    val obj = JSONObject(trimmed)
                    obj.optJSONArray("segments") ?: return emptyList()
                }
                else -> return emptyList()
            }

            buildList {
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(i) ?: continue
                    val text = item.optString("text", "").trim()
                    if (text.isEmpty()) continue

                    val startMs = (item.optDouble("start", 0.0) * 1000).toLong().coerceAtLeast(0L)
                    val endMs = (item.optDouble("end", startMs / 1000.0) * 1000).toLong()
                        .coerceAtLeast(startMs + 10)

                    add(
                        TranscriptSegment(
                            index = size,
                            text = text,
                            startMs = startMs,
                            endMs = endMs
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
