package dev.younesgouyd.apps.music.common.data.room

fun String.toSearchQuery(): String {
    return this.ifEmpty { "%" }
}