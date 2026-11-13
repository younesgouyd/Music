package dev.younesgouyd.apps.music.client.multiplatform.components

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
import dev.younesgouyd.apps.music.client.multiplatform.components.util.*
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.ImportSessionItemRepo
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.ImportSessionRepo
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.common.models.datamodels.ImportSessionItem
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.ui.Item
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ImportItemDetails(
    id: ImportSessionItemId,
    importSessionItemRepo: ImportSessionItemRepo,
    importSessionRepo: ImportSessionRepo,
    trackRepo: TrackRepo,
    showImportSession: (ImportSessionId) -> Unit,
    showTrack: (TrackId) -> Unit
) : Component() {
    override val title: String = "Import Item"
    private val state: MutableStateFlow<Ui.State> = MutableStateFlow(Ui.State.Loading)

    init {
        coroutineScope.launch {
            val dbItem = importSessionItemRepo.get(id).stateIn(coroutineScope)
            val dbImportId = dbItem.mapLatest { it?.importSessionId }
            combine(
                dbItem,
                dbImportId.flatMapLatest { if (it != null) importSessionRepo.get(it) else flowOf(null) },
                trackRepo.getImportSessionTrack(id),
                ::Triple
            ).mapLatest { (importSessionItem, importSession, trackRelation) ->
                if (importSessionItem == null) return@mapLatest Ui.State.ItemDoesNotExist
                Ui.State.Loaded(
                    importItem = importSessionItem.toModel(),
                    importSession = importSession?.let { Ui.State.Loaded.ImportSession(it.id, it.uri, it.imgId) },
                    track = trackRelation?.let { Ui.State.Loaded.Track(it.track.id) },
                    onImportSessionClick = showImportSession,
                    onTrackClick = showTrack
                )
            }.collect(state)
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

    private fun ImportSessionItem.toModel(): Ui.State.Loaded.ImportItem {
        return Ui.State.Loaded.ImportItem(
            id = this.id,
            uri = this.uri,
            importSessionId = this.importSessionId,
            state = this.state,
            title = this.inspection.title,
            duration = this.inspection.durationMilliseconds.milliseconds,
            artists = this.inspection.artists,
            album = this.inspection.album,
            image = this.imgId
        )
    }


    private object Ui {
        sealed class State {
            data object Loading : State()

            data class Loaded(
                val importItem: ImportItem,
                val importSession: ImportSession?,
                val track: Track?,
                val onImportSessionClick: (ImportSessionId) -> Unit,
                val onTrackClick: (TrackId) -> Unit
            ) : State() {
                data class ImportItem(
                    val id: ImportSessionItemId,
                    val uri: String,
                    val importSessionId: ImportSessionId,
                    val state: ImportSessionItem.State,
                    val title: String,
                    val duration: Duration,
                    val artists: List<String>,
                    val album: String?,
                    val image: MediaFileId?
                )

                data class ImportSession(
                    val id: ImportSessionId,
                    val uri: String,
                    val image: MediaFileId?
                )

                data class Track(
                    val id: TrackId
                )
            }

            data object ItemDoesNotExist : State()
        }

        @Composable
        fun Main(modifier: Modifier, state: State) {
            when (state) {
                is State.Loading -> Text(modifier = modifier, text = "Loading...")
                is State.Loaded -> Main(modifier = modifier, loaded = state)
                is State.ItemDoesNotExist -> Text(modifier = modifier, text = "This item no long exists")
            }
        }

        @Composable
        private fun Main(modifier: Modifier, loaded: State.Loaded) {
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
            importItem: State.Loaded.ImportItem,
            importSession: State.Loaded.ImportSession?,
            track: State.Loaded.Track?,
            onImportSessionClick: (ImportSessionId) -> Unit,
            onTrackClick: (TrackId) -> Unit
        ) {
            val scrollState = rememberScrollState()

            Surface(
                modifier = modifier,
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                        .verticalScroll(scrollState),
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
                    importSession?.let {
                        ImportSessionInfo(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            importSession = it,
                            onClick = { onImportSessionClick(it.id) }
                        )
                    } ?: Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text("Import Session not found!")
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Track:",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    track?.let {
                        Item(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            onClick = { onTrackClick(it.id) }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(start = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("track: ${it.id}")
                            }
                        }
                    } ?: Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text("Track not found!")
                    }
                }
            }
        }

        @Composable
        private fun ImportSessionInfo(
            modifier: Modifier,
            importSession: State.Loaded.ImportSession,
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
                        id = importSession.image
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