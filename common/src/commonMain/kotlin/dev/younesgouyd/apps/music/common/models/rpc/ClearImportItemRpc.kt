package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import kotlinx.serialization.Serializable

@Serializable
data class ClearImportItemRpc(
    val id: ImportSessionItemId
) : Rpc()
