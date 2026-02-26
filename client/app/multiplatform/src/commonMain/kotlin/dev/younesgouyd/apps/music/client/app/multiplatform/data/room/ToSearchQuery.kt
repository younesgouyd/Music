package dev.younesgouyd.apps.music.client.app.multiplatform.data.room

fun String.toSearchQuery(): String {
    return this.ifBlank { "%" }
}