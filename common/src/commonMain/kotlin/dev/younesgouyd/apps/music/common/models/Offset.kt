package dev.younesgouyd.apps.music.common.models

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
}