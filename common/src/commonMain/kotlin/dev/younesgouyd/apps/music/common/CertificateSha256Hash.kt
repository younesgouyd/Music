package dev.younesgouyd.apps.music.common

import java.security.MessageDigest
import java.security.cert.X509Certificate

fun X509Certificate.sha256Hash(): String {
    val hash = MessageDigest.getInstance("SHA-256").digest(encoded)
    return hash.joinToString(":") { "%02X".format(it) }
}