package dev.younesgouyd.apps.music.server.multiplatform.data.room

fun String.toSearchQuery(): String {
    return this.ifBlank { "%" }
}