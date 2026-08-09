package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.FolderId
import kotlinx.serialization.Serializable

@Serializable
data class DeleteFolderRpc(
    val id: FolderId
) : Rpc()
