package dev.younesgouyd.apps.music.server

import dev.younesgouyd.apps.music.common.Inspection
import dev.younesgouyd.apps.music.common.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Api {
    private val tempDir = File("temp").also { it.mkdir() }
    private val mediaDir = File(tempDir, "media").also { it.mkdir() }
    private val resultDir = File(tempDir, "result").also { it.mkdir() }
    private val ytDlpOutput = File(tempDir, "yt-dlp_output")
    private val ytDlpErrorOutput = File(tempDir, "yt-dlp_error_output")
    private var inspection: Inspection? = null
    private var selectedItems: List<Inspection.Item> = emptyList()

    suspend fun inspect(url: String): Inspection {
        inspection = null
        val commandResponse = runCommand("--dump-single-json", "--quiet", "--no-warnings", "--simulate", url)
        println("::inspect | commandResponse: $commandResponse")
        val ytDlpType = json.decodeFromString<YtDlpModels.Type>(commandResponse)
        val ret = Inspection(
            items = if (ytDlpType.type == "playlist") {
                val playlist = json.decodeFromString<YtDlpModels.Playlist>(commandResponse)
                playlist.entries.filterNotNull().mapIndexed { index, it ->
                    Inspection.Item(
                        id = (index + 1).toLong(),
                        url = it.webpageUrl,
                        title = it.title,
                        thumbnail = it.thumbnail,
                        artists = it.artists,
                        duration = it.duration,
                        album = it.album
                    )
                }
            } else {
                val single = json.decodeFromString<YtDlpModels.Single>(commandResponse)
                listOf(
                    Inspection.Item(
                        id = 1,
                        title = single.title,
                        thumbnail = single.thumbnail,
                        artists = single.artists,
                        duration = single.duration,
                        album = single.album,
                        url = single.webpageUrl
                    )
                )
            }
        )
        inspection = ret
        return ret
    }

    suspend fun download(items: List<Long>) {
        withContext(Dispatchers.IO) {
            selectedItems = emptyList()
            val index = File(tempDir, "index.json")
            index.delete()
            for (file in mediaDir.listFiles().orEmpty()) {
                file.deleteRecursively()
            }
            selectedItems = inspection!!.items.filter { it.id in items }

            val json = json.encodeToJsonElement(selectedItems)
            println("::download | encoded selected items: $json")
            index.writeText(json.toString())

            for (item in selectedItems) { // TODO
                val outputPath = "${mediaDir.absolutePath}/${item.id}"
                val commandResponse = runCommand(
                    "--extract-audio",
                    "--output", outputPath,
                    item.url
                )
                println("::download | command response for $item: \n$commandResponse")
            }
        }
    }

    suspend fun getResult(): File {
        println("--> ::getResult")
        return withContext(Dispatchers.IO) {
            for (file in resultDir.listFiles().orEmpty()) {
                file.deleteRecursively()
            }
            val zipFile = File(resultDir, "media.zip")
            println("::getResult | creating the zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                // index.json
                zipOut.putNextEntry(ZipEntry("index.json"))
                File(tempDir, "index.json").inputStream().use {
                    it.copyTo(zipOut)
                }
                zipOut.closeEntry()
                // media folder
                zipOut.putNextEntry(ZipEntry("media/"))
                zipOut.closeEntry()
                for (file in mediaDir.listFiles()) { // media files
                    check(file.isFile)
                    zipOut.putNextEntry(ZipEntry("media/${file.nameWithoutExtension}"))
                    file.inputStream().use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                }
            }
            zipFile
        }
    }

    private suspend fun runCommand(vararg args: String): String {
        return withContext(Dispatchers.IO) {
            println("::runCommand | args: ${args.joinToString(" ")}")
            val process = ProcessBuilder("yt-dlp", *args)
                .redirectErrorStream(false)
                .redirectOutput(ytDlpOutput)
                .redirectError(ytDlpErrorOutput)
                .start()
            println("::runCommand | waiting for command to finish...")
            val exitCode = process.waitFor()
            println("::runCommand | command finished with exitCode: $exitCode")
            if (exitCode != 0) {
                val errorMsg = ytDlpErrorOutput.readText()
                throw RuntimeException("yt-dlp failed: $errorMsg")
            }
            ytDlpOutput.readText()
        }
    }

    private object YtDlpModels {
        @Serializable
        data class Type(
            @SerialName("_type")
            val type: String
        )

        @Serializable
        data class Single(
            val id: String,
            val title: String,
            val thumbnail: String?,
            val duration: Int?,
            @SerialName("webpage_url")
            val webpageUrl: String,
            val album: String? = null,
            val artists: List<String> = emptyList(),
            @SerialName("release_date")
            val releaseDate: String? = null
        )

        @Serializable
        data class Playlist(
            val id: String,
            val title: String,
            @SerialName("playlist_count")
            val playlistCount: Int?,
            val entries: List<Entry?>
        ) {
            @Serializable
            data class Entry(
                val id: String,
                val title: String,
                val thumbnail: String?,
                val duration: Int?,
                @SerialName("webpage_url")
                val webpageUrl: String,
                val album: String? = null,
                val artists: List<String> = emptyList(),
                @SerialName("release_date")
                val releaseDate: String? = null
            )
        }
    }
}