package com.seoulhankuko.app.presentation.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seoulhankuko.app.R
import com.seoulhankuko.app.presentation.components.LottieAnimationResource
import com.seoulhankuko.app.presentation.components.SeoulLottieAnimation

@Composable
fun StreakCelebrationScreen(
    streakDays: Int,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startAnimations by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimations = true
    }

    val streakCount by animateIntAsState(
        targetValue = if (startAnimations) streakDays else 0,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 900,
            easing = FastOutSlowInEasing
        ),
        label = "streakCount"
    )

    val appearAlpha by animateFloatAsState(
        targetValue = if (startAnimations) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 420,
            easing = FastOutSlowInEasing
        ),
        label = "streakAlpha"
    )
    val baseScale by animateFloatAsState(
        targetValue = if (startAnimations) 1f else 0.7f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.7f,
            stiffness = 220f
        ),
        label = "streakBaseScale"
    )

    val pulseTransition = rememberInfiniteTransition(label = "streakPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "streakPulseScale"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "streakPulseAlpha"
    )

    val displayScale = if (startAnimations) baseScale * pulseScale else 0.7f
    val displayAlpha = if (startAnimations) appearAlpha * pulseAlpha else 0f

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
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            SeoulLottieAnimation(
                resource = LottieAnimationResource.Raw(R.raw.fire_animation),
                modifier = Modifier.size(220.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = streakCount.toString(),
                color = Color(0xFFFFF8E1),
                fontSize = 96.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer(
                    scaleX = displayScale,
                    scaleY = displayScale,
                    alpha = displayAlpha
                )
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        Button(
            onClick = onContinueClick,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E3A8A),
                contentColor = Color.White
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth()
                .height(58.dp)
        ) {
            Text(
                text = "Tiếp tục",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
