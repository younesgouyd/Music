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
    implementation(project(":client:app:multiplatform"))
    implementation(compose.desktop.currentOs) {
        exclude("org.jetbrains.compose.material") // todo
    }
}

compose.desktop {
    application {
        mainClass = "dev.younesgouyd.apps.music.client.app.jvm.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.AppImage, TargetFormat.Deb)
            packageName = "dev.younesgouyd.apps.music.client.app.jvm"
        }
    }
}