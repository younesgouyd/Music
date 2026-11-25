package dev.younesgouyd.apps.music.common.models.pagination

import dev.younesgouyd.apps.music.common.models.HomeItemType
import kotlinx.serialization.Serializable

@Serializable
sealed class Offset {
    @Serializable
    data class Id<T>(val value: T?): Offset() {
        companion object {
            fun <T> initial(): Id<T> = Id(null)
        }
    }

    @Serializable
    data class Index(val value: Int): Offset() {
        companion object {
            fun initial(): Index = Index(0)
        }
    }

    @Serializable
    data class Home(
        val creationDatetime: Long?,
        val itemType: HomeItemType?,
        val itemId: Long?
    ) : Offset() {
        companion object {
            fun initial(): Home = Home(
                creationDatetime = null,
                itemType = null,
                itemId = null
            )
        }
    }
}