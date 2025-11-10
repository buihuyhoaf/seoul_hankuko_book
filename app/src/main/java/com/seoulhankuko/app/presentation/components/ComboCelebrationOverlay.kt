package com.seoulhankuko.app.presentation.components

import android.media.MediaPlayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seoulhankuko.app.presentation.viewmodel.ComboCelebrationState
import com.seoulhankuko.app.presentation.viewmodel.ComboMilestone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ComboCelebrationOverlay(
    state: ComboCelebrationState,
    modifier: Modifier = Modifier,
    onAnimationFinished: () -> Unit
) {
    val style = remember(state.milestone) { state.milestone.toStyle() }
    val context = LocalContext.current
    val iconScale = remember { Animatable(0.7f) }
    val haloRotationTransition = rememberInfiniteTransition(label = "combo_halo_rotation")
    val haloRotation by haloRotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (state.milestone == ComboMilestone.GOLD) 360f else 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (style.durationMillis * 0.9f).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "combo_halo_rotation_value"
    )
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(state.triggeredAt) {
        iconScale.snapTo(0.7f)
        iconScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = 160f
            )
        )

        mediaPlayer?.release()
        mediaPlayer = null
        val resId = context.resources.getIdentifier(style.soundResourceName, "raw", context.packageName)
        if (resId != 0) {
            mediaPlayer = MediaPlayer.create(context, resId)?.apply {
                setOnCompletionListener { player ->
                    player.release()
                    mediaPlayer = null
                }
                start()
            }
        }

        delay(style.durationMillis)
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
            mediaPlayer = null
        }
        onAnimationFinished()
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.triggeredAt) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            if (change.pressed) {
                                change.consume()
                            }
                        }
                    }
                }
            }
            .background(Color.Black.copy(alpha = 0.55f))
    ) {
        ConfettiShower(
            colors = style.confettiPalette,
            durationMillis = style.durationMillis.toInt(),
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier
                        .size(style.haloSize)
                        .graphicsLayer { rotationZ = haloRotation }
                        .blur(if (state.milestone == ComboMilestone.GOLD) 28.dp else 22.dp),
                    shape = CircleShape,
                    color = Color.Transparent,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(colors = style.haloColors)
                            )
                    )
                }

                Surface(
                    modifier = Modifier
                        .size(style.haloSize * 0.68f)
                        .clip(CircleShape)
                        .shadow(elevation = 10.dp, shape = CircleShape),
                    color = Color.White.copy(alpha = 0.08f)
                ) {}

                Surface(
                    modifier = Modifier
                        .size(style.iconContainerSize)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (state.milestone == ComboMilestone.GOLD) 0.18f else 0.12f))
                        .graphicsLayer {
                            rotationZ = if (state.milestone == ComboMilestone.GOLD) 6f else -4f
                        },
                    color = Color.Transparent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = style.icon,
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = iconScale.value
                                    scaleY = iconScale.value
                                },
                            fontSize = style.iconSize,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (state.milestone == ComboMilestone.GOLD) {
                    GlintBurst(modifier = Modifier.align(Alignment.Center))
                }
            }

            Text(
                text = style.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = style.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class ComboCelebrationStyle(
    val icon: String,
    val iconSize: TextUnit,
    val iconContainerSize: Dp,
    val haloSize: Dp,
    val haloColors: List<Color>,
    val confettiPalette: List<Color>,
    val title: String,
    val subtitle: String,
    val durationMillis: Long,
    val soundResourceName: String
)

private fun ComboMilestone.toStyle(): ComboCelebrationStyle = when (this) {
    ComboMilestone.SILVER -> ComboCelebrationStyle(
        icon = "🌟",
        iconSize = 56.sp,
        iconContainerSize = 110.dp,
        haloSize = 240.dp,
        haloColors = listOf(
            Color(0x66FFFFFF),
            Color(0x33B0BEC5),
            Color(0x11FFFFFF)
        ),
        confettiPalette = listOf(
            Color(0xFFB0BEC5),
            Color(0xFF90CAF9),
            Color(0xFFECEFF1),
            Color(0xFFFFFFFF)
        ),
        title = "🔥 5 câu đúng liên tiếp!",
        subtitle = "Giỏi lắm, bạn đang rất tập trung!",
        durationMillis = 1500L,
        soundResourceName = "combo_silver"
    )
    ComboMilestone.GOLD -> ComboCelebrationStyle(
        icon = "🏅",
        iconSize = 64.sp,
        iconContainerSize = 124.dp,
        haloSize = 280.dp,
        haloColors = listOf(
            Color(0x80FFF59D),
            Color(0x4DFFC107),
            Color(0x1AFFFDE7)
        ),
        confettiPalette = listOf(
            Color(0xFFFFD54F),
            Color(0xFFFFB300),
            Color(0xFFFF8A65),
            Color(0xFFFFF59D),
            Color(0xFFFF80AB)
        ),
        title = "🏅 10 câu đúng liên tiếp!",
        subtitle = "Bạn đang bất khả chiến bại!",
        durationMillis = 2100L,
        soundResourceName = "combo_gold"
    )
}

private data class ConfettiParticle(
    val baseX: Float,
    val amplitudePx: Float,
    val widthPx: Float,
    val heightPx: Float,
    val rotationSpeed: Float,
    val delay: Float,
    val color: Color
)

@Composable
private fun ConfettiShower(
    colors: List<Color>,
    durationMillis: Int,
    modifier: Modifier = Modifier,
    particleCount: Int = 55
) {
    val density = LocalDensity.current
    val specs = remember(colors, particleCount) {
        val random = Random(System.currentTimeMillis())
        List(particleCount) {
            ConfettiParticle(
                baseX = random.nextFloat(),
                amplitudePx = with(density) { random.nextInt(20, 80).dp.toPx() },
                widthPx = with(density) { random.nextInt(6, 14).dp.toPx() },
                heightPx = with(density) { random.nextInt(16, 42).dp.toPx() },
                rotationSpeed = random.nextFloat().coerceAtLeast(0.6f),
                delay = random.nextFloat(),
                color = colors[random.nextInt(colors.size)]
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "combo_confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "combo_confetti_progress"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        specs.forEach { spec ->
            val pathProgress = (progress + spec.delay) % 1f
            val oscillation = (pathProgress + spec.delay).toDouble()
            val horizontalOffset = (sin(oscillation * PI * 2.0) * spec.amplitudePx).toFloat()
            val x = (spec.baseX * width + horizontalOffset).coerceIn(0f, width)
            val y = height * pathProgress
            val topLeft = Offset(x, y - spec.heightPx)
            val pivot = topLeft + Offset(spec.widthPx / 2f, spec.heightPx / 2f)
            rotate(spec.rotationSpeed * 360f * pathProgress, pivot) {
                drawRoundRect(
                    color = spec.color,
                    topLeft = topLeft,
                    size = Size(spec.widthPx, spec.heightPx),
                    cornerRadius = CornerRadius(spec.widthPx / 2f, spec.widthPx / 2f)
                )
            }
        }
    }
}

@Composable
private fun GlintBurst(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "combo_glint")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "combo_glint_alpha"
    )

    Canvas(
        modifier = modifier
            .size(220.dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radii = listOf(0.22f, 0.36f, 0.5f)
        radii.forEachIndexed { index, factor ->
            drawCircle(
                color = Color.White.copy(alpha = alpha * (0.6f - index * 0.15f)),
                radius = size.minDimension * factor
            )
        }
    }
}
