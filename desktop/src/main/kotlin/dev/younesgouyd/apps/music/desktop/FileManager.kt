package dev.younesgouyd.apps.music.desktop

import dev.younesgouyd.apps.music.common.util.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*

class FileManager : FileManager() {
    companion object {
        private const val DATA_FILE_NAME = "younesmusic.json"
        private val charset = Charsets.UTF_8
    }

    override suspend fun init() {
        withContext(Dispatchers.IO) {
            val file = File("younesmusic", DATA_FILE_NAME)
            file.parentFile?.mkdirs()
            if (!file.exists()) {
                OutputStreamWriter(FileOutputStream(file), charset).use {
                    it.write(JSONObject().toString())
                }
            }
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            OutputStreamWriter(FileOutputStream(File(DATA_FILE_NAME)), charset).use {
                it.write("{}")
            }
        }
    }

    override suspend fun save(data: JSONObject) {
        withContext(Dispatchers.IO) {
            OutputStreamWriter(FileOutputStream(File(DATA_FILE_NAME)), charset).use {
                it.write(data.toString())
            }
        }
    }

    override suspend fun getData(): JSONObject {
        return withContext(Dispatchers.IO) {
            val result: String
            InputStreamReader(FileInputStream(File(DATA_FILE_NAME)), charset).use {
                result = it.readText()
            }
            return@withContext JSONObject(result)
        }
    }
}