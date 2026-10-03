package dev.younesgouyd.apps.music.common

fun nsdServiceNameToServiceType(serviceName: String): String {
    return "_" + serviceName.replace("-", "").take(15) + "._tcp.local."
}