package dev.younesgouyd.apps.music.common.data

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LiveData(val timestamp: Long, val value: Double)

suspend fun main() {
    val client = HttpClient {
        install(WebSockets)
    }

    client.webSocket("ws://localhost:8080/live-data") {
        println("Connected to WebSocket!")

        for (frame in incoming) {
            if (frame is Frame.Text) {
                val liveData = Json.decodeFromString<LiveData>(frame.readText())
                println("Received live data: $liveData")
            }
        }
    }
}