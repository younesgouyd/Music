package dev.younesgouyd.apps.music.server.common.data.room

fun String.toSearchQuery(): String {
    return this.ifBlank { "%" }
}