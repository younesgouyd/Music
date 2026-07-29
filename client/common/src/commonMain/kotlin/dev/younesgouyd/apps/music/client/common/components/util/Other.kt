package dev.younesgouyd.apps.music.client.common.components.util

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.common.SpotifyArtistId
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

typealias Uri = String

enum class DbOrder { Ascending, Descending }

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
expect fun getWindowSizeClass(): WindowWidthSizeClass

@Composable
fun AdaptiveUi(
    wide: @Composable () -> Unit,
    compact: @Composable () -> Unit,
    onStateChange: ((WindowWidthSizeClass) -> Unit)? = null
) {
    val latestOnStateChange by rememberUpdatedState(onStateChange)
    val windowSizeClass = getWindowSizeClass()

    LaunchedEffect(windowSizeClass) {
        latestOnStateChange?.invoke(windowSizeClass)
    }

    when (windowSizeClass) {
        WindowWidthSizeClass.Compact -> compact()
        else -> wide()
    }
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

@Composable
fun Artists(
    modifier: Modifier = Modifier,
    names: List<String>
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(names) { name ->
            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, null)
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun Album(
    modifier: Modifier = Modifier,
    name: String
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Album, null)
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun Artists(
    modifier: Modifier = Modifier,
    artists: List<Pair<SpotifyArtistId, String>>,
    onArtistClick: (SpotifyArtistId) -> Unit
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(artists) { (id, name) ->
            TextButton(
                onClick = { onArtistClick(id) },
                content = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, null)
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun Album(
    modifier: Modifier = Modifier,
    name: String,
    onClick: () -> Unit
) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
        content = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Album, null)
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    )
}

@Composable
fun Duration(
    modifier: Modifier = Modifier,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Timer, null)
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

data class HeaderAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun ItemDetailsHeaderWide(
    modifier: Modifier = Modifier,
    title: String,
    image: File?,
    itemAttributes: (@Composable ColumnScope.() -> Unit)? = null,
    mainAction: HeaderAction? = null,
    actions: List<HeaderAction> = emptyList()
) {
    Surface(modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp, alignment = Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.weight(1f).aspectRatio(1f, true),
                file = image
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center
                )
                if (itemAttributes != null) { itemAttributes() }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (mainAction != null) {
                        item {
                            Button(mainAction.onClick) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(mainAction.icon, null)
                                    Text(text = mainAction.label, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                    items(actions) { (label, icon, onClick) ->
                        OutlinedButton(
                            content = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(icon, null)
                                    Text(text = label, style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            onClick = onClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemDetailsHeaderCompact(
    modifier: Modifier = Modifier,
    title: String,
    image: File?,
    itemAttributes: (@Composable ColumnScope.() -> Unit)? = null,
    mainAction: HeaderAction? = null,
    actions: List<HeaderAction> = emptyList()
) {
    Surface(modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Image(
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
                file = image
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
            if (itemAttributes != null) { itemAttributes() }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mainAction != null) {
                    item {
                        IconButton(
                            onClick = mainAction.onClick
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(),
                                imageVector = mainAction.icon,
                                contentDescription = mainAction.label,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                items(actions) { (label, icon, onClick) ->
                    OutlinedButton(
                        content = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, null)
                                Text(text = label, style = MaterialTheme.typography.labelMedium)
                            }
                        },
                        onClick = onClick
                    )
                }
            }
        }
    }
}