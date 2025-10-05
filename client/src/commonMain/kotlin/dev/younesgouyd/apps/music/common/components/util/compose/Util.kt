package dev.younesgouyd.apps.music.common.components.util.compose

import androidx.compose.animation.core.*
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

typealias Uri = String

@Composable
expect fun SystemFilePicker(onFolderPicked: (Uri) -> Unit)

@Composable
fun AdaptiveUi(
    wide: @Composable () -> Unit,
    compact: @Composable () -> Unit
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val widthDp = with(density) { windowInfo.containerSize.width.toDp() }

    when {
        widthDp < 840.dp -> compact()
        else -> wide()
    }
}

fun <T> linearAnimation(duration: Duration): TweenSpec<T> {
    return tween(
        durationMillis = duration.inWholeMilliseconds.toInt(), //TODO
        easing = LinearEasing
    )
}

fun Duration?.formatted(): String {
    return this?.toComponents { minutes, seconds, _ ->
        minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0')
    } ?: "??:??"
}

@Composable
fun PlaybackSlider(
    modifier: Modifier = Modifier,
    duration: Duration?,
    animatedPosition: Animatable<Float, AnimationVector1D>,
    enabled: Boolean,
    onSeek: (Duration) -> Unit,
    isInteracting: MutableState<Boolean>
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val sliderValue = if (isInteracting.value) sliderPosition else animatedPosition.value

    if (duration == null) {
        Slider(
            modifier = modifier,
            enabled = enabled,
            value = sliderValue,
            onValueChange = {}
        )
    } else {
        Slider(
            modifier = modifier,
            enabled = enabled,
            value = sliderValue,
            onValueChange = { newValue ->
                isInteracting.value = true
                sliderPosition = newValue
            },
            onValueChangeFinished = {
                isInteracting.value = false
                onSeek((sliderPosition * duration.inWholeMilliseconds.toFloat()).toLong().milliseconds)
            }
        )
    }
}