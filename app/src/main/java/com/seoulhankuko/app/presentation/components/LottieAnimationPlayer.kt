package com.seoulhankuko.app.presentation.components

import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.LottieDynamicProperties
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import java.io.File

sealed interface LottieAnimationResource {
    fun asSpec(): LottieCompositionSpec

    data class Raw(@RawRes val resId: Int) : LottieAnimationResource {
        override fun asSpec(): LottieCompositionSpec = LottieCompositionSpec.RawRes(resId)
    }
}

@Composable
fun SeoulLottieAnimation(
    resource: LottieAnimationResource,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
    isPlaying: Boolean = true,
    speed: Float = 1f,
    restartOnPlay: Boolean = false,
    clipSpec: LottieClipSpec? = null,
    clipToCompositionBounds: Boolean = false,
    outlineMasksAndMattes: Boolean = false,
    applyOpacityToLayers: Boolean = false,
    renderMode: RenderMode = RenderMode.AUTOMATIC,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    dynamicProperties: LottieDynamicProperties? = null
) {
    val composition by rememberLottieComposition(resource.asSpec())

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        isPlaying = isPlaying,
        speed = speed,
        restartOnPlay = restartOnPlay,
        clipSpec = clipSpec
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
        clipToCompositionBounds = clipToCompositionBounds,
        outlineMasksAndMattes = outlineMasksAndMattes,
        applyOpacityToLayers = applyOpacityToLayers,
        renderMode = renderMode,
        contentScale = contentScale,
        alignment = alignment,
        dynamicProperties = dynamicProperties
    )
}

