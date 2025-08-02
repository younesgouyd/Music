package dev.younesgouyd.apps.music.common.data.repoes

import dev.younesgouyd.apps.music.common.util.DarkThemeOptions
import dev.younesgouyd.apps.music.common.util.FileManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.*
import java.util.*

class SettingsRepo(private val fileManager: FileManager) {
    companion object {
        private const val DARK_THEME_TAG = "dark_theme"
    }

    private val darkThemeCallbacks: MutableList<Callback> = mutableListOf()

    suspend fun init() {
        fileManager.init()
        val value = getDarkTheme()
        if (value == null) {
            updateDarkTheme(DarkThemeOptions.SystemDefault)
        }
    }

    fun getDarkThemeFlow(): Flow<DarkThemeOptions?> {
        fun <T> MutableCollection<T>.removeIfCompat(predicate: (T) -> Boolean) {
            val iterator = this.iterator()
            while (iterator.hasNext()) {
                if (predicate(iterator.next())) {
                    iterator.remove()
                }
            }
        }
        return callbackFlow {
            val callback = object : Callback() {
                override suspend fun onNewValue(value: DarkThemeOptions?) {
                    send(value)
                }
            }
            darkThemeCallbacks += callback
            callback.onNewValue(getDarkTheme())
            awaitClose {
                darkThemeCallbacks.removeIfCompat { it.id == callback.id }
            }
        }
    }

    suspend fun updateDarkTheme(theme: DarkThemeOptions) {
        val data: String = fileManager.getData()
        val result = buildJsonObject {
            for ((key, value) in Json.parseToJsonElement(data).jsonObject) {
                put(key, value)
            }
            put(DARK_THEME_TAG, JsonPrimitive(theme.name))
        }.toString()
        fileManager.save(result)
        for (callback in darkThemeCallbacks) {
            callback.onNewValue(getDarkTheme())
        }
    }

    private suspend fun getDarkTheme(): DarkThemeOptions? {
        val data = fileManager.getData()
        val json = Json.parseToJsonElement(data).jsonObject
        val value = json[DARK_THEME_TAG]?.jsonPrimitive?.contentOrNull ?: return null
        return when (value) {
            DarkThemeOptions.SystemDefault.name -> DarkThemeOptions.SystemDefault
            DarkThemeOptions.Enabled.name -> DarkThemeOptions.Enabled
            DarkThemeOptions.Disabled.name -> DarkThemeOptions.Disabled
            else -> TODO()
        }
    }

    private abstract class Callback {
        val id: UUID = UUID.randomUUID()

        abstract suspend fun onNewValue(value: DarkThemeOptions?)
    }
}