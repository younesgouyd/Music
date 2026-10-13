package dev.younesgouyd.apps.music.common.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import dev.younesgouyd.apps.music.common.models.DeviceType

@Composable
fun DeviceIcon(type: DeviceType?) {
    when (type) {
        DeviceType.Smartphone -> Icon(Icons.Default.Smartphone, null)
        DeviceType.Tablet -> Icon(Icons.Default.Tablet, null)
        DeviceType.Laptop -> Icon(Icons.Default.Laptop, null)
        DeviceType.Desktop -> Icon(Icons.Default.DesktopWindows, null)
        DeviceType.Car -> Icon(Icons.Default.DirectionsCar, null)
        DeviceType.Tv -> Icon(Icons.Default.Tv, null)
        DeviceType.Watch -> Icon(Icons.Default.Watch, null)
        null -> Icon(Icons.Default.DeviceUnknown, null)
    }
}