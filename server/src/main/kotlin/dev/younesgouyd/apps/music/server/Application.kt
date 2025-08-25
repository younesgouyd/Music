package dev.younesgouyd.apps.music.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.event.Level

object Application {
    fun start() {
        embeddedServer(Netty, port = 8080) {
            install(CallLogging) {
                level = Level.DEBUG
            }
//            install(ContentNegotiation) { json() }
            configureRouting()
        }.start(wait = true)
    }

    private fun Application.configureRouting() {
        routing {
            route("/Music") {
                get("/download") {
                    // returns
                    // {
                    //    "domain_name": "",
                    //    "result_id": "",
                    //    "type": "", // one of: single, list,
                    //    "items": [
                    //        {
                    //            "name": "",
                    //            "thumbnail": "" // base64 of the image
                    //        }
                    //    ]
                    // }
                    val url = call.request.queryParameters["url"]
                    if (url.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }

                    call.respond(HttpStatusCode.InternalServerError, "not implemented yet")

//                    val result = runCommand(url)
//                    if (result.isBlank()) {
//                        call.respond(HttpStatusCode.InternalServerError)
//                    } else {
//                        call.respondText(result, ContentType.Application.Json)
//                    }
                }

                get("/getResult") {
                    // returns the requested file

                    val id = call.request.queryParameters["id"]
                    if (id.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }

                    call.respond(HttpStatusCode.InternalServerError, "not implemented yet")

//                    val outputFile = File("somefile.mp3")
//                    if (!outputFile.exists()) {
//                        call.respond(HttpStatusCode.InternalServerError, "Download failed:\n$result")
//                        return@get
//                    }
//                    call.respondFile(outputFile)
//                    outputFile.deleteOnExit()
                }
            }
        }
    }

    private suspend fun runCommand(vararg args: String): String {
        return withContext(Dispatchers.IO) {
            val process = ProcessBuilder("yt-dlp", *args)
                .redirectErrorStream(false)
                .start()

            process.inputStream.bufferedReader().use { it.readText() }
        }
    }

    private object Models {
        // returns
        // {
        //    "result_id": "",
        //    "type": "", // one of: single, list,
        //    "items": [
        //        {
        //            "name": "",
        //            "thumbnail": "" // base64 of the image
        //        }
        //    ]
        // }
        data class DownloadResponse(
            val resultId: Int,
            val domainName: String,
            val type: Type,
            val items: List<Item>
        ) {
            enum class Type { Single, List }

            data class Item(
                val name: String,
                val thumbnail: String
            )
        }
    }
}