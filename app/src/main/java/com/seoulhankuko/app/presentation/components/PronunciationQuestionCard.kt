package com.seoulhankuko.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.seoulhankuko.app.R
import com.seoulhankuko.app.presentation.utils.LessonFlowColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

data class PronunciationEvaluationUiState(
    val transcript: String? = null,
    val score: Float? = null,
    val passed: Boolean = false,
    val isEvaluated: Boolean = false,
    val errorMessage: String? = null
)

@Composable
fun PronunciationQuestionCard(
    prompt: String,
    modifier: Modifier = Modifier,
    explanation: String? = null,
    referenceAudioUrl: String? = null,
    evaluationState: PronunciationEvaluationUiState = PronunciationEvaluationUiState(),
    isProcessing: Boolean = false,
    onRecordStart: () -> Boolean,
    onRecordStop: suspend () -> Unit,
    onRetry: () -> Unit,
    onListenNative: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isRecording by rememberSaveable { mutableStateOf(false) }
    val waveformLevels = remember { mutableStateListOf<Float>().apply { repeat(32) { add(0.2f) } } }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                waveformLevels.indices.forEach { index ->
                    waveformLevels[index] = Random.nextFloat().coerceIn(0.15f, 1f)
                }
                delay(70)
            }
        } else {
            waveformLevels.indices.forEach { index ->
                waveformLevels[index] = (waveformLevels[index] * 0.45f).coerceAtMost(0.55f)
            }
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = LessonFlowColors.TextPrimary
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!explanation.isNullOrBlank()) {
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LessonFlowColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            RecordingWaveform(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(84.dp),
                levels = waveformLevels,
                isRecording = isRecording || isProcessing
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (isProcessing) {
                            return@Button
                        }
                        if (isRecording) {
                            isRecording = false
                            coroutineScope.launch {
                                onRecordStop()
                            }
                        } else {
                            val started = onRecordStart()
                            if (started) {
                                isRecording = true
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) LessonFlowColors.ErrorColor else LessonFlowColors.PrimaryColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = if (isRecording) painterResource(id = R.drawable.mic_off)
                            else painterResource(id = R.drawable.mic),
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isRecording) "Dừng ghi âm" else "Bắt đầu ghi âm",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                if (isProcessing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = LessonFlowColors.PrimaryColor,
                        trackColor = LessonFlowColors.SecondaryColor.copy(alpha = 0.4f)
                    )
                }
            }

                AnimatedVisibility(
                visible = !evaluationState.transcript.isNullOrBlank() || evaluationState.score != null,
                enter = fadeIn() + scaleIn()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = LessonFlowColors.SecondaryColor.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    evaluationState.transcript?.let { transcript ->
                        Text(
                            text = "Bản ghi: $transcript",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LessonFlowColors.TextPrimary
                        )
                    }

                    evaluationState.score?.let { score ->
                        val displayScore = if (score <= 1f) {
                            (score * 100f).coerceIn(0f, 100f)
                        } else {
                            score
                        }
                        Text(
                            text = "Điểm phát âm: ${displayScore.roundToInt()}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = LessonFlowColors.TextPrimary
                        )
                        Text(
                            text = if (evaluationState.passed) "Tuyệt vời! Bạn có thể tiếp tục." else "Cố gắng lần nữa để đạt điểm trên 60%.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (evaluationState.passed) LessonFlowColors.SuccessColor else LessonFlowColors.TextSecondary
                        )
                    }
                    evaluationState.errorMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = LessonFlowColors.ErrorColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingWaveform(
    modifier: Modifier,
    levels: List<Float>,
    isRecording: Boolean
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(color = LessonFlowColors.SecondaryColor.copy(alpha = 0.2f))
            .border(
                width = 1.dp,
                color = LessonFlowColors.PrimaryColor.copy(alpha = 0.35f),
                shape = shape
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (levels.isEmpty()) {
            Text(
                text = if (isRecording) "Đang ghi âm..." else "Sóng âm sẽ hiển thị ở đây",
                style = MaterialTheme.typography.bodyMedium,
                color = LessonFlowColors.TextSecondary,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                levels.forEach { level ->
                    val barHeight = (level.coerceIn(0.1f, 1f) * 48f).dp + 12.dp
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                color = if (isRecording) LessonFlowColors.PrimaryColor
                                else LessonFlowColors.PrimaryColor.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
    }
}

