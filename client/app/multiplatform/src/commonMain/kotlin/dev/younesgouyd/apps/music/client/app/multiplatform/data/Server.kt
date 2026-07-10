package dev.younesgouyd.apps.music.client.app.multiplatform.data

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
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import java.io.InputStream

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
            this.requestTimeoutMillis = 30*60*1000
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

    suspend fun getResult(): Pair<String, InputStream> {
        val response = client.get("${getAddress()}/getResult")
        val stream = response.bodyAsChannel().toInputStream()
        val filename = response.headers[HttpHeaders.ContentDisposition]!!
            .substringAfter("filename=\"")
            .substringBeforeLast("\"")
        return filename to stream
    }

    private fun getAddress(): String {
        val settingsAddress = serverAddress.value
        return if (settingsAddress.isNullOrBlank()) TODO() else settingsAddress
    }

    fun close() {
        client.close()
    }
}
