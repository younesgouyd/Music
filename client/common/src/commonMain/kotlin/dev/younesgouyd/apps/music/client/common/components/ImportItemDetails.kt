package dev.younesgouyd.apps.music.client.common.components

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
import dev.younesgouyd.apps.music.client.common.components.util.*
import dev.younesgouyd.apps.music.client.common.data.repoes.ImportSessionItemRepo
import dev.younesgouyd.apps.music.client.common.data.repoes.ImportSessionRepo
import dev.younesgouyd.apps.music.client.common.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.common.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.client.common.util.Component
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.ImportSessionItem
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.common.models.TrackId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
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
    private val state: StateFlow<Ui.State>

    init {
        val importItem = importSessionItemRepo.get(id).filterNotNull()
            .map { dbItem -> dbItem.toModel(mediaFileRepo) }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
        var loaded: Ui.State.Loaded? = null
        state = importItem.onEach {
            if (it != null && loaded == null) {
                loaded = Ui.State.Loaded(
                    importItem = importItem.filterNotNull().stateIn(coroutineScope),
                    importSession = importItem
                        .flatMapLatest {
                            it?.let { importSessionRepo.get(it.importSessionId) } ?: flowOf(null)
                        }.map { dbSession ->
                            dbSession?.let {
                                Ui.State.Loaded.ImportSession(
                                    id = dbSession.id,
                                    uri = dbSession.uri,
                                    image = mediaFileRepo.getImportSessionImage(dbSession.id)
                                )
                            }
                        }.stateIn(coroutineScope),
                    track = trackRepo.getImportSessionTrack(id).map { dbTrack ->
                        dbTrack?.let { Ui.State.Loaded.Track(dbTrack.track.id) }
                    }.stateIn(coroutineScope),
                    onImportSessionClick = showImportSession,
                    onTrackClick = showTrack
                )
            }
        }.map {
            if (it == null) {
                Ui.State.ItemDoesNotExist
            } else {
                loaded!!
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Ui.State.Loading)
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier, state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private suspend fun ImportSessionItem.toModel(mediaFileRepo: MediaFileRepo): Ui.State.Loaded.ImportItem {
        return Ui.State.Loaded.ImportItem(
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


    private object Ui {
        sealed class State {
            data object Loading : State()

            data class Loaded(
                val importItem: StateFlow<ImportItem>,
                val importSession: StateFlow<ImportSession?>,
                val track: StateFlow<Track?>,
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
                    val image: File?
                )

                data class ImportSession(
                    val id: ImportSessionId,
                    val uri: String,
                    val image: File?
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
            importItem: StateFlow<State.Loaded.ImportItem>,
            importSession: StateFlow<State.Loaded.ImportSession?>,
            track: StateFlow<State.Loaded.Track?>,
            onImportSessionClick: (ImportSessionId) -> Unit,
            onTrackClick: (TrackId) -> Unit
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