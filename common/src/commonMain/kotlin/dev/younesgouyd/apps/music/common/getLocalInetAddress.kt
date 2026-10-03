package dev.younesgouyd.apps.music.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

suspend fun getLocalInetAddress(): Inet4Address? {
    return withContext(Dispatchers.IO) {
        try {
            NetworkInterface.getNetworkInterfaces()
                .asSequence()
                .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                .flatMap { it.inetAddresses.asSequence() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }
}
