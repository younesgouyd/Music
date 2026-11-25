package dev.younesgouyd.apps.music.client.multiplatform

import dev.younesgouyd.apps.music.common.models.MediaFileId
import java.io.File

interface MediaFileLoader {
    suspend fun load(id: MediaFileId): File
}