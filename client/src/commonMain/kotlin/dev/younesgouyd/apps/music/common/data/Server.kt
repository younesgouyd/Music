package dev.younesgouyd.apps.music.common.data

import dev.younesgouyd.apps.music.common.Inspection
import dev.younesgouyd.apps.music.common.json
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

class Server(
    private val serverAddress: StateFlow<String?>
) {
    private val client = HttpClient(CIO) {
        install(Logging) { level = LogLevel.ALL }
        install(ContentNegotiation) {
            json(json)
        }
        install(SSE)
        install(HttpTimeout) {
            this.requestTimeoutMillis = 180_000
        }
    }

    suspend fun inspect(url: String): Inspection.Webpage {
        return client.get("${getAddress()}/inspect") {
            parameter("url", url)
        }.body<Inspection.Webpage>()
    }

    fun download(url: String): Flow<String> {
        return flow {
            client.sse(
                urlString = "${getAddress()}/download",
                request = { parameter("url", url) }
            ) {
                incoming.collect {
                    this@flow.emit(it.event!!)
                }
            }
        }
    }

    suspend fun getResult(): ByteArray {
        return client.get("${getAddress()}/getResult").bodyAsBytes()
    }

    private fun getAddress(): String {
        val settingsAddress = serverAddress.value
        return if (settingsAddress.isNullOrEmpty()) TODO() else settingsAddress
    }
}