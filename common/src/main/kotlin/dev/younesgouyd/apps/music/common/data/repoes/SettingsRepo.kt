package dev.younesgouyd.apps.music.common.data.repoes

import dev.younesgouyd.apps.music.common.DarkThemeOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*

class SettingsRepo {
    companion object {
        private const val DARK_THEME_TAG = "dark_theme"
    }

    private val darkThemeCallbacks: MutableList<Callback> = mutableListOf()

    suspend fun init() {
        AppDataSource.init()
        val value = getDarkTheme()
        if (value == null) {
            updateDarkTheme(DarkThemeOptions.SystemDefault)
        }
    }

    fun getDarkThemeFlow(): Flow<DarkThemeOptions?> {
        return callbackFlow {
            val callback = object : Callback() {
                override suspend fun onNewValue(value: DarkThemeOptions?) {
                    send(value)
                }
            }
            darkThemeCallbacks += callback
            callback.onNewValue(getDarkTheme())
            awaitClose { darkThemeCallbacks.removeIf { it.id == callback.id } }
        }
    }

    suspend fun updateDarkTheme(theme: DarkThemeOptions) {
        val data = AppDataSource.getData()
        data.put(DARK_THEME_TAG, theme.name)
        AppDataSource.save(data)
        for (callback in darkThemeCallbacks) {
            callback.onNewValue(getDarkTheme())
        }
    }

    private suspend fun getDarkTheme(): DarkThemeOptions? {
        val data = AppDataSource.getData()
        val value = if (data.has(DARK_THEME_TAG)) data.getString(DARK_THEME_TAG) else return null
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

    private object AppDataSource {
        private const val DATA_FILE_NAME = "younesmusic.json"
        private val charset = Charsets.UTF_8

        suspend fun init() {
            withContext(Dispatchers.IO) {
                val file = File(DATA_FILE_NAME)
                if (!file.exists()) {
                    file.createNewFile()
                    FileWriter(DATA_FILE_NAME, charset).use {
                        it.write(JSONObject().toString())
                    }
                }
            }
        }

        suspend fun clear() {
            withContext(Dispatchers.IO) {
                FileWriter(DATA_FILE_NAME, charset).use {
                    it.write("{}")
                }
            }
        }

        suspend fun save(data: JSONObject) {
            withContext(Dispatchers.IO) {
                FileWriter(DATA_FILE_NAME, charset).use {
                    it.write(data.toString())
                }
            }
        }

        suspend fun getData(): JSONObject {
            return withContext(Dispatchers.IO) {
                val result: String
                FileReader(DATA_FILE_NAME, charset).use {
                    result = it.readText()
                }
                return@withContext JSONObject(result)
            }
        }
    }
}