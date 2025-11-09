package com.seoulhankuko.app.presentation.screens

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seoulhankuko.app.presentation.utils.LessonFlowColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.runtime.State

@Composable
fun LessonResultScreen(
    totalTime: Long,
    experienceGained: Int,
    onContinue: () -> Unit
) {
    val evaluation = remember(experienceGained) { getEvaluation(experienceGained) }
    val (minutes, seconds) = remember(totalTime) {
        val totalSeconds = (totalTime / 1000L).coerceAtLeast(0)
        Pair((totalSeconds / 60).toInt(), (totalSeconds % 60).toInt())
    }

    val context = LocalContext.current

    DisposableEffect(key1 = Unit) {
        val resId = context.resources.getIdentifier("finish", "raw", context.packageName)
        val player = if (resId != 0) MediaPlayer.create(context, resId) else null
        player?.start()

        onDispose {
            player?.let {
                try {
                    if (it.isPlaying) {
                        it.stop()
                    }
                } catch (_: Exception) {
                } finally {
                    it.release()
                }
            }
        }
    }

    var titleVisible by remember { mutableStateOf(false) }
    var timeVisible by remember { mutableStateOf(false) }
    var xpVisible by remember { mutableStateOf(false) }
    var evaluationVisible by remember { mutableStateOf(false) }
    var progressVisible by remember { mutableStateOf(false) }
    var actionsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        titleVisible = true
        delay(100)
        timeVisible = true
        delay(100)
        xpVisible = true
        delay(100)
        evaluationVisible = true
        delay(100)
        progressVisible = true
        delay(120)
        actionsVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(24.dp)
    ) {
        ConfettiBackground(modifier = Modifier.fillMaxSize())

        Card(
            modifier = Modifier.align(Alignment.Center),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CelebrationHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )

                AnimatedVisibility(
                    visible = titleVisible,
                    enter = fadeIn(animationSpec = tween(delayMillis = 100, durationMillis = 350)) +
                        slideInVertically(
                            initialOffsetY = { it / 3 },
                            animationSpec = tween(delayMillis = 100, durationMillis = 350)
                        )
                ) {
                    Text(
                        text = "Hoàn thành bài học!",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = LessonFlowColors.TextPrimary
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                AnimatedVisibility(
                    visible = timeVisible,
                    enter = fadeIn(animationSpec = tween(delayMillis = 200, durationMillis = 350))
                ) {
                    Text(
                        text = "Bạn đã hoàn thành trong $minutes phút ${seconds.toString().padStart(2, '0')} giây",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = LessonFlowColors.TextSecondary
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                AnimatedVisibility(
                    visible = xpVisible,
                    enter = fadeIn(animationSpec = tween(delayMillis = 200, durationMillis = 350))
                ) {
                    AnimatedXpCounter(
                        targetXp = experienceGained,
                        highlightColor = evaluation.color,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AnimatedVisibility(
                    visible = evaluationVisible,
                    enter = fadeIn(animationSpec = tween(delayMillis = 250, durationMillis = 350))
                ) {
                    EvaluationSection(
                        evaluation = evaluation,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AnimatedVisibility(
                    visible = progressVisible,
                    enter = fadeIn(animationSpec = tween(delayMillis = 300, durationMillis = 500))
                ) {
                    ProgressSection(
                        color = evaluation.color,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AnimatedVisibility(
                    visible = actionsVisible,
                    enter = scaleIn(
                        initialScale = 1.1f,
                        animationSpec = tween(durationMillis = 400, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
                    ) + fadeIn(animationSpec = tween(durationMillis = 300))
                ) {
                    ActionButtonsRow(
                        onContinue = onContinue,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun CelebrationHeader(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val scale = remember { Animatable(0.6f) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            coroutineScope.launch {
                scale.snapTo(0.6f)
                scale.animateTo(
                    targetValue = 1.15f,
                    animationSpec = tween(durationMillis = 600, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f))
                )
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 280, easing = CubicBezierEasing(0.16f, 0f, 0.2f, 1f))
                )
            }
        }

        Text(
            text = "🎉",
            fontSize = 72.sp,
            modifier = Modifier.scale(scale.value),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AnimatedXpCounter(
    targetXp: Int,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedXp by animateIntAsState(
        targetValue = targetXp,
        animationSpec = tween(durationMillis = 1500),
        label = "xpCounter"
    )

    var triggerBounce by remember { mutableStateOf(false) }
    val scaleBounce by animateFloatAsState(
        targetValue = if (triggerBounce) 1.08f else 1f,
        animationSpec = tween(durationMillis = 220, easing = CubicBezierEasing(0.22f, 1.4f, 0.36f, 1f)),
        label = "xpScale"
    )

    LaunchedEffect(animatedXp, targetXp) {
        if (animatedXp == targetXp && targetXp > 0) {
            triggerBounce = true
            delay(220)
            triggerBounce = false
        }
    }

    Text(
        text = "⭐ +$animatedXp XP",
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            color = highlightColor,
            letterSpacing = 0.5.sp
        ),
        modifier = modifier.scale(scaleBounce),
        textAlign = TextAlign.Center
    )
}

@Composable
fun EvaluationSection(
    evaluation: Evaluation,
    modifier: Modifier = Modifier
) {
    Text(
        text = "${evaluation.emoji} ${evaluation.text}",
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = evaluation.color
        ),
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ProgressSection(
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)),
        label = "progress"
    )

    LinearProgressIndicator(
        progress = { progress },
        color = color,
        trackColor = color.copy(alpha = 0.15f),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .height(12.dp)
    )
}

@Composable
fun ActionButtonsRow(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Button(
            onClick = onContinue,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LessonFlowColors.PrimaryColor,
                contentColor = Color.White
            )
        ) {
            Text(text = "➡️ Tiếp tục")
        }
    }
}

@Composable
fun ConfettiBackground(
    modifier: Modifier = Modifier,
    particleCountRange: IntRange = 15..25,
    particleColors: List<Color> = listOf(
        Color(0xFF3B82F6),
        Color(0xFFF97316),
        Color(0xFF22C55E),
        Color(0xFF6366F1),
        Color(0xFFFACC15)
    )
) {
    val actualCount = remember(particleCountRange) {
        Random.nextInt(particleCountRange.first, particleCountRange.last + 1)
    }
    val particles = remember(actualCount, particleColors) {
        List(actualCount) { index ->
            ConfettiParticle(
                id = index,
                xFraction = Random.nextFloat(),
                size = Random.nextInt(6, 12).dp,
                color = particleColors.random(),
                durationMillis = Random.nextInt(1600, 2600),
                delayMillis = Random.nextInt(0, 600)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progresses: List<State<Float>> = particles.map { particle: ConfettiParticle ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable<Float>(
                animation = tween(
                    durationMillis = particle.durationMillis,
                    delayMillis = particle.delayMillis,
                    easing = CubicBezierEasing(0.33f, 0f, 0.67f, 1f)
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "confetti-${particle.id}"
        )
    }

    Canvas(modifier = modifier) {
        particles.forEachIndexed { index, particle ->
            val progress = progresses[index].value
            val y = size.height * progress
            val x = size.width * particle.xFraction
            val wobble = ((particle.size.toPx() * 0.6f) * sin(progress * 6f)).toFloat()

            drawCircle(
                color = particle.color,
                radius = particle.size.toPx() / 2f,
                center = Offset(x + wobble, y)
            )
        }
    }
}

@Stable
private data class ConfettiParticle(
    val id: Int,
    val xFraction: Float,
    val size: Dp,
    val color: Color,
    val durationMillis: Int,
    val delayMillis: Int
)

data class Evaluation(
    val text: String,
    val emoji: String,
    val color: Color
)

fun getEvaluation(xp: Int): Evaluation {
    return when {
        xp >= 80 -> Evaluation("Tuyệt vời!", "🏅", Color(0xFF22C55E))
        xp >= 50 -> Evaluation("Khá tốt!", "💪", Color(0xFF3B82F6))
        else -> Evaluation("Cố gắng thêm nhé!", "🌱", Color(0xFFF97316))
    }
}
