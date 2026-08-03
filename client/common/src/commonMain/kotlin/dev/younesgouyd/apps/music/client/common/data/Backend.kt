package dev.younesgouyd.apps.music.client.common.data

import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.rpc.Rpc
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.job

class Backend(serverAddress: String) {
    private val httpClient = HttpClient(CIO) {
        install(Logging) { level = LogLevel.ALL }
        install(ContentNegotiation) { json(json) }
        defaultRequest {
            url(serverAddress)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
    }

    suspend fun call(rpc: Rpc): HttpResponse {
        return httpClient.request("rpc") {
            setBody<Rpc>(rpc)
        }
    }

    suspend fun close() {
        httpClient.close()
        httpClient.coroutineContext.job.join()
    }
}