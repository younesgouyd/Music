package dev.younesgouyd.apps.music.common

import kotlinx.serialization.Serializable

typealias Base64String = String

@Serializable
sealed class Inspection {
    @Serializable
    data class Webpage(
        val container: ContainerInspection.Webpage,
        val items: List<ItemInspection.InternetTrack>
    ) : Inspection()

    @Serializable
    data class Folder(
        val container: ContainerInspection.Folder,
        val items: List<ItemInspection.LocalFileTrack>
    ) : Inspection()

    @Serializable
    sealed class ContainerInspection {
        @Serializable
        data class Webpage(
            val uri: String,
            val title: String?,
            val description: String?,
            val thumbnailUrl: String?,
            val thumbnail: Base64String?
        ) : ContainerInspection()

        @Serializable
        data class Folder(
            val uri: String
        ) : ContainerInspection()
    }

    @Serializable
    sealed class ItemInspection {
        abstract val uri: String
        abstract val title: String
        abstract val durationMilliseconds: Long
        abstract val artists: List<String>
        abstract val album: String?

        @Serializable
        data class InternetTrack(
            override val uri: String,
            override val title: String,
            override val durationMilliseconds: Long,
            override val artists: List<String>,
            override val album: String?,
            val id: Long,
            val thumbnailUrl: String?,
            val thumbnail: Base64String?
        ) : ItemInspection()

        @Serializable
        data class LocalFileTrack(
            override val uri: String,
            override val title: String,
            override val durationMilliseconds: Long,
            override val artists: List<String>,
            override val album: String?,
            val path: List<String>,
            val albumTrackNumber: Int?,
            val lyrics: String?,
            val year: Int?,
            val albumImage: Base64String?
        ) : ItemInspection()
    }
}