package dev.younesgouyd.apps.music.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.app.Component
import dev.younesgouyd.apps.music.app.DarkThemeOptions
import dev.younesgouyd.apps.music.app.data.RepoStore
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class Settings(
    private val repoStore: RepoStore
) : Component() {
    override val title: String = "Settings"
    private val state: MutableStateFlow<SettingsState> = MutableStateFlow(SettingsState.Loading)

    init {
        coroutineScope.launch {
            state.update {
                SettingsState.State(
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
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private sealed class SettingsState {
        data object Loading : SettingsState()

        data class State(
            val darkTheme: StateFlow<DarkThemeOptions?>,
            val onDarkThemeChange: (DarkThemeOptions) -> Unit
        ) : SettingsState()
    }

    private object Ui {
        @Composable
        fun Main(state: SettingsState) {
            when (state) {
                is SettingsState.Loading -> Text("Loading...")
                is SettingsState.State -> Settings(state)
            }
        }

        @Composable
        private fun Settings(state: SettingsState.State) {
            val scrollState = rememberScrollState()
            val darkTheme by state.darkTheme.collectAsState()

            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DarkTheme(
                    modifier = Modifier.fillMaxWidth(),
                    selectedOption = darkTheme,
                    onDarkThemeChange = state.onDarkThemeChange
                )
            }
        }

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        private fun DarkTheme(
            modifier: Modifier = Modifier,
            selectedOption: DarkThemeOptions?,
            onDarkThemeChange: (DarkThemeOptions) -> Unit
        ) {
            var expanded by remember { mutableStateOf(false) }

            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark Theme")
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(),
                        value = selectedOption?.label ?: "",
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        for (darkThemeOption in DarkThemeOptions.entries) {
                            DropdownMenuItem(
                                text =  { Text(darkThemeOption.label) },
                                onClick = {
                                    onDarkThemeChange(darkThemeOption)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}