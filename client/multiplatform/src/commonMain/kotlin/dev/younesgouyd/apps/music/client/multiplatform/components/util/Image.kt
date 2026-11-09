package dev.younesgouyd.apps.music.client.multiplatform.components.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import dev.younesgouyd.apps.music.client.multiplatform.ImageLoader
import dev.younesgouyd.apps.music.client.multiplatform.LocalImageLoader
import dev.younesgouyd.apps.music.common.models.MediaFileId

@Composable
fun Image(
    modifier: Modifier = Modifier,
    id: MediaFileId?,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center
) {
    if (id == null) {
        BrokenImage(modifier)
        return
    }
    val loader = LocalImageLoader.current
    val image = produceState<Result<ImageBitmap>>(Result.Loading, id) {
        value = Result.Success(loader.get(ImageLoader.Key.Mfid(id)))
    }

    when (val img = image.value) {
        is Result.Error -> BrokenImage(modifier)
        is Result.Loading -> BrokenImage(modifier)
        is Result.Success<ImageBitmap> -> Image(
            modifier = modifier,
            bitmap = img.value,
            contentDescription = null,
            contentScale = contentScale,
            alignment = alignment
        )
    }
}

@Composable
fun Image(
    modifier: Modifier = Modifier,
    url: String?,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center
) {
    if (url == null) {
        BrokenImage(modifier)
        return
    }
    val loader = LocalImageLoader.current
    val image = produceState<Result<ImageBitmap>>(Result.Loading, url) {
        value = Result.Success(loader.get(ImageLoader.Key.Url(url)))
    }

    when (val img = image.value) {
        is Result.Error -> BrokenImage(modifier)
        is Result.Loading -> BrokenImage(modifier)
        is Result.Success<ImageBitmap> -> Image(
            modifier = modifier,
            bitmap = img.value,
            contentDescription = null,
            contentScale = contentScale,
            alignment = alignment
        )
    }
}

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
            image = data.decodeToImageBitmap()
            loading = false
        }

        when (loading) {
            true -> BrokenImage(modifier)
            false -> {
                image?.let {
                    Image(
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
private fun BrokenImage(
    modifier: Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = null
        )
    }
}

sealed class Result<out T> {
    object Loading : Result<Nothing>()
    object Error : Result<Nothing>()
    class Success<T>(val value: T) : Result<T>()
}
