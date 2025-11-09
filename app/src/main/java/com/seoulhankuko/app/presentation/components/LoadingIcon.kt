package com.seoulhankuko.app.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.seoulhankuko.app.R

@Composable
fun SouthKoreaLoadingIcon(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    rotationDurationMillis: Int = 1400
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = rotationDurationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    androidx.compose.foundation.Image(
        painter = painterResource(id = R.drawable.southkorea),
        contentDescription = stringResource(id = R.string.loading),
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation },
        contentScale = ContentScale.Fit
    )
}

