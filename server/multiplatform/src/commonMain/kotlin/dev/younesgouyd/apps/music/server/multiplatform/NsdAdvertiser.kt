package dev.younesgouyd.apps.music.server.multiplatform

import dev.younesgouyd.apps.music.common.getLocalInetAddress
import dev.younesgouyd.apps.music.common.nsdServiceNameToServiceType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener

class NsdAdvertiser {
    companion object {
        const val PORT = 8443
    }
    private val _status: MutableStateFlow<NsdStatus> = MutableStateFlow(NsdStatus.Idle)
    private val logger = KotlinLogging.logger {}
    @Volatile private var jmDns: JmDNS? = null
    @Volatile private var serviceInfo: ServiceInfo? = null
    @Volatile private var listener: ServiceListener? = null
    @Volatile private var serviceType: String? = null

    val status = _status.asStateFlow()

    suspend fun registerService(name: String) {
        logger.debug { "--> ::registerService(name: $name)" }
        withContext(Dispatchers.IO) {
            unregisterService()
            try {
                val localInetAddress = runCatching { getLocalInetAddress() }.getOrNull()
                if (localInetAddress == null) {
                    logger.error { "... ::registerService | Could not determine local InetAddress" }
                    _status.value = NsdStatus.RegistrationFailed
                    return@withContext
                }
                logger.debug { "... ::registerService | ip: ${localInetAddress.hostAddress}" }
                jmDns = JmDNS.create(localInetAddress)
                listener = ServiceListenerImpl(jmDns!!, name, _status)
                serviceType = nsdServiceNameToServiceType(name)
                logger.debug { "... ::registerService | serviceType: $serviceType" }
                jmDns!!.addServiceListener(serviceType, listener)
                serviceInfo = ServiceInfo.create(serviceType, name, PORT, "")
                jmDns!!.registerService(serviceInfo)
                logger.debug { "... ::registerService | operation initiated" }
            } catch (e: Exception) {
                logger.error(e) { "... ::registerService | operation failed" }
                _status.value = NsdStatus.RegistrationFailed
            }
        }
    }

    suspend fun unregisterService() {
        withContext(Dispatchers.IO) {
            try {
                val jmDns = jmDns
                if (jmDns != null) {
                    jmDns.unregisterAllServices()
                    if (serviceType != null && listener != null) {
                        jmDns.removeServiceListener(serviceType, listener)
                    }
                    jmDns.close()
                    this@NsdAdvertiser.jmDns = null
                    serviceType = null
                    serviceInfo = null
                    listener = null
                    _status.value = NsdStatus.UnregistrationSuccessful
                    logger.debug { "... ::unregisterService | operation initiated" }
                }
            } catch (e: Exception) {
                logger.error(e) { "... ::unregisterService | operation failed" }
                _status.value = NsdStatus.UnregistrationFailed
            }
        }
    }

    private class ServiceListenerImpl(
        private val jmDns: JmDNS,
        private val name: String,
        private val status: MutableStateFlow<NsdStatus>
    ) : ServiceListener {
        private val logger = KotlinLogging.logger(ServiceListenerImpl::class.qualifiedName!!)

        override fun serviceAdded(event: ServiceEvent) {
            if (event.name == name) {
                logger.info { "service added" }
                jmDns.requestServiceInfo(event.type, event.name)
            }
        }

        override fun serviceResolved(event: ServiceEvent) {
            if (event.name == name) {
                logger.info { "service resolved" }
                status.value = NsdStatus.RegistrationSuccessful
            }
        }

        override fun serviceRemoved(event: ServiceEvent) {
            if (event.name == name) {
                logger.info { "service removed" }
                status.value = NsdStatus.UnregistrationSuccessful
            }
        }
    }
}