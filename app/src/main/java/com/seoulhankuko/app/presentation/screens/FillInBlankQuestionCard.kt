package com.seoulhankuko.app.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.seoulhankuko.app.R
import com.seoulhankuko.app.data.api.model.QuestionResponse
import com.seoulhankuko.app.domain.model.ChallengeWithOptions
import com.seoulhankuko.app.presentation.utils.AppColors
import com.seoulhankuko.app.presentation.utils.LessonFlowColors
import kotlinx.coroutines.delay

private const val OPTIONS_PER_ROW = 2

private data class FillInBlankOption(
    val id: String,
    val text: String,
    val isCorrect: Boolean
)

@Composable
fun FillInBlankQuestionCard(
    challenge: ChallengeWithOptions,
    question: QuestionResponse?,
    questionIndex: Int,
    selectedOption: String?,
    answerStatus: AnswerStatus,
    onOptionSelected: (String) -> Unit,
    onAnswerSubmitted: (Boolean) -> Unit,
    onPlayAudio: (() -> Unit)? = null
) {
    val correctOption = remember(challenge) {
        challenge.options.firstOrNull { it.correct }
    }
    val correctAnswerValue = remember(challenge, question, correctOption) {
        challenge.options.firstOrNull { it.correct }?.text
            ?: question?.blank?.correctAnswer
            ?: question?.metadata?.choices?.firstOrNull { choice ->
                val normalized = choice.trim().lowercase()
                val normalizedCorrect = correctOption?.text?.trim()?.lowercase()
                normalizedCorrect != null && normalizedCorrect == normalized
            }
    }
    val options = remember(challenge, question, correctAnswerValue, correctOption) {
        val metadataChoices = question?.metadata?.choices
        if (!metadataChoices.isNullOrEmpty()) {
            metadataChoices.map { choiceText ->
                FillInBlankOption(
                    id = choiceText,
                    text = choiceText,
                    isCorrect = when {
                        correctAnswerValue != null -> {
                            choiceText.trim().equals(correctAnswerValue.trim(), ignoreCase = true)
                        }
                        correctOption != null -> {
                            choiceText.trim().equals(correctOption.text.trim(), ignoreCase = true)
                        }
                        else -> false
                    }
                )
            }
        } else {
            challenge.options.map { option ->
                FillInBlankOption(
                    id = option.id,
                    text = option.text,
                    isCorrect = option.correct
                )
            }
        }
    }
    val correctAnswerId = options.firstOrNull { it.isCorrect }?.id.orEmpty()
    val isSubmitted = answerStatus != AnswerStatus.NONE
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FillInBlankQuestionHeader(
                    text = challenge.challenge.question,
                    questionIndex = questionIndex,
                    showAudioIcon = onPlayAudio != null,
                    onPlayAudio = onPlayAudio
                )

                question?.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    options.chunked(OPTIONS_PER_ROW).forEachIndexed { rowIndex, rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowOptions.forEachIndexed { columnIndex, option ->
                                var visible by remember { mutableStateOf(false) }
                                val animationIndex = rowIndex * OPTIONS_PER_ROW + columnIndex
                                LaunchedEffect(option.id) {
                                    delay(animationIndex * 100L)
                                    visible = true
                                }

                                AnimatedVisibility(
                                    visible = visible,
                                    modifier = Modifier.weight(1f),
                                    enter = fadeIn(animationSpec = tween(200)) +
                                        scaleIn(animationSpec = tween(200))
                                ) {
                                    val isSelected = selectedOption == option.id

                                    FillBlankOptionCard(
                                        text = option.text,
                                        isSelected = isSelected,
                                        isCorrect = option.isCorrect,
                                        answerStatus = answerStatus,
                                        onClick = { onOptionSelected(option.id) }
                                    )
                                }
                            }

                            repeat(OPTIONS_PER_ROW - rowOptions.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                val explanationText = question?.explanation
                if (isSubmitted && !explanationText.isNullOrBlank()) {
                    Text(
                        text = "💡 Giải thích: $explanationText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LessonFlowColors.TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FillBlankOptionCard(
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    answerStatus: AnswerStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAnswered = answerStatus != AnswerStatus.NONE
    val backgroundColor = Color.White

    val borderColor by animateColorAsState(
        targetValue = when {
            !isAnswered && isSelected -> LessonFlowColors.PrimaryColor
            answerStatus == AnswerStatus.WRONG && isSelected -> LessonFlowColors.ErrorColor
            isAnswered && answerStatus == AnswerStatus.CORRECT && isCorrect -> LessonFlowColors.SuccessColor
            else -> AppColors.LightGray
        },
        animationSpec = tween(200),
        label = "fibOptionBorder"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            !isAnswered && isSelected -> LessonFlowColors.PrimaryColor
            answerStatus == AnswerStatus.WRONG && isSelected -> LessonFlowColors.ErrorColor
            isAnswered && answerStatus == AnswerStatus.CORRECT && isCorrect -> LessonFlowColors.SuccessColor
            else -> LessonFlowColors.TextPrimary
        },
        animationSpec = tween(200),
        label = "fibOptionText"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected && !isAnswered) 0.97f else 1f,
        animationSpec = tween(150),
        label = "fibOptionScale"
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
                .padding(horizontal = 16.dp, vertical = 20.dp),
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

@Composable
private fun FillInBlankQuestionHeader(
    text: String,
    questionIndex: Int,
    showAudioIcon: Boolean,
    onPlayAudio: (() -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Câu ${questionIndex + 1}",
            style = MaterialTheme.typography.titleSmall,
            color = LessonFlowColors.TextSecondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = highlightBlank(text),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp
                ),
                color = LessonFlowColors.TextPrimary,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )

            if (showAudioIcon && onPlayAudio != null) {
                IconButton(onClick = onPlayAudio) {
                    Icon(
                        painter = painterResource(id = R.drawable.volume_up_awesome),
                        contentDescription = "Phát âm",
                        tint = LessonFlowColors.PrimaryColor
                    )
                }
            }
        }

        Text(
            text = "Chọn đáp án đúng để hoàn thành câu.",
            style = MaterialTheme.typography.bodyMedium,
            color = LessonFlowColors.TextSecondary
        )
    }
}

private fun highlightBlank(text: String) = buildAnnotatedString {
    text.forEachIndexed { index, char ->
        val isUnderscore = char == '_'
        if (isUnderscore) {
            withStyle(
                SpanStyle(
                    background = LessonFlowColors.PrimaryColor.copy(alpha = 0.15f),
                    color = LessonFlowColors.PrimaryColor,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(char)
            }
        } else {
            append(char)
        }
    }
}
