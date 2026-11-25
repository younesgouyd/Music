package dev.younesgouyd.apps.music.client.multiplatform.components.util

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String? = null
) {
    TextField(
        modifier = modifier.size(width = 720.dp, height = SearchBarDefaults.InputFieldHeight),
        value = query,
        onValueChange = onQueryChange,
        placeholder = placeholder?.let { { Text(placeholder) } },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        } else null,
        singleLine = true,
        shape = SearchBarDefaults.inputFieldShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        )
    )
}