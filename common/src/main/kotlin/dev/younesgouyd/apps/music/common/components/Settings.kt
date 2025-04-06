package dev.younesgouyd.apps.music.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.util.Component
import dev.younesgouyd.apps.music.common.util.DarkThemeOptions
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class Settings(
    private val repoStore: RepoStore
) : Component() {
    override val title: String = "Settings"
    protected val state: MutableStateFlow<SettingsState> = MutableStateFlow(SettingsState.Loading)

    init {
        coroutineScope.launch {
            state.update {
                SettingsState.Loaded(
                    darkTheme = repoStore.settingsRepo.getDarkThemeFlow().stateIn(
                        scope = coroutineScope,
                        started = SharingStarted.WhileSubscribed(),
                        initialValue = null
                    ),
                    onDarkThemeChange = {
                        coroutineScope.launch { repoStore.settingsRepo.updateDarkTheme(it) }
                    }
                )
            }
        }
    }

    @Composable
    abstract override fun show(modifier: Modifier)

    override fun clear() {
        coroutineScope.cancel()
    }

    protected sealed class SettingsState {
        data object Loading : SettingsState()

        data class Loaded(
            val darkTheme: StateFlow<DarkThemeOptions?>,
            val onDarkThemeChange: (DarkThemeOptions) -> Unit
        ) : SettingsState()
    }
}