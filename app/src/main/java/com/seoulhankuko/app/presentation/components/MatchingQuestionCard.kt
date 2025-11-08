package com.seoulhankuko.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seoulhankuko.app.presentation.utils.AppColors
import com.seoulhankuko.app.presentation.utils.LessonFlowColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class MatchingQuestion(
    val content: String,
    val pairs: List<Pair<String, String>>,
    val explanation: String,
    val hasAudio: Boolean = false
)

@Composable
fun MatchingQuestionCard(
    question: MatchingQuestion,
    modifier: Modifier = Modifier,
    onMatchCompleted: (Boolean) -> Unit,
    onMatchFeedback: (Boolean) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    val leftItems = remember(question) {
        question.pairs.mapIndexed { index, pair ->
            MatchingDisplayItem(originalIndex = index, text = pair.first)
        }
    }

    val rightItems = remember(question) {
        question.pairs.mapIndexed { index, pair ->
            MatchingDisplayItem(originalIndex = index, text = pair.second)
        }.shuffled()
    }

    val matchedOriginalIndexes = remember(question) { mutableStateMapOf<Int, Boolean>() }
    var pendingSelection by remember(question) { mutableStateOf<PendingSelection?>(null) }
    var errorHighlight by remember(question) { mutableStateOf<ErrorHighlight?>(null) }
    var interactionLocked by remember(question) { mutableStateOf(false) }
    var showExplanation by remember(question) { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(question) {
        shakeOffset.snapTo(0f)
    }

    LaunchedEffect(matchedOriginalIndexes.size, question) {
        val allMatched = matchedOriginalIndexes.size == question.pairs.size && question.pairs.isNotEmpty()
        if (allMatched) {
            showExplanation = true
        }
        onMatchCompleted(allMatched)
    }

    val selectedLeftOriginal = (pendingSelection as? PendingSelection.Left)?.originalIndex
    val selectedRightOriginal = (pendingSelection as? PendingSelection.Right)?.originalIndex

    Card(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question.content,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = LessonFlowColors.TextPrimary
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Cột A",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = LessonFlowColors.TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Cột B",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = LessonFlowColors.TextSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    leftItems.forEachIndexed { index, item ->
                        val isMatched = matchedOriginalIndexes.containsKey(item.originalIndex)
                        val isSelected = selectedLeftOriginal == item.originalIndex
                        val isError = errorHighlight?.leftOriginal == item.originalIndex
                        MatchingOptionItem(
                            text = item.text,
                            index = index,
                            enabled = !isMatched && !interactionLocked,
                            visualState = when {
                                isError -> VisualState.Error
                                isMatched -> VisualState.Correct
                                isSelected -> VisualState.Selected
                                else -> VisualState.Default
                            },
                            onClick = {
                                if (interactionLocked || isMatched) return@MatchingOptionItem
                                when (val currentSelection = pendingSelection) {
                                    null -> pendingSelection = PendingSelection.Left(item.originalIndex)
                                    is PendingSelection.Left -> {
                                        pendingSelection = if (currentSelection.originalIndex == item.originalIndex) {
                                            null
                                        } else {
                                            PendingSelection.Left(item.originalIndex)
                                        }
                                    }
                                    is PendingSelection.Right -> {
                                        if (currentSelection.originalIndex == item.originalIndex) {
                                            confirmMatch(
                                                originalIndex = item.originalIndex,
                                                matchedOriginalIndexes = matchedOriginalIndexes,
                                                onMatchFeedback = onMatchFeedback
                                            )
                                            pendingSelection = null
                                        } else {
                                            handleMismatch(
                                                shakeOffset = shakeOffset,
                                                coroutineScope = coroutineScope,
                                                onInteractionLocked = { interactionLocked = it },
                                                onErrorHighlight = { highlight ->
                                                    errorHighlight = highlight
                                                },
                                                onMatchFeedback = onMatchFeedback,
                                                leftOriginal = item.originalIndex,
                                                rightOriginal = currentSelection.originalIndex
                                            )
                                            pendingSelection = null
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rightItems.forEachIndexed { index, item ->
                        val isMatched = matchedOriginalIndexes.containsKey(item.originalIndex)
                        val isSelected = selectedRightOriginal == item.originalIndex
                        val isError = errorHighlight?.rightOriginal == item.originalIndex
                        MatchingOptionItem(
                            text = item.text,
                            index = index,
                            enabled = !isMatched && !interactionLocked,
                            visualState = when {
                                isError -> VisualState.Error
                                isMatched -> VisualState.Correct
                                isSelected -> VisualState.Selected
                                else -> VisualState.Default
                            },
                            onClick = {
                                if (interactionLocked || isMatched) return@MatchingOptionItem
                                when (val currentSelection = pendingSelection) {
                                    null -> pendingSelection = PendingSelection.Right(item.originalIndex)
                                    is PendingSelection.Right -> {
                                        pendingSelection = if (currentSelection.originalIndex == item.originalIndex) {
                                            null
                                        } else {
                                            PendingSelection.Right(item.originalIndex)
                                        }
                                    }
                                    is PendingSelection.Left -> {
                                        if (currentSelection.originalIndex == item.originalIndex) {
                                            confirmMatch(
                                                originalIndex = item.originalIndex,
                                                matchedOriginalIndexes = matchedOriginalIndexes,
                                                onMatchFeedback = onMatchFeedback
                                            )
                                            pendingSelection = null
                                        } else {
                                            handleMismatch(
                                                shakeOffset = shakeOffset,
                                                coroutineScope = coroutineScope,
                                                onInteractionLocked = { interactionLocked = it },
                                                onErrorHighlight = { highlight ->
                                                    errorHighlight = highlight
                                                },
                                                onMatchFeedback = onMatchFeedback,
                                                leftOriginal = currentSelection.originalIndex,
                                                rightOriginal = item.originalIndex
                                            )
                                            pendingSelection = null
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showExplanation, enter = fadeIn(tween(durationMillis = 300))) {
                Text(
                    text = question.explanation,
                    color = LessonFlowColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun confirmMatch(
    originalIndex: Int,
    matchedOriginalIndexes: MutableMap<Int, Boolean>,
    onMatchFeedback: (Boolean) -> Unit
) {
    matchedOriginalIndexes[originalIndex] = true
    onMatchFeedback(true)
}

private fun handleMismatch(
    shakeOffset: Animatable<Float, *>,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onInteractionLocked: (Boolean) -> Unit,
    onErrorHighlight: (ErrorHighlight?) -> Unit,
    onMatchFeedback: (Boolean) -> Unit,
    leftOriginal: Int,
    rightOriginal: Int
) {
    onInteractionLocked(true)
    onErrorHighlight(ErrorHighlight(leftOriginal = leftOriginal, rightOriginal = rightOriginal))
    onMatchFeedback(false)
    coroutineScope.launch {
        val shakeMagnitude = 10f
        val shakeDuration = 50
        val iterations = 3
        repeat(iterations) { iteration ->
            val direction = if (iteration % 2 == 0) 1f else -1f
            shakeOffset.animateTo(
                targetValue = direction * shakeMagnitude,
                animationSpec = tween(durationMillis = shakeDuration)
            )
        }
        shakeOffset.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = shakeDuration))
        delay(180)
        onErrorHighlight(null)
        onInteractionLocked(false)
    }
}

private data class MatchingDisplayItem(
    val originalIndex: Int,
    val text: String
)

private data class ErrorHighlight(
    val leftOriginal: Int,
    val rightOriginal: Int
)

private sealed interface PendingSelection {
    data class Left(val originalIndex: Int) : PendingSelection
    data class Right(val originalIndex: Int) : PendingSelection
}

private enum class VisualState {
    Default,
    Selected,
    Correct,
    Error
}

@Composable
private fun MatchingOptionItem(
    text: String,
    index: Int,
    enabled: Boolean,
    visualState: VisualState,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    var hasAnimatedCorrect by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 60L)
        isVisible = true
    }

    LaunchedEffect(visualState) {
        when (visualState) {
            VisualState.Correct -> {
                if (!hasAnimatedCorrect) {
                    hasAnimatedCorrect = true
                    scale.snapTo(0.95f)
                    scale.animateTo(1f, animationSpec = tween(durationMillis = 200))
                }
            }
            VisualState.Selected -> {
                hasAnimatedCorrect = false
                scale.animateTo(0.97f, animationSpec = tween(durationMillis = 120))
            }
            VisualState.Error, VisualState.Default -> {
                hasAnimatedCorrect = false
                scale.animateTo(1f, animationSpec = tween(durationMillis = 150))
            }
        }
    }

    val targetBorderColor = when (visualState) {
        VisualState.Selected -> LessonFlowColors.PrimaryColor
        VisualState.Correct -> LessonFlowColors.SuccessColor
        VisualState.Error -> LessonFlowColors.ErrorColor
        VisualState.Default -> AppColors.LightGray
    }
    val targetTextColor = when (visualState) {
        VisualState.Selected -> LessonFlowColors.PrimaryColor
        VisualState.Correct -> LessonFlowColors.SuccessColor
        VisualState.Error -> LessonFlowColors.ErrorColor
        VisualState.Default -> LessonFlowColors.TextPrimary
    }

    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = 200),
        label = "matchingBorder"
    )
    val textColor by animateColorAsState(
        targetValue = targetTextColor,
        animationSpec = tween(durationMillis = 200),
        label = "matchingText"
    )

    AnimatedVisibility(visible = isVisible, enter = fadeIn(animationSpec = tween(250))) {
        Card(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = if (visualState == VisualState.Selected || visualState == VisualState.Correct) 6.dp else 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 80.dp)
                .clip(RoundedCornerShape(16.dp))
                .graphicsLayerScale(scale = scale.value)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

private fun Modifier.graphicsLayerScale(scale: Float): Modifier =
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }

private val Blue500 = Color(0xFF3B82F6)
private val Blue50 = Color(0xFFEFF6FF)
private val Blue700 = Color(0xFF1D4ED8)

private val Green500 = Color(0xFF22C55E)
private val Green50 = Color(0xFFF0FDF4)
private val Green700 = Color(0xFF15803D)

private val Red500 = Color(0xFFEF4444)
private val Red50 = Color(0xFFFFEDED)
private val Red700 = Color(0xFFB91C1C)

private val LightGray = Color(0xFFE2E8F0)


