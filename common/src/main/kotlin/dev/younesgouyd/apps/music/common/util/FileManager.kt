package dev.younesgouyd.apps.music.common.util

import org.json.JSONObject

typealias FileName = String

abstract class FileManager {
    abstract suspend fun init()

    abstract suspend fun clear()

    abstract suspend fun save(data: JSONObject)

    abstract suspend fun getData(): JSONObject
}