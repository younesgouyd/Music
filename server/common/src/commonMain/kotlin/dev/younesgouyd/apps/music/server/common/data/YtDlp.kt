package dev.younesgouyd.apps.music.server.common.data

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
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import java.io.InputStream

class YtDlp(
    var serverAddress: String
) {
    private val client = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                private val binaryContentTypes = setOf(
                    "application/octet-stream",
                    "audio/",
                    "video/",
                    "image/",
                    "application/zip",
                    "application/pdf"
                )
                override fun log(message: String) {
                    if (message.contains("BODY START") && message.contains("Content-Type")) {
                        val contentType = message.substringAfter("Content-Type: ").substringBefore("\n")
                        if (binaryContentTypes.any { contentType.contains(it) }) {
                            return
                        }
                    }
                    println(message)
                }
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(SSE)
        install(HttpTimeout) {
            this.requestTimeoutMillis = 30*60*1000
        }
    }

    suspend fun inspect(url: String): Inspection {
        return client.get("$serverAddress/inspect") {
            parameter("url", url)
        }.body<Inspection>()
    }

    fun download(url: String): Flow<String> {
        return flow {
            client.sse(
                urlString = "$serverAddress/download",
                request = { parameter("url", url) }
            ) {
                incoming.collect {
                    this@flow.emit(it.event!!)
                }
            }
        }
    }

    suspend fun getResult(): Pair<String, InputStream> {
        val response = client.get("$serverAddress/getResult")
        val stream = response.bodyAsChannel().toInputStream()
        val filename = response.headers[HttpHeaders.ContentDisposition]!!
            .substringAfter("filename=\"")
            .substringBeforeLast("\"")
        return filename to stream
    }

    suspend fun close() {
        client.close()
        client.coroutineContext.job.join()
    }
}
