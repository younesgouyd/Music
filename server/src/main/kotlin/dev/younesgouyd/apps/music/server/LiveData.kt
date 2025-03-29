package dev.younesgouyd.apps.music.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.time.Duration.Companion.seconds

@Serializable
data class LiveData(val timestamp: Long, val value: Double)

fun generateLiveData(): Flow<LiveData> = flow {
    while (true) {
        emit(LiveData(System.currentTimeMillis(), Random().nextDouble() * 100))
        delay(1000) // Send data every second
    }
}

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(WebSockets) {
            pingPeriod = 15.seconds
        }

        routing {
            webSocket("/live-data") {
                println("Client connected!")

                val job = launch {
                    generateLiveData().collect { data ->
                        val json = Json.encodeToString(data)
                        send(json)
                    }
                }

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            println("Client message: ${frame.readText()}")
                        }
                    }
                } finally {
                    job.cancel() // Stop sending data when client disconnects
                    println("Client disconnected")
                }
            }
        }
    }.start(wait = true)
}
