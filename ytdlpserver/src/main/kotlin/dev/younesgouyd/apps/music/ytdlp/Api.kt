package dev.younesgouyd.apps.music.ytdlp

import dev.younesgouyd.apps.music.common.models.Base64Image
import dev.younesgouyd.apps.music.common.models.datamodels.Inspection
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import java.io.File
import java.net.URI
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.seconds

object Api {
    private val logger = KotlinLogging.logger {}
    private val tempDir = File(System.getProperty("user.home"), "ytdlpserverdata").also { it.mkdir() }
    private val downloadDir = File(tempDir, "download").also { it.mkdir() }
    private val ytDlpOutput = File(tempDir, "ytdlp_stdout")
    private val ytDlpErrorOutput = File(tempDir, "ytdlp_stderr")
    private val ytDlpSerializer = Json { ignoreUnknownKeys = true }

    suspend fun inspect(url: String): Inspection {
        logger.debug { "--> ::inspect(url: $url)" }
        ensureLatestVersion()
        val containerCommand = runCommand(
            args = listOf("--dump-single-json", "--simulate", "--quiet", "--no-warnings", url),
            ignoreError = true
        )
        val ytDlpType = ytDlpSerializer.decodeFromString<YtDlpModels.Type>(containerCommand)
        return when (ytDlpType.type) {
            "playlist" -> {
                val container = ytDlpSerializer.decodeFromString<YtDlpModels.Container>(containerCommand)
                val containerThumbnailUrl = container.thumbnails
                    .filter { !it.url.contains("maxresdefault") }
                    .maxBy { it.width * it.height }
                    .url
                Inspection(
                    container = Inspection.Container(
                        uri = url,
                        title = container.title.ifBlank { TODO() },
                        description = container.description?.nullIfBlank(),
                        thumbnailUrl = containerThumbnailUrl,
                        thumbnail = downloadThumbnail(containerThumbnailUrl),
                    ),
                    items = buildList {
                        runCommand(listOf("--get-url", "--flat-playlist", "--simulate", "--quiet", "--no-warnings", url))
                            .split("\n")
                            .distinct()
                            .filter { it != "null" && it.isNotBlank() }
                            .forEachIndexed { index, url ->
                                val ytDlpInspection = runCommand(
                                    args = listOf("--dump-single-json", "--simulate", "--quiet", "--no-warnings", url),
                                    ignoreError = true
                                )
                                if (ytDlpInspection.isNotBlank() && ytDlpInspection.trim() != "null") {
                                    val entry = ytDlpSerializer.decodeFromString<YtDlpModels.Entry>(ytDlpInspection)
                                    val itemThumbnailUrl = entry.thumbnail?.nullIfBlank()
                                    val item = Inspection.Item(
                                        uri = entry.url.ifBlank { TODO() },
                                        title = entry.title.ifBlank { TODO() },
                                        durationMilliseconds = entry.duration?.seconds?.inWholeMilliseconds ?: TODO(),
                                        artists = entry.artists,
                                        album = entry.album?.let { if (it == "null") null else it.nullIfBlank() },
                                        id = (index + 1).toLong(),
                                        thumbnailUrl = itemThumbnailUrl,
                                        thumbnail = itemThumbnailUrl?.let { downloadThumbnail(it) }
                                    )
                                    add(item)
                                }
                            }
                    }
                )
            }
            else -> {
                val single = ytDlpSerializer.decodeFromString<YtDlpModels.Entry>(containerCommand)
                val title = single.title.ifBlank { TODO() }
                val thumbnailUrl = single.thumbnail?.nullIfBlank()
                Inspection(
                    container = Inspection.Container(
                        uri = url,
                        title = title,
                        description = null,
                        thumbnailUrl = null,
                        thumbnail = null
                    ),
                    items = listOf(
                        Inspection.Item(
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
            logger.debug { "::download | command response for $url: \n$commandResponse" }
        }
    }

    fun getResult(): File {
        val downloads = downloadDir.listFiles().orEmpty()
        if (downloads.isEmpty() || downloads.size > 1) { TODO() }
        return downloads[0]
    }

    private suspend fun ensureLatestVersion() {
        withContext(Dispatchers.IO) {
            val version = runCommand(listOf("--version"))
            logger.debug { "yt-dlp version: $version" }
            logger.debug { "calling --update..." }
            runCommand(listOf("--update"))
            val stdout = ytDlpOutput.readText()
            val stderr = ytDlpErrorOutput.readText()
            logger.debug { "--update stdout: \n$stdout" }
            logger.debug { "--update stderr: \n$stderr" }
        }
    }

    private suspend fun runCommand(args: List<String>, ignoreError: Boolean = false): String {
        return withContext(Dispatchers.IO) {
            logger.debug { "::runCommand | args: ${args.joinToString(" ")}" }
            val process = ProcessBuilder("yt-dlp", *args.toTypedArray())
                .redirectOutput(ytDlpOutput)
                .redirectError(ytDlpErrorOutput)
                .start()
            try {
                logger.debug { "::runCommand | waiting for command to finish..." }
                val exitCode = runInterruptible { process.waitFor() }
                logger.debug { "::runCommand | command finished with exitCode: $exitCode" }
                if (!ignoreError && exitCode != 0) {
                    val errorMsg = ytDlpErrorOutput.readText()
                    throw Exception("yt-dlp failed: $errorMsg")
                }
                ytDlpOutput.readText()
            } catch (e: CancellationException) {
                logger.debug { "::runCommand | coroutine cancelled, terminating yt-dlp..." }
                process.destroyForcibly()
                throw e
            }
        }
    }

    private suspend fun downloadThumbnail(url: String): Base64Image {
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
            val thumbnails: List<Thumbnail>
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
