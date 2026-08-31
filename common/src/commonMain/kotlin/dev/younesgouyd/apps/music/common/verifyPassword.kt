package dev.younesgouyd.apps.music.common

import io.ktor.http.*
import io.ktor.server.application.*
import kotlin.io.encoding.Base64

fun ApplicationCall.verifyPassword(expectedPassword: String): Boolean {
    val authHeader = request.headers[HttpHeaders.Authorization] ?: return false
    return runCatching {
        val decodedPassword = String(Base64.decode(authHeader), Charsets.UTF_8)
        decodedPassword == expectedPassword
    }.getOrDefault(false)
}