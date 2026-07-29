rootProject.name = "Music"

include(":server:common")
include(":server:jvm")
include(":server:android")
include(":common")
include(":client:common")
include(":client:jvm")
include(":client:android")
include(":ytdlpserver")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            val versions = object {
                val java = version("java", "21")
                val kotlin = "2.4.0"
                val serialization = "1.11.0"
                val jsonJava = "20260522"
                val coroutines = "1.11.0"
                val room = "2.8.4"
                val sqlite = "2.7.0"
                val ksp = "2.3.10"
                val logging = "8.0.4"
                val logback = object {
                    val jvm = "1.5.38"
                    val android = "3.0.0"
                }
                val ktor = "3.5.1"
                val compose = object {
                    val plugin = "1.11.1"
                    val material3 = "1.9.0"
                    val materialIconsExtended = "1.7.3"
                }
                val android = object {
                    val agp = "9.0.0"
                    val androidCompileSdk = version("androidCompileSdk", "36")
                    val androidMinSdk = version("androidMinSdk", "29")
                    val androidTargetSdk = version("androidTargetSdk", "36")
                    val coreKtx = "1.17.0"
                    val activity = "1.13.0"
                    val media3 = "1.10.1"
                    val startup = "1.2.0"
                }
                val javacv = "1.5.13"
                val ffmpeg = "8.0.1-1.5.13"
            }

            plugin("kotlin.multiplatform", "org.jetbrains.kotlin.multiplatform").version(versions.kotlin)
            plugin("kotlin.jvm", "org.jetbrains.kotlin.jvm").version(versions.kotlin)
            plugin("kotlin.serialization", "org.jetbrains.kotlin.plugin.serialization").version(versions.kotlin)
            plugin("composeMultiplatform", "org.jetbrains.compose").version(versions.compose.plugin)
            plugin("composeCompiler", "org.jetbrains.kotlin.plugin.compose").version(versions.kotlin)
            plugin("ksp", "com.google.devtools.ksp").version(versions.ksp)
            plugin("room", "androidx.room").version(versions.room)
            plugin("android.application", "com.android.application").version(versions.android.agp)
            plugin("android.multiplatform", "com.android.kotlin.multiplatform.library").version(versions.android.agp)

            library("compose.material3", "org.jetbrains.compose.material3", "material3").version(versions.compose.material3)
            library("compose.windowSizeClass", "org.jetbrains.compose.material3", "material3-window-size-class").version(versions.compose.material3)
            library("compose.materialIconsExtended", "org.jetbrains.compose.material", "material-icons-extended").version(versions.compose.materialIconsExtended)
            library("compose.uiDesktop", "org.jetbrains.compose.ui", "ui-desktop").version(versions.compose.plugin)

            library("serialization.core", "org.jetbrains.kotlinx", "kotlinx-serialization-core").version(versions.serialization)
            library("serialization.json", "org.jetbrains.kotlinx", "kotlinx-serialization-json").version(versions.serialization)
            library("jsonJava", "org.json", "json").version(versions.jsonJava)

            library("coroutines.core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").version(versions.coroutines)
            library("coroutines.desktop", "org.jetbrains.kotlinx", "kotlinx-coroutines-swing").version(versions.coroutines)
            library("coroutines.android", "org.jetbrains.kotlinx", "kotlinx-coroutines-android").version(versions.coroutines)

            library("room.runtime", "androidx.room", "room-runtime").version(versions.room)
            library("room.compiler", "androidx.room", "room-compiler").version(versions.room)
            library("sqlite.jvm", "androidx.sqlite", "sqlite-bundled-jvm").version(versions.sqlite)
            library("sqlite.android", "androidx.sqlite", "sqlite-bundled").version(versions.sqlite)

            library("javacv", "org.bytedeco", "javacv").version(versions.javacv)
            library("ffmpeg", "org.bytedeco", "ffmpeg-platform").version(versions.ffmpeg)

            library("logging", "io.github.oshai", "kotlin-logging").version(versions.logging)
            library("logback.jvm", "ch.qos.logback", "logback-classic").version(versions.logback.jvm)
            library("logback.android", "com.github.tony19", "logback-android").version(versions.logback.android)

            library("ktor.serialization", "io.ktor", "ktor-serialization-kotlinx-json").version(versions.ktor)
            library("ktor.server.core", "io.ktor", "ktor-server-core").version(versions.ktor)
            library("ktor.server.engine.cio", "io.ktor", "ktor-server-cio").version(versions.ktor)
            library("ktor.server.engine.netty", "io.ktor", "ktor-server-netty").version(versions.ktor)
            library("ktor.server.logging", "io.ktor", "ktor-server-call-logging").version(versions.ktor)
            library("ktor.server.contentNegotiation", "io.ktor", "ktor-server-content-negotiation").version(versions.ktor)
            library("ktor.server.sse", "io.ktor", "ktor-server-sse").version(versions.ktor)
            library("ktor.client.core", "io.ktor", "ktor-client-core").version(versions.ktor)
            library("ktor.client.engine", "io.ktor", "ktor-client-cio").version(versions.ktor)
            library("ktor.client.logging", "io.ktor", "ktor-client-logging").version(versions.ktor)
            library("ktor.client.contentNegotiation", "io.ktor", "ktor-client-content-negotiation").version(versions.ktor)

            library("android.coreKtx", "androidx.core", "core-ktx").version(versions.android.coreKtx)
            library("android.activityKtx", "androidx.activity", "activity-ktx").version(versions.android.activity)
            library("android.activityCompose", "androidx.activity", "activity-compose").version(versions.android.activity)
            library("android.media3.common", "androidx.media3", "media3-common").version(versions.android.media3)
            library("android.media3.exoplayer", "androidx.media3", "media3-exoplayer").version(versions.android.media3)
            library("android.media3.session", "androidx.media3", "media3-session").version(versions.android.media3)
            library("android.startup", "androidx.startup", "startup-runtime").version(versions.android.startup)
        }
    }
}
