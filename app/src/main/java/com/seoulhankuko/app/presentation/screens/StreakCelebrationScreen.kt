package com.seoulhankuko.app.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun StreakCelebrationScreen(
    streakDays: Int,
    onContinueClick: () -> Unit,
    onExitConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var startAnimations by remember { mutableStateOf(false) }

    BackHandler {
        showConfirmDialog = true
    }

    LaunchedEffect(Unit) {
        startAnimations = true
    }

    val scale by animateFloatAsState(
        targetValue = if (startAnimations) 1f else 0.65f,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = 260f
        ),
        label = "streakScale"
    )

    val glowPulse = rememberInfiniteTransition(label = "glowTransition").animateFloat(
        initialValue = 0.8f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    ).value

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFE082),
                        Color(0xFFFFB74D),
                        Color(0xFFFF7043)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        ConfettiBurst(
            modifier = Modifier.fillMaxSize(),
            enabled = startAnimations
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(220.dp)
                    .drawBehind {
                        val gradient = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFF8E1).copy(alpha = 0.6f),
                                Color(0x00FFF8E1)
                            ),
                            radius = size.minDimension / 1.4f
                        )
                        drawCircle(brush = gradient, radius = size.minDimension / 2f)
                    }
                    .clip(CircleShape)
            ) {
                FlameIcon(
                    modifier = Modifier
                        .size(180.dp)
                        .graphicsLayer(
                            scaleX = scale * glowPulse,
                            scaleY = scale * glowPulse
                        )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = streakDays.toString(),
                color = Color.White,
                fontSize = 96.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onContinueClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E3A8A),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Text(
                    text = "Tiếp tục học",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "Thoát màn hình?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Bạn có muốn thoát? Nếu thoát sẽ mất streak hôm nay.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onExitConfirmed()
                    }
                ) {
                    Text(
                        text = "Thoát",
                        color = Color(0xFFEF5350),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(
                        text = "Ở lại",
                        color = Color(0xFF1E3A8A),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }
}

@Composable
private fun FlameIcon(
    modifier: Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = size.minDimension / 2f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF8E1).copy(alpha = 0.35f),
                    Color.Transparent
                ),
                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                radius = radius
            )
        )

        val flamePath = Path().apply {
            moveTo(centerX, height * 0.05f)
            cubicTo(
                centerX + width * 0.22f,
                height * 0.25f,
                centerX + width * 0.34f,
                height * 0.58f,
                centerX,
                height * 0.95f
            )
            cubicTo(
                centerX - width * 0.34f,
                height * 0.6f,
                centerX - width * 0.2f,
                height * 0.28f,
                centerX,
                height * 0.05f
            )
            close()
        }

        drawPath(
            path = flamePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFF59D),
                    Color(0xFFFFB74D),
                    Color(0xFFFF7043)
                )
            )
        )

        val innerPath = Path().apply {
            moveTo(centerX, height * 0.2f)
            cubicTo(
                centerX + width * 0.14f,
                height * 0.36f,
                centerX + width * 0.12f,
                height * 0.58f,
                centerX,
                height * 0.78f
            )
            cubicTo(
                centerX - width * 0.14f,
                height * 0.58f,
                centerX - width * 0.12f,
                height * 0.36f,
                centerX,
                height * 0.2f
            )
            close()
        }

        drawPath(
            path = innerPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color(0xFFFFE082)
                )
            )
        )
    }
}

@Composable
private fun ConfettiBurst(
    modifier: Modifier = Modifier,
    enabled: Boolean
) {
    if (!enabled) return

    val confettiColors = remember {
        listOf(
            Color(0xFFFFF59D),
            Color(0xFFFFAB91),
            Color(0xFFFFCC80),
            Color(0xFFFFFFFF),
            Color(0xFFFFE082)
        )
    }

    val particles = remember {
        val random = Random(0x1a2b3c4d)
        List(42) {
            StreakConfettiParticle(
                angle = random.nextFloat() * 360f,
                speed = 0.45f + random.nextFloat() * 0.35f,
                size = 6f + random.nextFloat() * 10f,
                delay = random.nextFloat(),
                color = confettiColors[it % confettiColors.size]
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confettiTransition")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiProgress"
    )

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = size.minDimension * 0.75f

        particles.forEach { particle ->
            val normalized = ((progress + particle.delay) % 1f).coerceIn(0f, 1f)
            val distance = maxRadius * normalized * particle.speed
            val angleRad = (particle.angle * PI / 180f).toFloat()
            val x = centerX + cos(angleRad) * distance
            val y = centerY + sin(angleRad) * distance
            val alpha = (1f - normalized).coerceIn(0f, 1f)

            translate(left = x, top = y) {
                drawCircle(
                    color = particle.color.copy(alpha = alpha),
                    radius = particle.size
                )
            }
        }
    }
}

private data class StreakConfettiParticle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val delay: Float,
    val color: Color
)
