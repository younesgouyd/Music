package dev.younesgouyd.apps.music.common

import java.security.SecureRandom

fun generatePassword(): String {
    val random = SecureRandom()
    val minAscii = 33
    val maxAscii = 126
    return (1..64)
        .map { (minAscii + random.nextInt(maxAscii - minAscii + 1)).toChar() }
        .joinToString("")
}