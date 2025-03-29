package dev.younesgouyd.apps.music.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.common.data.repoes.ArtistRepo
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class ArtistList(
    artistRepo: ArtistRepo,
    showArtistDetails: (Long) -> Unit
) : Component() {
    override val title: String = "Artists"
    protected val state: MutableStateFlow<ArtistListState> = MutableStateFlow(ArtistListState.Loading)

    init {
        coroutineScope.launch {
            state.update {
                ArtistListState.Loaded(
                    artists = artistRepo.getAll().mapLatest { list ->
                        list.map { dbArtist ->
                            ArtistListState.Loaded.ArtistItem(
                                id = dbArtist.id,
                                name = dbArtist.name,
                                image = dbArtist.image
                            )
                        }
                    }.stateIn(coroutineScope),
                    onArtistClick = showArtistDetails
                )
            }
        }
    }

    @Composable
    abstract override fun show(modifier: Modifier)

    override fun clear() {
        coroutineScope.cancel()
    }

    protected sealed class ArtistListState {
        data object Loading : ArtistListState()

        data class Loaded(
            val artists: StateFlow<List<ArtistItem>>,
            val onArtistClick: (Long) -> Unit
        ) : ArtistListState() {
            data class ArtistItem(
                val id: Long,
                val name: String,
                val image: ByteArray?
            )
        }
    }
}
