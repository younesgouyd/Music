package dev.younesgouyd.apps.music.common.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import java.io.*

class FileManager(private val directory: String) {
    companion object {
        private const val DATA_FILE_NAME = "younesmusic.json"
        private val charset = Charsets.UTF_8
    }

    suspend fun init() {
        withContext(Dispatchers.IO) {
            val file = File(directory, DATA_FILE_NAME)
            file.parentFile?.mkdirs()
            if (!file.exists()) {
                OutputStreamWriter(FileOutputStream(file), charset).use {
                    it.write(buildJsonObject {}.toString())
                }
            }
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            val file = File(directory, DATA_FILE_NAME)
            OutputStreamWriter(FileOutputStream(file), charset).use {
                it.write("{}")
            }
        }
    }

    suspend fun save(data: String) {
        withContext(Dispatchers.IO) {
            val file = File(directory, DATA_FILE_NAME)
            OutputStreamWriter(FileOutputStream(file), charset).use {
                it.write(data)
            }
        }
    }

    suspend fun getData(): String {
        return withContext(Dispatchers.IO) {
            val file = File(directory, DATA_FILE_NAME)
            InputStreamReader(FileInputStream(file), charset).use {
                return@use it.readText()
            }
        }
    }
}