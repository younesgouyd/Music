package dev.younesgouyd.apps.music.server

import dev.younesgouyd.apps.music.common.Base64String
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.seconds

object Api {
    private val tempDir = File("temp").also { it.mkdir() }
    private val downloadDir = File(tempDir, "download").also { it.mkdir() }
    private val ytDlpOutput = File(tempDir, "yt-dlp_output")
    private val ytDlpErrorOutput = File(tempDir, "yt-dlp_error_output")
    private val ytDlpSerializer = Json { ignoreUnknownKeys = true }

    suspend fun inspect(url: String): Inspection.Webpage {
        val commandResponse = runCommand(
            "--dump-single-json",
            "--quiet",
            "--no-warnings",
            "--simulate",
            url
        )
        println("::inspect | commandResponse: $commandResponse")
        val ytDlpType = ytDlpSerializer.decodeFromString<YtDlpModels.Type>(commandResponse)
        return if (ytDlpType.type == "playlist") {
            val playlist = ytDlpSerializer.decodeFromString<YtDlpModels.Playlist>(commandResponse)
            val containerThumbnailUrl = playlist.thumbnails
                .filter { !it.url.contains("maxresdefault") }
                .maxBy { it.width * it.height }
                .url
            Inspection.Webpage(
                ytDlpInspection = commandResponse,
                container = Inspection.ContainerInspection.Webpage(
                    uri = url,
                    title = playlist.title.ifBlank { TODO() },
                    description = playlist.description?.nullIfBlank(),
                    thumbnailUrl = containerThumbnailUrl,
                    thumbnail = downloadThumbnail(containerThumbnailUrl),
                ),
                items = playlist.entries.filterNotNull().mapIndexed { index, it ->
                    val itemThumbnailUrl = it.thumbnail?.nullIfBlank()
                    Inspection.ItemInspection.InternetTrack(
                        uri = it.webpageUrl.ifBlank { TODO() },
                        title = it.title.ifBlank { TODO() },
                        durationMilliseconds = it.duration?.seconds?.inWholeMilliseconds ?: TODO(),
                        artists = it.artists,
                        album = it.album?.nullIfBlank(),
                        id = (index + 1).toLong(),
                        thumbnailUrl = itemThumbnailUrl,
                        thumbnail = itemThumbnailUrl?.let { downloadThumbnail(it) }
                    )
                }
            )
        } else {
            val single = ytDlpSerializer.decodeFromString<YtDlpModels.Single>(commandResponse)
            val title = single.title.ifBlank { TODO() }
            val thumbnailUrl = single.thumbnail?.nullIfBlank()
            Inspection.Webpage(
                ytDlpInspection = commandResponse,
                container = Inspection.ContainerInspection.Webpage(
                    uri = url,
                    title = title,
                    description = null,
                    thumbnailUrl = null,
                    thumbnail = null
                ),
                items = listOf(
                    Inspection.ItemInspection.InternetTrack(
                        uri = single.webpageUrl.ifBlank { TODO() },
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

    suspend fun download(url: String) {
        return withContext(Dispatchers.IO) {
            downloadDir.listFiles().orEmpty().forEach {
                it.delete()
            }
            val commandResponse = runCommand(
                "--extract-audio",
                "--output", "${downloadDir.absolutePath}/%(title)s.%(ext)s",
                url
            )
            println("::download | command response for $url: \n$commandResponse")
        }
    }

    fun getResult(): File {
        val downloads = downloadDir.listFiles().orEmpty()
        if (downloads.isEmpty() || downloads.size > 1) { TODO() }
        return downloads[0]
    }

    private suspend fun runCommand(vararg args: String): String {
        return withContext(Dispatchers.IO) {
            println("::runCommand | args: ${args.joinToString(" ")}")
            val process = ProcessBuilder("yt-dlp", *args)
                .redirectOutput(ytDlpOutput)
                .redirectError(ytDlpErrorOutput)
                .start()
            println("::runCommand | waiting for command to finish...")
            val exitCode = process.waitFor()
            println("::runCommand | command finished with exitCode: $exitCode")
            if (exitCode != 0) {
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

            @Serializable
            data class Entry(
                val id: String,
                val title: String,
                val thumbnail: String?,
                val duration: Int?, // TODO: confirm if this is always in seconds
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