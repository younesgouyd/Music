package dev.younesgouyd.apps.music.common.ui

import androidx.collection.intListOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

data class MenuItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

class RightClickMenuScope internal constructor(
    private val getContainerCoords: () -> LayoutCoordinates?,
    private val onOpenAt: (Offset?) -> Unit
) {
    private var anchorOffset: Offset? = null

    fun Modifier.menuAnchor(): Modifier = this.onGloballyPositioned { targetCoords ->
        val parent = getContainerCoords()
        if (parent != null && parent.isAttached && targetCoords.isAttached) {
            anchorOffset = parent.localPositionOf(
                targetCoords,
                Offset(0f, targetCoords.size.height.toFloat())
            )
        }
    }

    fun openMenuAtAnchor() {
        onOpenAt(anchorOffset)
    }
}

@Composable
fun RightClickMenuBox(
    modifier: Modifier = Modifier,
    menuItems: List<List<MenuItem>>,
    content: @Composable RightClickMenuScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var pointerOffset by remember { mutableStateOf<Offset?>(null) }
    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val scope = remember {
        RightClickMenuScope(
            getContainerCoords = { containerCoords },
            onOpenAt = { offset ->
                pointerOffset = offset
                expanded = true
            }
        )
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { containerCoords = it }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            val changes = event.changes.filterNot { it.isConsumed }
                            if (changes.isNotEmpty()) {
                                pointerOffset = changes.first().position
                                expanded = true
                                changes.forEach { it.consume() }
                            }
                        }
                    }
                }
            }
    ) {
        scope.content()

        val anchorPosition = remember(pointerOffset) {
            pointerOffset?.let { MenuAnchorPosition.atPointer(it) } ?: MenuAnchorPosition.Below
        }

        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(anchorPosition),
        ) {
            menuItems.forEachIndexed { index, group ->
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShape(index, menuItems.size)
                ) {
                    for (item in group) {
                        DropdownMenuItem(
                            text = { Text(item.label) },
                            leadingIcon = {
                                Icon(
                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                    imageVector = item.icon,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                expanded = false
                                item.onClick()
                            }
                        )
                    }
                }
                Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            }
        }
    }
}

private fun MenuAnchorPosition.Companion.atPointer(pointerOffset: Offset): MenuAnchorPosition {
    return Custom(
        xCandidates = {
            val rawX = anchorBounds.left + pointerOffset.x.toInt()
            // Flip to the left of the cursor if it overflows the right edge of the window
            val flippedX = rawX - menuSize.width
            // Fallback: clamp strictly inside window bounds
            val clampedX = rawX.coerceIn(0, (windowSize.width - menuSize.width).coerceAtLeast(0))

            intListOf(rawX, flippedX, clampedX)
        },
        yCandidates = {
            val rawY = anchorBounds.top + pointerOffset.y.toInt()
            // Flip above the cursor if it overflows the bottom edge of the window
            val flippedY = rawY - menuSize.height
            // Fallback: clamp strictly inside window bounds
            val clampedY = rawY.coerceIn(0, (windowSize.height - menuSize.height).coerceAtLeast(0))

            intListOf(rawY, flippedY, clampedY)
        }
    )
}