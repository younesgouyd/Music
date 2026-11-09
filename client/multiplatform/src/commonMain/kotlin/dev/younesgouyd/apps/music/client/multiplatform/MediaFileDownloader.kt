package dev.younesgouyd.apps.music.client.multiplatform

import dev.younesgouyd.apps.music.common.models.MediaFileId
import java.io.File

interface MediaFileDownloader {
    suspend fun download(id: MediaFileId): File
}