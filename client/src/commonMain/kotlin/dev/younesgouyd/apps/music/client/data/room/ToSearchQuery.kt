package dev.younesgouyd.apps.music.client.data.room

fun String.toSearchQuery(): String {
    return this.ifBlank { "%" }
}