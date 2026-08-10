package dev.younesgouyd.apps.music.server

import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.Inspection
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.application.Application
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.*
import org.slf4j.event.Level
import java.io.File

object Application {
    fun start() {
        embeddedServer(factory = CIO, port = 8080) {
            install(CallLogging) {
                level = Level.DEBUG
                this.format { call ->
                    val status = call.response.status()
                    val httpMethod = call.request.httpMethod.value
                    val userAgent = call.request.headers["User-Agent"]
                    "Status: $status, HTTP method: $httpMethod, User agent: $userAgent"
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(SSE)
            configureRouting()
        }.start(wait = true)
    }

    private fun Application.configureRouting() {
        routing {
            route("/Music") {
                get("/inspect") {
                    val url = call.request.queryParameters["url"]
                    if (url.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    try {
                        val result: Inspection = Api.inspect(url)
                        call.respond<Inspection>(result)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }

                sse("/download") {
                    val url = call.request.queryParameters["url"]
                    if (url.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@sse
                    }
                    try {
                        Api.download(url)
                        send(ServerSentEvent(event = "completed"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        send(ServerSentEvent(event = "error"))
                    }
                    close()
                }

                get("/getResult") {
                    val file: File = Api.getResult()
                    call.response.header(
                        name = HttpHeaders.ContentDisposition,
                        value = "attachment; filename=\"${file.name}\""
                    )
                    call.respondFile(file)
                }
            }
        }
    }
}