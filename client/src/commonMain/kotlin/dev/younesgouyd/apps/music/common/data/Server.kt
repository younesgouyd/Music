package dev.younesgouyd.apps.music.common.data

import dev.younesgouyd.apps.music.common.Inspection
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.zip.ZipInputStream

class Server(private val serverAddress: StateFlow<String?>) {
    private val client = HttpClient(CIO) {
        install(Logging) { level = LogLevel.ALL }
        install(ContentNegotiation) { json() }
        install(SSE)
        install(HttpTimeout) {
            this.requestTimeoutMillis = 60_000
        }
        engine {
            requestTimeout = 60_000
        }
    }
    private val tempDir = File("temp").also { it.mkdir() }

    suspend fun inspect(url: String): Inspection {
        return client.get("${getAddress()}/inspect") {
            parameter("url", url)
        }.body<Inspection>()
    }

    fun download(items: List<Long>): Flow<String> {
        return flow {
            client.sse(
                urlString = "${getAddress()}/download",
                request = {
                    contentType(ContentType.Application.Json)
                    setBody(items)
                },
                block = {
                    incoming.collect {
                        this@flow.emit(it.event!!)
                    }
                }
            )
        }
    }

    suspend fun getResult(): File {
        val zipFile = File(tempDir, "result.zip")
        if (zipFile.exists()) {
            zipFile.delete()
        }

        client.prepareGet("${getAddress()}/getResult") {
            this.onDownload { bytesSentTotal, contentLength ->
//                println("::getResult | Received $bytesSentTotal bytes from $contentLength") // TODO
            }
        }.execute { httpResponse ->
            val channel = httpResponse.body<ByteReadChannel>()
            channel.copyAndClose(zipFile.writeChannel())
        }

        val extractedDir = File(tempDir, "extracted")
        if (extractedDir.exists()) {
            extractedDir.deleteRecursively()
        } else {
            extractedDir.mkdir()
        }
        ZipInputStream(zipFile.inputStream()).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val outFile = File(extractedDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { output ->
                        zipIn.copyTo(output)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        return extractedDir
    }

    private fun getAddress(): String {
        val settingsAddress = serverAddress.value
        return if (settingsAddress.isNullOrEmpty()) TODO() else settingsAddress
    }
}