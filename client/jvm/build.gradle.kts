import org.jetbrains.compose.desktop.application.dsl.TargetFormat

group = "dev.younesgouyd"
version = "0.1.0"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    implementation(project(":client:multiplatform"))
    implementation(libs.compose.uiDesktop)
    implementation(libs.compose.material3)
}

compose.desktop {
    application {
        mainClass = "dev.younesgouyd.apps.music.client.jvm.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Exe)
            packageName = "YounesMusic"
            linux {
                packageName = "dev.younesgouyd.apps.music.client.jvm"
                shortcut = true
            }
            windows {
                upgradeUuid = "118c8f19-14dd-4d77-8c06-a80b706b594c"
                shortcut = true
                menu = true
            }
        }
    }
}