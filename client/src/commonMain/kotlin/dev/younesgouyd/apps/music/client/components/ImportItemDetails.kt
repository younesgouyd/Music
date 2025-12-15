package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.components.util.compose.formatted
import dev.younesgouyd.apps.music.client.components.util.compose.widgets.Image
import dev.younesgouyd.apps.music.client.components.util.compose.widgets.Item
import dev.younesgouyd.apps.music.client.data.*
import dev.younesgouyd.apps.music.client.data.repoes.*
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSession.SourceType
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFile
import dev.younesgouyd.apps.music.client.util.Component
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class ImportItemDetails(
    id: ImportSessionItemId,
    importSessionItemRepo: ImportSessionItemRepo,
    importSessionRepo: ImportSessionRepo,
    trackRepo: TrackRepo,
    mediaFileRepo: MediaFileRepo,
    artistRepo: ArtistRepo,
    showImportSession: (ImportSessionId) -> Unit,
    showTrack: (TrackId) -> Unit,
    showArtist: (ArtistId) -> Unit
) : Component() {
    override val title: String = "Import Item"
    private val state: MutableStateFlow<ImportItemDetailState> = MutableStateFlow(ImportItemDetailState.Loading)

    init {
        coroutineScope.launch {
            val importItem = importSessionItemRepo.get(id)
                .map { dbItem -> dbItem.toModel(mediaFileRepo) }
                .stateIn(coroutineScope)
            val importSession = importItem.flatMapLatest { importSessionRepo.get(it.importSessionId) }
                .map { dbSession ->
                    ImportItemDetailState.Loaded.ImportSession(
                        id = dbSession.id,
                        uri = dbSession.uri,
                        sourceType = dbSession.sourceType,
                        image = mediaFileRepo.getImportSessionImage(dbSession.id)
                    )
                }.stateIn(coroutineScope)
            val track = trackRepo.getImportSessionTrack(id).map { dbTrack ->
                if (dbTrack == null) return@map null
                ImportItemDetailState.Loaded.Track(
                    id = dbTrack.id,
                    name = dbTrack.name,
                    duration = dbTrack.duration,
                    image = mediaFileRepo.getTrackImage(dbTrack.id),
                    album = dbTrack.album,
                    artists = artistRepo.getTrackArtists(dbTrack.id).first().map { dbArtist ->
                        ImportItemDetailState.Loaded.Track.Artist(
                            id = dbArtist.id,
                            name = dbArtist.name
                        )
                    }
                )
            }.stateIn(coroutineScope)
            state.value = ImportItemDetailState.Loaded(
                importItem = importItem,
                imageFile = mediaFileRepo.getImportSessionItemImageMediaFile(id),
                audioFile = mediaFileRepo.getImportSessionItemAudioMediaFile(id),
                importSession = importSession,
                track = track,
                onImportSessionClick = { showImportSession(importSession.value.id) },
                onTrackClick = { showTrack(track.value!!.id) },
                onArtistClick = showArtist,
                onExportImageClick = {
                    TODO()
                }
            )
        }
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier, state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private suspend fun ImportSessionItem.toModel(mediaFileRepo: MediaFileRepo): ImportItemDetailState.Loaded.ImportItem {
        return when (this.inspection) {
            is Inspection.ItemInspection.LocalFileTrack -> {
                ImportItemDetailState.Loaded.ImportItem(
                    id = this.id,
                    uri = this.uri,
                    importSessionId = this.importSessionId,
                    state = this.state,
                    title = this.inspection.title,
                    duration = this.inspection.duration,
                    artists = this.inspection.artists,
                    album = this.inspection.album,
                    image = mediaFileRepo.getImportSessionItemImage(this.id)
                )
            }
            is Inspection.ItemInspection.InternetTrack -> {
                ImportItemDetailState.Loaded.ImportItem(
                    id = this.id,
                    uri = this.uri,
                    importSessionId = this.importSessionId,
                    state = this.state,
                    title = this.inspection.title,
                    duration = this.inspection.duration,
                    artists = this.inspection.artists,
                    album = this.inspection.album,
                    image = mediaFileRepo.getImportSessionItemImage(this.id)
                )
            }
        }
    }

    private sealed class ImportItemDetailState {
        data object Loading : ImportItemDetailState()

        data class Loaded(
            val importItem: StateFlow<ImportItem>,
            val imageFile: MediaFile?,
            val audioFile: MediaFile?,
            val importSession: StateFlow<ImportSession>,
            val track: StateFlow<Track?>,
            val onImportSessionClick: () -> Unit,
            val onTrackClick: () -> Unit,
            val onArtistClick: (ArtistId) -> Unit,
            val onExportImageClick: (MediaFileId) -> Unit,
        ) : ImportItemDetailState() {
            data class ImportItem(
                val id: ImportSessionItemId,
                val uri: String,
                val importSessionId: ImportSessionId,
                val state: ImportSessionItem.State,
                val title: String,
                val duration: Duration,
                val artists: List<String>,
                val album: String?,
                val image: File?
            )

            data class ImportSession(
                val id: ImportSessionId,
                val uri: String,
                val sourceType: SourceType,
                val image: File?
            )

            data class Track(
                val id: TrackId,
                val name: String,
                val duration: Duration?,
                val image: File?,
                val album: String?,
                val artists: List<Artist>
            ) {
                data class Artist(
                    val id: ArtistId,
                    val name: String
                )
            }
        }
    }

    private object Ui {
        private val IMAGE_SIZE = 250.dp

        @Composable
        fun Main(modifier: Modifier, state: ImportItemDetailState) {
            when (state) {
                is ImportItemDetailState.Loading -> Text(modifier = modifier, text = "Loading...")
                is ImportItemDetailState.Loaded -> Main(modifier = modifier, loaded = state)
            }
        }

        @Composable
        private fun Main(modifier: Modifier, loaded: ImportItemDetailState.Loaded) {
            Main(
                modifier = modifier,
                importItem = loaded.importItem,
                imageFile = loaded.imageFile,
                audioFile = loaded.audioFile,
                importSession = loaded.importSession,
                track = loaded.track,
                onImportSessionClick = loaded.onImportSessionClick,
                onTrackClick = loaded.onTrackClick,
                onArtistClick = loaded.onArtistClick,
                onExportImageClick = loaded.onExportImageClick
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            importItem: StateFlow<ImportItemDetailState.Loaded.ImportItem>,
            imageFile: MediaFile?,
            audioFile: MediaFile?,
            importSession: StateFlow<ImportItemDetailState.Loaded.ImportSession>,
            track: StateFlow<ImportItemDetailState.Loaded.Track?>,
            onImportSessionClick: () -> Unit,
            onTrackClick: () -> Unit,
            onArtistClick: (ArtistId) -> Unit,
            onExportImageClick: (MediaFileId) -> Unit
        ) {
            val importItem by importItem.collectAsState()
            val importSession by importSession.collectAsState()
            val track by track.collectAsState()

            Surface(
                modifier = modifier,
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Import Info:",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    ImportItemInfo(
                        modifier = Modifier.fillMaxWidth(),
                        importItem = importItem
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Import Session:",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    ImportSessionInfo(
                        modifier = Modifier.fillMaxWidth(),
                        importSession = importSession,
                        onClick = onImportSessionClick
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Track:",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    track?.let {
                        TrackInfo(
                            modifier = Modifier.fillMaxWidth(),
                            track = it,
                            onClick = onTrackClick,
                            onArtistClick = onArtistClick
                        )
                    }
                }
            }
        }

        @Composable
        private fun ImportItemInfo(
            modifier: Modifier,
            importItem: ImportItemDetailState.Loaded.ImportItem
        ) {
            Surface(
                modifier = modifier,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier.size(IMAGE_SIZE),
                        file = importItem.image
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = importItem.uri,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = importItem.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (importItem.album != null) {
                            Album(importItem.album)
                        }
                        Artists(
                            modifier = Modifier.fillMaxWidth(),
                            list = importItem.artists
                        )
                        Duration(importItem.duration)
                    }
                }
            }
        }

        @Composable
        private fun ImportSessionInfo(
            modifier: Modifier,
            importSession: ImportItemDetailState.Loaded.ImportSession,
            onClick: () -> Unit
        ) {
            Item(
                modifier = modifier,
                onClick = onClick
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier.size(IMAGE_SIZE),
                        file = importSession.image
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = importSession.uri,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        @OptIn(ExperimentalLayoutApi::class)
        @Composable
        private fun TrackInfo(
            modifier: Modifier,
            track: ImportItemDetailState.Loaded.Track,
            onClick: () -> Unit,
            onArtistClick: (ArtistId) -> Unit
        ) {
            Item(
                modifier = modifier,
                onClick = onClick
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier.size(IMAGE_SIZE),
                        file = track.image
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = track.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (track.album != null) {
                            Album(track.album)
                        }
                        Artists(
                            modifier = Modifier.fillMaxWidth(),
                            list = track.artists,
                            onArtistClick = onArtistClick
                        )
                        if (track.duration != null) {
                            Duration(track.duration)
                        }
                    }
                }
            }
        }

        @Composable
        private fun Artists(
            modifier: Modifier,
            list: List<ImportItemDetailState.Loaded.Track.Artist>,
            onArtistClick: (ArtistId) -> Unit
        ) {
            LazyRow(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(items = list, key = { it.id.value }) { artist ->
                    Item(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onArtistClick(artist.id) }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, null)
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        @Composable
        private fun Artists(
            modifier: Modifier,
            list: List<String>
        ) {
            LazyRow(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(items = list) { artist ->
                    Item(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, null)
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        @Composable
        private fun Album(value: String) {
            Surface(
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Album, null)
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        @Composable
        private fun Duration(value: Duration) {
            Surface(
                modifier = Modifier.padding(8.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Timer, null)
                    Text(
                        text = value.formatted(),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}