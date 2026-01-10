package dev.younesgouyd.apps.music.client.components.util

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
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
expect fun SystemFolderPicker(
    onFolderChosen: (Uri) -> Unit,
    onCancelled: () -> Unit
)

@Composable
expect fun SystemFilePicker(
    onFileChosen: (Uri) -> Unit,
    onCancelled: () -> Unit
)

@Composable
fun AdaptiveUi(
    wide: @Composable () -> Unit,
    compact: @Composable () -> Unit,
    onStateChange: ((WindowSizeClass) -> Unit)? = null
) {
    val latestOnStateChange by rememberUpdatedState(onStateChange)
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current

    val windowSizeClass = remember(windowInfo.containerSize.width, density) {
        with(density) {
            if (windowInfo.containerSize.width.toDp() < 1000.dp) WindowSizeClass.Compact
            else WindowSizeClass.Wide
        }
    }

    LaunchedEffect(windowSizeClass) {
        latestOnStateChange?.invoke(windowSizeClass)
    }

    when (windowSizeClass) {
        WindowSizeClass.Compact -> compact()
        WindowSizeClass.Wide -> wide()
    }
}

enum class WindowSizeClass { Wide, Compact }

fun <T> linearAnimation(duration: Duration): TweenSpec<T> {
    return tween(
        durationMillis = duration.inWholeMilliseconds.toInt(), //TODO
        easing = LinearEasing
    )
}

fun Duration?.formatted(): String {
    if (this == null) {
        return "??:??"
    }
    return this.toComponents { hours: Long, minutes: Int, seconds: Int, _: Int ->
        StringBuilder()
            .append(hours.toString().padStart(2, '0'))
            .append(":")
            .append(minutes.toString().padStart(2, '0'))
            .append(":")
            .append(seconds.toString().padStart(2, '0'))
            .toString()
    }
}

@Composable
fun PlaybackSlider(
    modifier: Modifier = Modifier,
    duration: Duration?,
    currentPosition: Duration,
    enabled: Boolean,
    onSeek: (Duration) -> Unit,
) {
    var isInteracting by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val sliderValue = if (isInteracting) sliderPosition else currentPosition.inWholeMilliseconds.toFloat()

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
            valueRange = 0f..duration.inWholeMilliseconds.toFloat(),
            onValueChange = { newValue ->
                isInteracting = true
                sliderPosition = newValue
            },
            onValueChangeFinished = {
                isInteracting = false
                onSeek(sliderPosition.toLong().milliseconds)
            }
        )
    }
}