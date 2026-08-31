package dev.younesgouyd.apps.music.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

suspend fun getLocalIpAddress(): String {
    return withContext(Dispatchers.IO) {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val addresses = networkInterface.inetAddresses
                for (inetAddress in addresses) {
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return@withContext inetAddress.hostAddress
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        ""
    }
}
