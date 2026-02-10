package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import dev.younesgouyd.apps.music.client.components.util.*
import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.client.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.repoes.ImportSessionItemRepo
import dev.younesgouyd.apps.music.client.data.repoes.ImportSessionRepo
import dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSession.SourceType
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.client.util.Component
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ImportItemDetails(
    id: ImportSessionItemId,
    importSessionItemRepo: ImportSessionItemRepo,
    importSessionRepo: ImportSessionRepo,
    trackRepo: TrackRepo,
    mediaFileRepo: MediaFileRepo,
    showImportSession: (ImportSessionId) -> Unit,
    showTrack: (TrackId) -> Unit
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
                ImportItemDetailState.Loaded.Track(dbTrack.track.id,)
            }.stateIn(coroutineScope)
            state.value = ImportItemDetailState.Loaded(
                importItem = importItem,
                importSession = importSession,
                track = track,
                onImportSessionClick = { showImportSession(importSession.value.id) },
                onTrackClick = { showTrack(track.value!!.id) }
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
                    duration = this.inspection.durationMilliseconds.milliseconds,
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
                    duration = this.inspection.durationMilliseconds.milliseconds,
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
            val importSession: StateFlow<ImportSession>,
            val track: StateFlow<Track?>,
            val onImportSessionClick: () -> Unit,
            val onTrackClick: () -> Unit
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
                val id: TrackId
            )
        }
    }

    private object Ui {
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
                importSession = loaded.importSession,
                track = loaded.track,
                onImportSessionClick = loaded.onImportSessionClick,
                onTrackClick = loaded.onTrackClick
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            importItem: StateFlow<ImportItemDetailState.Loaded.ImportItem>,
            importSession: StateFlow<ImportItemDetailState.Loaded.ImportSession>,
            track: StateFlow<ImportItemDetailState.Loaded.Track?>,
            onImportSessionClick: () -> Unit,
            onTrackClick: () -> Unit
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
                    AdaptiveUi(
                        wide = {
                            ItemDetailsHeaderWide(
                                modifier = Modifier.height(500.dp),
                                title = importItem.title,
                                image = importItem.image,
                                itemAttributes = {
                                    Text(
                                        text = importItem.uri,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Album(name = importItem.album ?: "")
                                    Artists(names = importItem.artists)
                                    Duration(value = importItem.duration.formatted())
                                }
                            )
                        },
                        compact = {
                            ItemDetailsHeaderCompact(
                                title = importItem.title,
                                image = importItem.image,
                                itemAttributes = {
                                    Text(
                                        text = importItem.uri,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Album(name = importItem.album ?: "")
                                    Artists(names = importItem.artists)
                                    Duration(value = importItem.duration.formatted())
                                }
                            )
                        }
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Import Session:",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    ImportSessionInfo(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
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
                        Item(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            onClick = onTrackClick
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(start = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("track: ${it.id}")
                            }
                        }
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
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier.fillMaxHeight().aspectRatio(1f),
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
    }
}