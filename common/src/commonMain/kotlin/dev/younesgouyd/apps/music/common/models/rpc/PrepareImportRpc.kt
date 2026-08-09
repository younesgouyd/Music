package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.Inspection
import kotlinx.serialization.Serializable

@Serializable
data class PrepareImportRpc(
    val selected: List<Long>,
    val url: String,
    val inspection: Inspection,
    val destinationFolderId: FolderId
) : Rpc()
