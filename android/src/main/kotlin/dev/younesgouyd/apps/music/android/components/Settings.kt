package dev.younesgouyd.apps.music.android.components

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
import dev.younesgouyd.apps.music.common.components.Settings
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.util.DarkThemeOptions

class Settings(
    repoStore: RepoStore
) : Settings(repoStore) {
    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier = modifier, state = state)
    }

    private object Ui {
        @Composable
        fun Main(modifier: Modifier, state: SettingsState) {
            when (state) {
                is SettingsState.Loading -> Text(modifier = modifier, text = "Loading...")
                is SettingsState.Loaded -> Settings(modifier = modifier, loaded = state)
            }
        }

        @Composable
        private fun Settings(modifier: Modifier, loaded: SettingsState.Loaded) {
            val scrollState = rememberScrollState()
            val darkTheme by loaded.darkTheme.collectAsState()

            Column(
                modifier = modifier.fillMaxWidth().verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DarkTheme(
                    modifier = Modifier.fillMaxWidth(),
                    selectedOption = darkTheme,
                    onDarkThemeChange = loaded.onDarkThemeChange
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