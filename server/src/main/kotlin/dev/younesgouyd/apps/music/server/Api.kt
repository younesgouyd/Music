package dev.younesgouyd.apps.music.server

import dev.younesgouyd.apps.music.common.Base64String
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import java.io.File
import java.net.URI
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.seconds

object Api {
    private val tempDir = File(System.getProperty("user.home"), "younesmusicserverdata").also { it.mkdir() }
    private val downloadDir = File(tempDir, "download").also { it.mkdir() }
    private val ytDlpOutput = File(tempDir, "yt-dlp_output")
    private val ytDlpErrorOutput = File(tempDir, "yt-dlp_error_output")
    private val ytDlpSerializer = Json { ignoreUnknownKeys = true }

    suspend fun inspect(url: String): Inspection.Webpage {
        val containerCommand = runCommand(
            args = listOf("--dump-single-json", "--simulate", "--quiet", "--no-warnings", url),
            ignoreError = true
        )
        println("::inspect | commandResponse: $containerCommand")
        val ytDlpType = ytDlpSerializer.decodeFromString<YtDlpModels.Type>(containerCommand)
        return when (ytDlpType.type) {
            "playlist" -> {
                val nonNullEntries = ytDlpSerializer.decodeFromString<YtDlpModels.Container>(
                    runCommand(listOf("--dump-single-json", "--flat-playlist", "--simulate", "--quiet", "--no-warnings", url))
                ).entries
                if (nonNullEntries.contains(null)) { TODO() }
                val container = ytDlpSerializer.decodeFromString<YtDlpModels.Container>(containerCommand)
                val containerThumbnailUrl = container.thumbnails
                    .filter { !it.url.contains("maxresdefault") }
                    .maxBy { it.width * it.height }
                    .url
                Inspection.Webpage(
                    container = Inspection.ContainerInspection.Webpage(
                        uri = url,
                        title = container.title.ifBlank { TODO() },
                        description = container.description?.nullIfBlank(),
                        thumbnailUrl = containerThumbnailUrl,
                        thumbnail = downloadThumbnail(containerThumbnailUrl),
                    ),
                    items = runCommand(listOf("--get-url", "--flat-playlist", "--simulate", "--quiet", "--no-warnings", url))
                        .split("\n")
                        .distinct()
                        .filter { it != "null" && it.isNotBlank() }
                        .mapIndexed { index, url ->
                            val ytDlpInspection = runCommand(
                                args = listOf("--dump-single-json", "--simulate", "--quiet", "--no-warnings", url),
                                ignoreError = true
                            )
                            val entry = if (ytDlpInspection.trim() == "null") {
                                nonNullEntries.find { it!!.url == url }!!
                            } else {
                                ytDlpSerializer.decodeFromString<YtDlpModels.Entry>(ytDlpInspection)
                            }
                            val itemThumbnailUrl = entry.thumbnail?.nullIfBlank()
                            Inspection.ItemInspection.InternetTrack(
                                uri = entry.url.ifBlank { TODO() },
                                title = entry.title.ifBlank { TODO() },
                                durationMilliseconds = entry.duration?.seconds?.inWholeMilliseconds ?: TODO(),
                                artists = entry.artists,
                                album = entry.album?.let { if (it == "null") null else it.nullIfBlank() },
                                id = (index + 1).toLong(),
                                thumbnailUrl = itemThumbnailUrl,
                                thumbnail = itemThumbnailUrl?.let { downloadThumbnail(it) }
                            )
                        }
                )
            }
            else -> {
                val single = ytDlpSerializer.decodeFromString<YtDlpModels.Entry>(containerCommand)
                val title = single.title.ifBlank { TODO() }
                val thumbnailUrl = single.thumbnail?.nullIfBlank()
                Inspection.Webpage(
                    container = Inspection.ContainerInspection.Webpage(
                        uri = url,
                        title = title,
                        description = null,
                        thumbnailUrl = null,
                        thumbnail = null
                    ),
                    items = listOf(
                        Inspection.ItemInspection.InternetTrack(
                            uri = single.url.ifBlank { TODO() },
                            title = title,
                            durationMilliseconds = single.duration?.seconds?.inWholeMilliseconds ?: TODO(),
                            artists = single.artists,
                            album = single.album?.nullIfBlank(),
                            id = 1,
                            thumbnailUrl = thumbnailUrl,
                            thumbnail = thumbnailUrl?.let { downloadThumbnail(it) }
                        )
                    )
                )
            }
        }
    }

    suspend fun download(url: String) {
        return withContext(Dispatchers.IO) {
            downloadDir.listFiles().orEmpty().forEach {
                it.delete()
            }
            val commandResponse = runCommand(
                listOf(
                    "--extract-audio",
                    "--output", "${downloadDir.absolutePath}/%(title)s.%(ext)s",
                    url
                )
            )
            println("::download | command response for $url: \n$commandResponse")
        }
    }

    fun getResult(): File {
        val downloads = downloadDir.listFiles().orEmpty()
        if (downloads.isEmpty() || downloads.size > 1) { TODO() }
        return downloads[0]
    }

    private suspend fun runCommand(args: List<String>, ignoreError: Boolean = false): String {
        return withContext(Dispatchers.IO) {
            println("::runCommand | args: ${args.joinToString(" ")}")
            val process = ProcessBuilder("yt-dlp", *args.toTypedArray())
                .redirectOutput(ytDlpOutput)
                .redirectError(ytDlpErrorOutput)
                .start()
            println("::runCommand | waiting for command to finish...")
            val exitCode = process.waitFor()
            println("::runCommand | command finished with exitCode: $exitCode")
            if (!ignoreError && exitCode != 0) {
                val errorMsg = ytDlpErrorOutput.readText()
                throw Exception("yt-dlp failed: $errorMsg")
            }
            ytDlpOutput.readText()
        }
    }

    private suspend fun downloadThumbnail(url: String): Base64String {
        return withContext(Dispatchers.IO) {
            val bytes = URI(url).toURL().readBytes()
            Base64.encode(bytes)
        }
    }

    private fun String.nullIfBlank(): String? {
        return this.ifBlank { null }
    }

    private object YtDlpModels {
        @Serializable
        data class Type(
            @SerialName("_type")
            val type: String
        )
        @OptIn(ExperimentalSerializationApi::class)
        @Serializable
        data class Entry(
            val id: String,
            val title: String,
            val thumbnail: String? = null,
            val duration: Int? = null, // TODO: confirm if this is always in seconds
            @JsonNames("url", "original_url") // TODO
            val url: String,
            val album: String? = null,
            val artists: List<String> = emptyList(),
            @SerialName("release_date")
            val releaseDate: String? = null
        )

        @Serializable
        data class Container(
            val id: String,
            val title: String,
            val description: String?,
            val thumbnails: List<Thumbnail>,
            val entries: List<Entry?>
        ) {
            @Serializable
            data class Thumbnail(
                val url: String,
                val height: Int,
                val width: Int,
                val id: String
            )
        }
    }
}
