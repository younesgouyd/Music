package dev.younesgouyd.apps.music.server.multiplatform

import org.conscrypt.Conscrypt
import java.security.Security

actual fun setupSecurityProvider() {
    Security.insertProviderAt(Conscrypt.newProvider(), 1)
}