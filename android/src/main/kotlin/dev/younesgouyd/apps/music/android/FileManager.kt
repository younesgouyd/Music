package dev.younesgouyd.apps.music.android

import android.content.Context
import dev.younesgouyd.apps.music.common.util.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*

class FileManager(private val context: Context) : FileManager() {
    companion object {
        private const val DATA_FILE_NAME = "younesmusic.json"
        private val charset = Charsets.UTF_8
    }

    override suspend fun init() {
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, DATA_FILE_NAME)
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
            val file = File(context.filesDir, DATA_FILE_NAME)
            OutputStreamWriter(FileOutputStream(file), charset).use {
                it.write("{}")
            }
        }
    }

    override suspend fun save(data: JSONObject) {
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, DATA_FILE_NAME)
            OutputStreamWriter(FileOutputStream(file), charset).use {
                it.write(data.toString())
            }
        }
    }

    override suspend fun getData(): JSONObject {
        return withContext(Dispatchers.IO) {
            val file = File(context.filesDir, DATA_FILE_NAME)
            val result: String
            InputStreamReader(FileInputStream(file), charset).use {
                result = it.readText()
            }
            return@withContext JSONObject(result)
        }
    }
}