package dev.younesgouyd.apps.music.common.models

import dev.younesgouyd.apps.music.common.models.datamodels.MediaPlayerState
import dev.younesgouyd.apps.music.common.models.datamodels.PlaybackContext
import dev.younesgouyd.apps.music.common.models.datamodels.PlaybackIntervalStart
import dev.younesgouyd.apps.music.common.models.spotify.SearchResult
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import dev.younesgouyd.apps.music.common.models.RepeatState as RepeatStateModel
import dev.younesgouyd.apps.music.common.models.datamodels.Client as ClientModel
import dev.younesgouyd.apps.music.common.models.datamodels.Folder as FolderModel
import dev.younesgouyd.apps.music.common.models.datamodels.ImportSession as ImportSessionModel
import dev.younesgouyd.apps.music.common.models.datamodels.ImportSessionItem as ImportSessionItemModel
import dev.younesgouyd.apps.music.common.models.datamodels.Inspection as InspectionModel
import dev.younesgouyd.apps.music.common.models.datamodels.MediaPlayerState as MediaPlayerStateModel
import dev.younesgouyd.apps.music.common.models.datamodels.PlaybackInterval as PlaybackIntervalModel
import dev.younesgouyd.apps.music.common.models.datamodels.Playlist as PlaylistModel
import dev.younesgouyd.apps.music.common.models.datamodels.PlaylistTrack as PlaylistTrackModel
import dev.younesgouyd.apps.music.common.models.datamodels.PlaylistTrackCrossRef as PlaylistTrackCrossRefModel
import dev.younesgouyd.apps.music.common.models.datamodels.QueueItem as QueueItemModel
import dev.younesgouyd.apps.music.common.models.datamodels.Setting as SettingModel
import dev.younesgouyd.apps.music.common.models.datamodels.SpotifyAlbum as SpotifyAlbumModel
import dev.younesgouyd.apps.music.common.models.datamodels.SpotifyArtist as SpotifyArtistModel
import dev.younesgouyd.apps.music.common.models.datamodels.SpotifyAuthState as SpotifyAuthStateModel
import dev.younesgouyd.apps.music.common.models.datamodels.SpotifyTrackRelation as SpotifyTrackRelationModel
import dev.younesgouyd.apps.music.common.models.datamodels.Tag as TagModel
import dev.younesgouyd.apps.music.common.models.datamodels.Track as TrackModel
import dev.younesgouyd.apps.music.common.models.datamodels.TrackRelation as TrackRelationModel

@Serializable
sealed class Rpc {
    @Serializable
    sealed class ToClient : Rpc() {
        @Serializable
        sealed class MediaPlayer : ToClient() {
            @Serializable
            data object ObserveState : MediaPlayer(), ResultType.Data.Dynamic<MediaPlayerState> {
                @Transient override val serializer: KSerializer<MediaPlayerState> = MediaPlayerState.serializer()
            }

            @Serializable
            data class SetTrack(val id: MediaFileId) : MediaPlayer(), ResultType.Action

            @Serializable
            data class Play(val context: PlaybackContext.ServerSent) : MediaPlayer(), ResultType.Action

            @Serializable
            data object Pause : MediaPlayer(), ResultType.Action

            @Serializable
            data object Stop : MediaPlayer(), ResultType.Action

            @Serializable
            data class SetTime(val positionMs: Long, val context: PlaybackContext.ServerSent) : MediaPlayer(), ResultType.Action

            @Serializable
            data class SetState(val value: MediaPlayerState, val context: PlaybackContext.ServerSent?) : MediaPlayer(), ResultType.Action
        }
    }

    @Serializable
    sealed class ToServer : Rpc() {
        @Serializable
        data class ClearImportItem(val id: ImportSessionItemId) : ToServer(), ResultType.Action

        @Serializable
        data class DeleteFolder(val id: FolderId) : ToServer(), ResultType.Action

        @Serializable
        sealed class ImportSessionItem : ToServer() {
            @Serializable
            data class Get(
                val id: ImportSessionItemId
            ) : ImportSessionItem(), ResultType.Data.Dynamic<ImportSessionItemModel?> {
                @Transient override val serializer: KSerializer<ImportSessionItemModel?> = ImportSessionItemModel.serializer().nullable
            }

            @Serializable
            data class Search(
                val importSessionId: ImportSessionId,
                val state: ImportSessionItemModel.State,
                val titleQuery: String,
                val order: DbOrder
            ) : ImportSessionItem(), ResultType.Data.Dynamic<List<ImportSessionItemModel>> {
                @Transient override val serializer: KSerializer<List<ImportSessionItemModel>> = ListSerializer(ImportSessionItemModel.serializer())
            }

            @Serializable
            data class UpdateState(
                val id: ImportSessionItemId,
                val state: ImportSessionItemModel.State
            ) : ImportSessionItem(), ResultType.Action
        }

        @Serializable
        sealed class ImportSession : ToServer() {
            @Serializable
            data class GetAll(
                val limit: Int,
                val offset: Offset.Index
            ) : ImportSession(), ResultType.Data.Static<List<ImportSessionModel>> {
                @Transient override val serializer: KSerializer<List<ImportSessionModel>> = ListSerializer(ImportSessionModel.serializer())
            }

            @Serializable
            data class Get(
                val id: ImportSessionId
            ) : ImportSession(), ResultType.Data.Dynamic<ImportSessionModel?> {
                @Transient override val serializer: KSerializer<ImportSessionModel?> = ImportSessionModel.serializer().nullable
            }
        }

        @Serializable
        sealed class Inspection : ToServer() {
            @Serializable
            data class Inspect(
                val url: String
            ) : Inspection(), ResultType.Data.Static<InspectionModel> {
                @Transient override val serializer: KSerializer<InspectionModel> = InspectionModel.serializer()
            }
        }

        @Serializable
        sealed class MediaController : ToServer() {
            @Serializable
            data object ObserveState : MediaController(), ResultType.Data.Dynamic<MediaControllerGeneralState> {
                @Transient override val serializer: KSerializer<MediaControllerGeneralState> = MediaControllerGeneralState.serializer()
            }

            @Serializable
            sealed class ObserveAvailableState : MediaController() {
                @Serializable data object Clients : ObserveAvailableState(), ResultType.Data.Dynamic<List<ClientModel>> {
                    @Transient override val serializer: KSerializer<List<ClientModel>> = ListSerializer(ClientModel.serializer())
                }
                @Serializable data object SelectedClient : ObserveAvailableState(), ResultType.Data.Dynamic<ClientId?> {
                    @Transient override val serializer: KSerializer<ClientId?> = ClientId.serializer().nullable
                }
                @Serializable data object Enabled : ObserveAvailableState(), ResultType.Data.Dynamic<Boolean> {
                    @Transient override val serializer: KSerializer<Boolean> = Boolean.serializer()
                }
                @Serializable data object Queue : ObserveAvailableState(), ResultType.Data.Dynamic<List<QueueItemModel>> {
                    @Transient override val serializer: KSerializer<List<QueueItemModel>> = ListSerializer(QueueItemModel.serializer())
                }
                @Serializable data object QueueItemIndex : ObserveAvailableState(), ResultType.Data.Dynamic<Int> {
                    @Transient override val serializer: KSerializer<Int> = Int.serializer()
                }
                @Serializable data object RepeatState : ObserveAvailableState(), ResultType.Data.Dynamic<RepeatStateModel> {
                    @Transient override val serializer: KSerializer<RepeatStateModel> = RepeatStateModel.serializer()
                }
                @Serializable data object MediaPlayerState : ObserveAvailableState(), ResultType.Data.Dynamic<MediaPlayerStateModel> {
                    @Transient override val serializer: KSerializer<MediaPlayerStateModel> = MediaPlayerStateModel.serializer()
                }
            }

            @Serializable
            data class SelectClient(val id: ClientId) : MediaController(), ResultType.Action

            @Serializable
            data class PlayQueue(
                val queue: List<QueueItemParameter>,
                val queueItemIndex: Int = 0
            ) : MediaController(), ResultType.Action

            @Serializable
            data object Play : MediaController(), ResultType.Action

            @Serializable
            data object Pause : MediaController(), ResultType.Action

            @Serializable
            data class Seek(val positionMs: Long) : MediaController(), ResultType.Action

            @Serializable
            data object Next : MediaController(), ResultType.Action

            @Serializable
            data object Previous : MediaController(), ResultType.Action

            @Serializable
            data object Repeat : MediaController(), ResultType.Action

            @Serializable
            data class AddToQueue(val items: List<QueueItemParameter>) : MediaController(), ResultType.Action

            @Serializable
            data class PlayItem(val index: Int) : MediaController(), ResultType.Action

            @Serializable
            data class ChangeItemIndex(
                val oldIndex: Int,
                val newIndex: Int
            ) : MediaController(), ResultType.Action

            @Serializable
            data class RemoveItem(val key: Int) : MediaController(), ResultType.Action
        }

        @Serializable
        sealed class PlaybackInterval : ToServer() {
            @Serializable
            data class Add(
                val item: PlaybackIntervalModel
            ) : PlaybackInterval(), ResultType.Action
        }

        @Serializable
        data class SetPlaybackIntervalStart(
            val start: PlaybackIntervalStart
        ) : ToServer(), ResultType.Action

        @Serializable
        sealed class Folder : ToServer() {
            @Serializable
            data class Get(
                val id: FolderId
            ) : Folder(), ResultType.Data.Dynamic<FolderModel?> {
                @Transient override val serializer: KSerializer<FolderModel?> = FolderModel.serializer().nullable
            }

            @Serializable
            data class SearchFolder(
                val folderId: FolderId?,
                val nameQuery: String
            ) : Folder(), ResultType.Data.Dynamic<List<FolderModel>> {
                @Transient override val serializer: KSerializer<List<FolderModel>> = ListSerializer(FolderModel.serializer())
            }

            @Serializable
            data class GetSubfolders(
                val id: FolderId?
            ) : Folder(), ResultType.Data.Dynamic<List<FolderModel>> {
                @Transient override val serializer: KSerializer<List<FolderModel>> = ListSerializer(FolderModel.serializer())
            }

            @Serializable
            data class Add(
                val name: String,
                val parentFolderId: FolderId?
            ) : Folder(), ResultType.Data.Static<FolderId> {
                @Transient override val serializer: KSerializer<FolderId> = FolderId.serializer()
            }

            @Serializable
            data class UpdateName(
                val id: FolderId,
                val name: String
            ) : Folder(), ResultType.Action

            @Serializable
            data class UpdateParentFolderId(
                val id: FolderId,
                val parentFolderId: FolderId?
            ) : Folder(), ResultType.Action

            @Serializable
            data class Delete(
                val id: FolderId
            ) : Folder(), ResultType.Action
        }

        @Serializable
        sealed class Playlist : ToServer() {
            @Serializable
            data class GetAll(
                val limit: Int,
                val offset: Offset.Id<PlaylistId>
            ) : Playlist(), ResultType.Data.Static<List<PlaylistModel>> {
                @Transient override val serializer: KSerializer<List<PlaylistModel>> = ListSerializer(PlaylistModel.serializer())
            }

            @Serializable
            data class Get(
                val id: PlaylistId
            ) : Playlist(), ResultType.Data.Dynamic<PlaylistModel?> {
                @Transient override val serializer: KSerializer<PlaylistModel?> = PlaylistModel.serializer().nullable
            }

            @Serializable
            data class Search(
                val nameQuery: String
            ) : Playlist(), ResultType.Data.Dynamic<List<PlaylistModel>> {
                @Transient override val serializer: KSerializer<List<PlaylistModel>> = ListSerializer(PlaylistModel.serializer())
            }

            @Serializable
            data class SearchFolder(
                val folderId: FolderId?,
                val nameQuery: String
            ) : Playlist(), ResultType.Data.Dynamic<List<PlaylistModel>> {
                @Transient override val serializer: KSerializer<List<PlaylistModel>> = ListSerializer(PlaylistModel.serializer())
            }

            @Serializable
            data class GetFolderPlaylists(
                val folderId: FolderId?
            ) : Playlist(), ResultType.Data.Dynamic<List<PlaylistModel>> {
                @Transient override val serializer: KSerializer<List<PlaylistModel>> = ListSerializer(PlaylistModel.serializer())
            }

            @Serializable
            data class GetTrackPlaylists(
                val id: TrackId
            ) : Playlist(), ResultType.Data.Dynamic<List<PlaylistModel>> {
                @Transient override val serializer: KSerializer<List<PlaylistModel>> = ListSerializer(PlaylistModel.serializer())
            }

            @Serializable
            data class Add(
                val name: String,
                val folderId: FolderId?
            ) : Playlist(), ResultType.Data.Static<PlaylistId> {
                @Transient override val serializer: KSerializer<PlaylistId> = PlaylistId.serializer()
            }

            @Serializable
            data class UpdateName(
                val id: PlaylistId,
                val name: String
            ) : Playlist(), ResultType.Action

            @Serializable
            data class UpdateFolderId(
                val id: PlaylistId,
                val folderId: FolderId?
            ) : Playlist(), ResultType.Action

            @Serializable
            data class Delete(
                val id: PlaylistId
            ) : Playlist(), ResultType.Action
        }

        @Serializable
        sealed class PlaylistTrackCrossRef : ToServer() {
            @Serializable
            data class Get(
                val playlistId: PlaylistId,
                val trackId: TrackId
            ) : PlaylistTrackCrossRef(), ResultType.Data.Dynamic<PlaylistTrackCrossRefModel?> {
                @Transient override val serializer: KSerializer<PlaylistTrackCrossRefModel?> = PlaylistTrackCrossRefModel.serializer().nullable
            }

            @Serializable
            data class Add(
                val playlistId: PlaylistId,
                val trackId: TrackId
            ) : PlaylistTrackCrossRef(), ResultType.Action

            @Serializable
            data class ChangeItemPosition(
                val playlistId: PlaylistId,
                val from: Int,
                val to: Int
            ) : PlaylistTrackCrossRef(), ResultType.Action

            @Serializable
            data class Delete(
                val playlistId: PlaylistId,
                val trackId: TrackId
            ) : PlaylistTrackCrossRef(), ResultType.Action
        }

        @Serializable
        data class PrepareImport(
            val selected: List<Long>,
            val url: String,
            val inspection: InspectionModel,
            val destinationFolderId: FolderId
        ) : ToServer(), ResultType.Data.Static<ImportSessionId> {
            @Transient override val serializer: KSerializer<ImportSessionId> = ImportSessionId.serializer()
        }

        @Serializable
        sealed class Setting : ToServer() {
            @Serializable
            data object GetDarkTheme : Setting(), ResultType.Data.Dynamic<SettingModel?> {
                @Transient override val serializer: KSerializer<SettingModel?> = SettingModel.serializer().nullable
            }

            @Serializable
            data class UpdateDarkTheme(
                val theme: DarkThemeOptions
            ) : Setting(), ResultType.Action
        }

        @Serializable
        data class SetTrackMetadataFromSpotify(
            val trackId: TrackId,
            val spotifyApiTrack: dev.younesgouyd.apps.music.common.models.spotify.Track
        ) : ToServer(), ResultType.Action

        @Serializable
        sealed class SpotifyAlbum : ToServer() {
            @Serializable
            data class Get(
                val id: SpotifyAlbumId
            ) : SpotifyAlbum(), ResultType.Data.Dynamic<SpotifyAlbumModel?> {
                @Transient override val serializer: KSerializer<SpotifyAlbumModel?> = SpotifyAlbumModel.serializer().nullable
            }

            @Serializable
            data class SearchArtist(
                val id: SpotifyArtistId,
                val nameQuery: String
            ) : SpotifyAlbum(), ResultType.Data.Dynamic<List<SpotifyAlbumModel>> {
                @Transient override val serializer: KSerializer<List<SpotifyAlbumModel>> = ListSerializer(SpotifyAlbumModel.serializer())
            }
        }

        @Serializable
        sealed class SpotifyArtist : ToServer() {
            @Serializable
            data class Get(
                val id: SpotifyArtistId
            ) : SpotifyArtist(), ResultType.Data.Dynamic<SpotifyArtistModel?> {
                @Transient override val serializer: KSerializer<SpotifyArtistModel?> = SpotifyArtistModel.serializer().nullable
            }

            @Serializable
            data class Search(
                val nameQuery: String,
                val limit: Int,
                val offset: Offset.Id<SpotifyArtistId>
            ) : SpotifyArtist(), ResultType.Data.Static<List<SpotifyArtistModel>> {
                @Transient override val serializer: KSerializer<List<SpotifyArtistModel>> = ListSerializer(SpotifyArtistModel.serializer())
            }

            @Serializable
            data class GetSpotifyTrackSpotifyArtists(
                val id: SpotifyTrackId
            ) : SpotifyArtist(), ResultType.Data.Dynamic<List<SpotifyArtistModel>> {
                @Transient override val serializer: KSerializer<List<SpotifyArtistModel>> = ListSerializer(SpotifyArtistModel.serializer())
            }

            @Serializable
            data class GetSpotifyAlbumSpotifyArtists(
                val id: SpotifyAlbumId
            ) : SpotifyArtist(), ResultType.Data.Dynamic<List<SpotifyArtistModel>> {
                @Transient override val serializer: KSerializer<List<SpotifyArtistModel>> = ListSerializer(SpotifyArtistModel.serializer())
            }
        }

        @Serializable
        sealed class SpotifyAuth : ToServer() {
            @Serializable
            data object GetAuthState : SpotifyAuth(), ResultType.Data.Static<SpotifyAuthStateModel> {
                @Transient override val serializer: KSerializer<SpotifyAuthStateModel> = SpotifyAuthStateModel.serializer()
            }

            @Serializable
            data class Authorize(
                val clientId: String,
                val clientSecret: String
            ) : SpotifyAuth(), ResultType.Action

            @Serializable
            data object Deauthorize : SpotifyAuth(), ResultType.Action
        }

        @Serializable
        sealed class SpotifySearch : ToServer() {
            @Serializable
            data class Search(
                val track: String,
                val artist: String?,
                val album: String?,
                val year: String?
            ) : SpotifySearch(), ResultType.Data.Static<SearchResult> {
                @Transient override val serializer: KSerializer<SearchResult> = SearchResult.serializer()
            }
        }

        @Serializable
        sealed class SpotifyTrack : ToServer() {
            @Serializable
            data class GetId(
                val spotifyId: String
            ) : SpotifyTrack(), ResultType.Data.Static<SpotifyTrackId?> {
                @Transient override val serializer: KSerializer<SpotifyTrackId?> = SpotifyTrackId.serializer().nullable
            }

            @Serializable
            data class GetAlbumTracks(
                val id: SpotifyAlbumId
            ) : SpotifyTrack(), ResultType.Data.Dynamic<List<SpotifyTrackRelationModel>> {
                @Transient override val serializer: KSerializer<List<SpotifyTrackRelationModel>> = ListSerializer(SpotifyTrackRelationModel.serializer())
            }
        }

        @Serializable
        sealed class Tag : ToServer() {
            @Serializable
            data class Get(
                val id: TagId
            ) : Tag(), ResultType.Data.Dynamic<TagModel?> {
                @Transient override val serializer: KSerializer<TagModel?> = TagModel.serializer().nullable
            }

            @Serializable
            data class Search(
                val nameQuery: String
            ) : Tag(), ResultType.Data.Dynamic<List<TagModel>> {
                @Transient override val serializer: KSerializer<List<TagModel>> = ListSerializer(TagModel.serializer())
            }

            @Serializable
            data class SearchTrackTags(
                val id: TrackId,
                val nameQuery: String
            ) : Tag(), ResultType.Data.Dynamic<List<TagModel>> {
                @Transient override val serializer: KSerializer<List<TagModel>> = ListSerializer(TagModel.serializer())
            }

            @Serializable
            data class SearchTrackUnsetTags(
                val id: TrackId,
                val nameQuery: String
            ) : Tag(), ResultType.Data.Dynamic<List<TagModel>> {
                @Transient override val serializer: KSerializer<List<TagModel>> = ListSerializer(TagModel.serializer())
            }

            @Serializable
            data class Add(
                val name: String
            ) : Tag(), ResultType.Data.Static<TagId> {
                @Transient override val serializer: KSerializer<TagId> = TagId.serializer()
            }

            @Serializable
            data class Delete(
                val id: TagId
            ) : Tag(), ResultType.Action
        }

        @Serializable
        sealed class TagTrackCrossRef : ToServer() {
            @Serializable
            data class Add(
                val tagId: TagId,
                val trackId: TrackId
            ) : TagTrackCrossRef(), ResultType.Action

            @Serializable
            data class Delete(
                val tagId: TagId,
                val trackId: TrackId
            ) : TagTrackCrossRef(), ResultType.Action
        }

        @Serializable
        sealed class Track : ToServer() {
            @Serializable
            data class Get(
                val id: TrackId
            ) : Track(), ResultType.Data.Dynamic<TrackRelationModel?> {
                @Transient override val serializer: KSerializer<TrackRelationModel?> = TrackRelationModel.serializer().nullable
            }

            @Serializable
            data class Search(
                val nameQuery: String,
                val limit: Int,
                val offset: Offset.Id<TrackId>
            ) : Track(), ResultType.Data.Static<List<TrackRelationModel>> {
                @Transient override val serializer: KSerializer<List<TrackRelationModel>> = ListSerializer(TrackRelationModel.serializer())
            }

            @Serializable
            data class SearchWithTags(
                val nameQuery: String,
                val tags: List<TagId>,
                val includeUntagged: Boolean,
                val limit: Int,
                val offset: Offset.Id<TrackId>
            ) : Track(), ResultType.Data.Static<List<TrackRelationModel>> {
                @Transient override val serializer: KSerializer<List<TrackRelationModel>> = ListSerializer(TrackRelationModel.serializer())
            }

            @Serializable
            data class SearchFolder(
                val folderId: FolderId?,
                val nameQuery: String
            ) : Track(), ResultType.Data.Dynamic<List<TrackRelationModel>> {
                @Transient override val serializer: KSerializer<List<TrackRelationModel>> = ListSerializer(TrackRelationModel.serializer())
            }

            @Serializable
            data class SearchFolderWithTags(
                val folderId: FolderId?,
                val nameQuery: String,
                val tags: List<TagId>,
                val includeUntagged: Boolean
            ) : Track(), ResultType.Data.Dynamic<List<TrackRelationModel>> {
                @Transient override val serializer: KSerializer<List<TrackRelationModel>> = ListSerializer(TrackRelationModel.serializer())
            }

            @Serializable
            data class SearchArtistContributions(
                val id: SpotifyArtistId,
                val nameQuery: String,
                val limit: Int,
                val offset: Offset.Id<TrackId>
            ) : Track(), ResultType.Data.Static<List<TrackRelationModel>> {
                @Transient override val serializer: KSerializer<List<TrackRelationModel>> = ListSerializer(TrackRelationModel.serializer())
            }

            @Serializable
            data class SearchPlaylist(
                val id: PlaylistId,
                val nameQuery: String
            ) : Track(), ResultType.Data.Dynamic<List<PlaylistTrackModel>> {
                @Transient override val serializer: KSerializer<List<PlaylistTrackModel>> = ListSerializer(PlaylistTrackModel.serializer())
            }

            @Serializable
            data class SearchWithTag(
                val nameQuery: String,
                val tag: TagId
            ) : Track(), ResultType.Data.Dynamic<List<TrackRelationModel>> {
                @Transient override val serializer: KSerializer<List<TrackRelationModel>> = ListSerializer(TrackRelationModel.serializer())
            }

            @Serializable
            data class GetFolderTracks(
                val id: FolderId
            ) : Track(), ResultType.Data.Dynamic<List<TrackModel>> {
                @Transient override val serializer: KSerializer<List<TrackModel>> = ListSerializer(TrackModel.serializer())
            }

            @Serializable
            data class GetArtistTracks(
                val id: SpotifyArtistId
            ) : Track(), ResultType.Data.Dynamic<List<TrackRelationModel>> {
                @Transient override val serializer: KSerializer<List<TrackRelationModel>> = ListSerializer(TrackRelationModel.serializer())
            }

            @Serializable
            data class GetAlbumTracks(
                val id: SpotifyAlbumId
            ) : Track(), ResultType.Data.Dynamic<List<TrackRelationModel>> {
                @Transient override val serializer: KSerializer<List<TrackRelationModel>> = ListSerializer(TrackRelationModel.serializer())
            }

            @Serializable
            data class GetPlaylistTracks(
                val id: PlaylistId
            ) : Track(), ResultType.Data.Dynamic<List<TrackRelationModel>> {
                @Transient override val serializer: KSerializer<List<TrackRelationModel>> = ListSerializer(TrackRelationModel.serializer())
            }

            @Serializable
            data class GetId(
                val spotifyId: String
            ) : Track(), ResultType.Data.Static<TrackId?> {
                @Transient override val serializer: KSerializer<TrackId?> = TrackId.serializer().nullable
            }

            @Serializable
            data class GetImportSessionTrack(
                val id: ImportSessionItemId
            ) : Track(), ResultType.Data.Dynamic<TrackRelationModel?> {
                @Transient override val serializer: KSerializer<TrackRelationModel?> = TrackRelationModel.serializer().nullable
            }

            @Serializable
            data class Add(
                val importSessionItemId: ImportSessionItemId,
                val spotifyTrackId: SpotifyTrackId?,
                val folderId: FolderId
            ) : Track(), ResultType.Data.Static<TrackId> {
                @Transient override val serializer: KSerializer<TrackId> = TrackId.serializer()
            }

            @Serializable
            data class UpdateFolderId(
                val id: TrackId,
                val folderId: FolderId?
            ) : Track(), ResultType.Action
        }

        @Serializable
        data class UnsetSpotifyTrack(
            val trackId: TrackId,
            val spotifyTrackId: SpotifyTrackId,
            val spotifyAlbumId: SpotifyAlbumId
        ) : ToServer(), ResultType.Action
    }
}

sealed interface ResultType {
    interface Action : ResultType

    sealed interface Data<T> : ResultType {
        val serializer: KSerializer<T>

        interface Static<T> : Data<T>

        interface Dynamic<T> : Data<T>

        interface Stream<T> : Data<T>
    }
}