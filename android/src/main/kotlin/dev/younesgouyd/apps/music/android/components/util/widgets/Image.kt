package dev.younesgouyd.apps.music.android.components.util.widgets

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.ByteArrayInputStream

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun Image(
    modifier: Modifier = Modifier,
    data: ByteArray?,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center
) {
    var loading by remember { mutableStateOf(true) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }

    if (data == null) {
        BrokenImage(modifier)
    } else {
        LaunchedEffect(data) {
            loading = true
            ByteArrayInputStream(data).use {
                image = it.readAllBytes().decodeToImageBitmap()
            }
            loading = false
        }

        when (loading) {
            true -> LoadingImage(modifier)
            false -> {
                image?.let {
                    androidx.compose.foundation.Image(
                        modifier = modifier,
                        bitmap = it,
                        contentDescription = null,
                        contentScale = contentScale,
                        alignment = alignment
                    )
                } ?: BrokenImage(modifier)
            }
        }
    }
}

@Composable
private fun LoadingImage(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
        content = { CircularProgressIndicator() }
    )
}

@Composable
private fun BrokenImage(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = null
        )
    }
}