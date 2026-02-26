package dev.younesgouyd.apps.music.client.app.multiplatform.util

sealed class Offset {
    data class Id<T>(val value: T?): Offset() {
        companion object {
            fun <T> initial(): Id<T> = Id(null)
        }
    }

    data class Index(val value: Int): Offset() {
        companion object {
            fun initial(): Index = Index(0)
        }
    }
}