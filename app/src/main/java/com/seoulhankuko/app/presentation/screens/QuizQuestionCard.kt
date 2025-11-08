package com.seoulhankuko.app.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.seoulhankuko.app.R
import com.seoulhankuko.app.domain.model.ChallengeWithOptions
import com.seoulhankuko.app.presentation.utils.AppColors
import com.seoulhankuko.app.presentation.utils.LessonFlowColors
import kotlin.math.roundToInt

private const val OPTIONS_PER_ROW = 2

@Composable
fun QuizQuestionCard(
    challenge: ChallengeWithOptions,
    selectedOption: String?,
    answerStatus: AnswerStatus,
    onOptionSelected: (String) -> Unit,
    onAnswerSubmitted: (Boolean) -> Unit,
    questionIndex: Int,
    onPlayAudio: (() -> Unit)? = null
) {
    val isAnswered = answerStatus != AnswerStatus.NONE
    val correctOption = challenge.options.firstOrNull { it.correct }
    val audioAvailable = onPlayAudio != null && challenge.options.any { !it.audioSrc.isNullOrBlank() }
    val imageUrl = challenge.options.firstOrNull { !it.imageSrc.isNullOrBlank() }?.imageSrc
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(answerStatus) {
        if (answerStatus == AnswerStatus.WRONG) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 500
                    -8f at 50
                    8f at 100
                    -6f at 150
                    6f at 200
                    -4f at 250
                    4f at 300
                    -2f at 350
                    2f at 400
                    0f at 500
                }
            )
        } else {
            shakeOffset.snapTo(0f)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Câu ${questionIndex + 1}",
                style = MaterialTheme.typography.titleSmall,
                color = LessonFlowColors.TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = challenge.challenge.question,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = LessonFlowColors.TextPrimary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )

                if (audioAvailable && onPlayAudio != null) {
                    IconButton(onClick = onPlayAudio) {
                        Icon(
                            painter = painterResource(id = R.drawable.volume_up_awesome),
                            contentDescription = "Phát âm",
                            tint = LessonFlowColors.PrimaryColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Hình minh hoạ",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                challenge.options
                    .chunked(OPTIONS_PER_ROW)
                    .forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowOptions.forEach { option ->
                                val isSelected = selectedOption == option.id
                                val isCorrectOption = option.id == correctOption?.id

                                Box(modifier = Modifier.weight(1f, fill = true)) {
                                    QuizOptionCard(
                                        text = option.text,
                                        isSelected = isSelected,
                                        answerStatus = answerStatus,
                                        isCorrect = isCorrectOption,
                                        onClick = {
                                            if (!isAnswered) {
                                                onOptionSelected(option.id)
                                            }
                                        }
                                    )
                                }
                            }

                            if (rowOptions.size < OPTIONS_PER_ROW) {
                                Spacer(modifier = Modifier.width(0.dp).weight(1f, fill = true))
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun QuizOptionCard(
    text: String,
    isSelected: Boolean,
    answerStatus: AnswerStatus,
    isCorrect: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAnswered = answerStatus != AnswerStatus.NONE
    val backgroundColor = Color.White

    val textColor by animateColorAsState(
        targetValue = when {
            !isAnswered && isSelected -> LessonFlowColors.PrimaryColor
            answerStatus == AnswerStatus.WRONG && isSelected -> LessonFlowColors.ErrorColor
            answerStatus == AnswerStatus.CORRECT && isCorrect -> LessonFlowColors.SuccessColor
            else -> LessonFlowColors.TextPrimary
        },
        animationSpec = tween(200),
        label = "optionText"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !isAnswered && isSelected -> LessonFlowColors.PrimaryColor
            answerStatus == AnswerStatus.WRONG && isSelected -> LessonFlowColors.ErrorColor
            answerStatus == AnswerStatus.CORRECT && isCorrect -> LessonFlowColors.SuccessColor
            else -> AppColors.LightGray
        },
        animationSpec = tween(200),
        label = "optionBorder"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected && !isAnswered) 0.97f else 1f,
        animationSpec = tween(150),
        label = "optionScale"
    )

    Card(
        onClick = {
            if (!isAnswered) {
                onClick()
            }
        },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 0.dp),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 80.dp)
                .padding(horizontal = 12.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
