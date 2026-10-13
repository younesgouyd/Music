package dev.younesgouyd.apps.music.server.multiplatform

import dev.younesgouyd.apps.music.common.models.ClientId
import java.util.concurrent.ConcurrentHashMap

class WebSocketSessionHandlers(
    private val sessions: ConcurrentHashMap<ClientId, WebSocketSessionHandler>
) {
    fun get(id: ClientId): WebSocketSessionHandler? {
        return sessions[id]
    }
}