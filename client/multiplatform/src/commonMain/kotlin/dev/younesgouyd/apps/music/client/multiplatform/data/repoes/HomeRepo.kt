package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.HomeItemType
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.TagId
import dev.younesgouyd.apps.music.common.models.datamodels.home.HomeItems
import dev.younesgouyd.apps.music.common.models.pagination.Offset
import dev.younesgouyd.apps.music.common.models.pagination.Page

class HomeRepo(
    private val server: Server
) {
    suspend fun getPage(
        query: String,
        selectedTypes: Set<HomeItemType>,
        selectedTags: Set<TagId>,
        offset: Offset.Home?
    ): Page<HomeItems, Offset.Home> {
        return server.get(
            Rpc.ToServer.Home.GetPage(
                query = query,
                selectedTypes = selectedTypes,
                selectedTags = selectedTags,
                offset = offset
            )
        )
    }
}