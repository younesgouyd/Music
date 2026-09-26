package dev.younesgouyd.apps.music.server.multiplatform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

class ClientManager {
    private val _clientsStateFlow: MutableStateFlow<List<Client>> = MutableStateFlow(emptyList())
    val clients: StateFlow<List<Client>> = _clientsStateFlow.asStateFlow()

    fun register(client: Client) {
        _clientsStateFlow.update { it + client }

    }

    suspend fun remove(id: Uuid) {
        val client = _clientsStateFlow.value.find { it.id == id }
        _clientsStateFlow.update {
            it.filterNot { it.id == id }
        }
        client?.close()
    }

    fun get(id: Uuid): Client? {
        return _clientsStateFlow.value.find { it.id == id }
    }
}