package dev.younesgouyd.apps.music.common.usecases

import dev.younesgouyd.apps.music.common.Inspection
import dev.younesgouyd.apps.music.common.data.RepoStore
import org.apache.tika.Tika
import java.io.File

expect class ImportFolderUseCase(
    repoStore: RepoStore,
    saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase,
    saveMp3FileAsTrackUseCase: SaveMp3FileAsTrackUseCase
) {
    val repoStore: RepoStore
    val saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase
    val saveMp3FileAsTrackUseCase: SaveMp3FileAsTrackUseCase
    suspend fun execute(import: Import): Boolean
}

sealed class Import {
    abstract val folderUri: String

    data class Local(
        override val folderUri: String
    ) : Import()

    data class Internet(
        override val folderUri: String,
        val url: String,
        val items: List<Inspection.Item>
    ) : Import()
}

private val tika = Tika()

fun File.isAudioFile(): Boolean {
    return audioMimeTypes.any { it == tika.detect(this) }
}

private val audioMimeTypes = listOf(
    // Uncompressed / PCM
    "audio/wav",
    "audio/x-wav", // (legacy)
    "audio/basic", //(µ-law, a-law)

    // MPEG family
    "audio/mpeg", // MP3
    "audio/aac", // audio-only MP4, AAC/ALAC
    "audio/mp4", // AAC in 3GPP container
    "audio/3gpp",
    "audio/3gpp2",

    // Ogg / Opus / Vorbis
    "application/ogg", // container, official default
    "audio/ogg", // when audio-only
    "audio/opus",
    "audio/vorbis",

    // FLAC / Lossless
    "audio/flac",
    "audio/alac", // less common, Apple Lossless

    // RealAudio
    "audio/vnd.rn-realaudio",

    // Windows formats
    "audio/x-ms-wma",
    "audio/x-ms-wax",

    // Other common ones
    "audio/amr",
    "audio/amr-wb",
    "audio/midi",
    "audio/x-midi",
    "audio/x-aiff",

    // Special cases
    "audio/webm",
    "application/x-shockwave-flash"
)