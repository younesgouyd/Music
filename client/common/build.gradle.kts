plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
    androidLibrary {
        namespace = "dev.younesgouyd.apps.music.client.common"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        androidResources.enable = true
    }
    jvm()
    sourceSets {
        commonMain.dependencies {
            implementation(project(":common"))
            implementation(libs.serialization.core)
            implementation(libs.serialization.json)
            implementation(libs.coroutines.core)
            implementation(libs.compose.material3)
            implementation(libs.compose.windowSizeClass)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.logging)
            implementation(libs.ktor.serialization)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.engine)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.websockets)
        }
        jvmMain.dependencies {
            implementation(libs.coroutines.desktop)
            implementation(compose.desktop.currentOs) {
                exclude("org.jetbrains.compose.material") // todo
            }
            implementation(libs.logback.jvm)
            implementation(libs.javacv)
            runtimeOnly(libs.ffmpeg)
        }
        androidMain.dependencies {
            implementation(libs.coroutines.android)
            implementation(libs.android.coreKtx)
            implementation(libs.android.activityKtx)
            implementation(libs.android.activityCompose)
            implementation(libs.android.media3.common)
            implementation(libs.android.media3.exoplayer)
            implementation(libs.android.media3.session)
            implementation(libs.logback.android)
        }
    }
}
