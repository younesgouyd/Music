rootProject.name = "Music"

include(":client:app:android")
include(":client:app:jvm")
include(":client:app:multiplatform")
include(":client:spotifyapi")
include(":server")
include(":common")

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
                val java = version("java", "17")
                val kotlin = version("kotlin", "2.3.10")
                val serialization = version("serialization", "1.10.0")
                val jsonJava = version("jsonJava", "20251224")
                val coroutines = version("coroutines", "1.10.2")
                val room = version("room", "2.8.4")
                val sqlite = version("sqlite", "2.6.2")
                val ksp = version("ksp", "2.3.5")
                val logging = version("logging", "8.0.01")
                val logback = object {
                    val jvm = version("logback.jvm", "1.5.31")
                    val android = version("logback.android", "3.0.0")
                }
                val ktor = version("ktor", "3.4.0")
                val compose = object {
                    val plugin = version("compose.jetbrains", "1.10.1")
                    val material3 = version("compose.material3", "1.9.0")
                    val materialIconsExtended = version("compose.materialIconsExtended", "1.7.3")
                    val uiDesktop = version("compose.uiDesktop", "1.10.1")
                }
                val android = object {
                    val agp = version("agp", "8.12.0")
                    val coreKtx = version("coreKtx", "1.17.0")
                    val appcompat = version("appcompat", "1.7.1")
                    val activity = version("android.activity", "1.12.3")
                    val media3 = version("android.media3", "1.9.2")
                    val documentfile = version("documentfile", "1.1.0")
                }
                val vlcj = version("vlcj", "4.12.1")
                val mp3agic = version("mp3agic", "0.9.1")
                val tika = version("tika", "3.2.3")
            }

            plugin("kotlin.multiplatform", "org.jetbrains.kotlin.multiplatform").versionRef(versions.kotlin)
            plugin("kotlin.jvm", "org.jetbrains.kotlin.jvm").versionRef(versions.kotlin)
            plugin("kotlin.android", "org.jetbrains.kotlin.android").versionRef(versions.kotlin)
            plugin("kotlin.serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef(versions.kotlin)
            plugin("composeMultiplatform", "org.jetbrains.compose").versionRef(versions.compose.plugin)
            plugin("composeCompiler", "org.jetbrains.kotlin.plugin.compose").versionRef(versions.kotlin)
            plugin("ksp", "com.google.devtools.ksp").versionRef(versions.ksp)
            plugin("room", "androidx.room").versionRef(versions.room)
            plugin("androidApplication", "com.android.application").versionRef(versions.android.agp)
            plugin("androidMultiplatform", "com.android.kotlin.multiplatform.library").versionRef(versions.android.agp)

            library("compose.material3", "org.jetbrains.compose.material3", "material3").versionRef(versions.compose.material3)
            library("compose.windowSizeClass", "org.jetbrains.compose.material3", "material3-window-size-class").versionRef(versions.compose.material3)
            library("compose.materialIconsExtended", "org.jetbrains.compose.material", "material-icons-extended").versionRef(versions.compose.materialIconsExtended)
            library("compose.uiDesktop", "org.jetbrains.compose.ui", "ui-desktop").versionRef(versions.compose.uiDesktop)

            library("serialization.core", "org.jetbrains.kotlinx", "kotlinx-serialization-core").versionRef(versions.serialization)
            library("serialization.json", "org.jetbrains.kotlinx", "kotlinx-serialization-json").versionRef(versions.serialization)
            library("jsonJava", "org.json", "json").versionRef(versions.jsonJava)

            library("coroutines.core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef(versions.coroutines)
            library("coroutines.desktop", "org.jetbrains.kotlinx", "kotlinx-coroutines-swing").versionRef(versions.coroutines)
            library("coroutines.android", "org.jetbrains.kotlinx", "kotlinx-coroutines-android").versionRef(versions.coroutines)

            library("room.runtime", "androidx.room", "room-runtime").versionRef(versions.room)
            library("room.compiler", "androidx.room", "room-compiler").versionRef(versions.room)
            library("sqlite.jvm", "androidx.sqlite", "sqlite-bundled-jvm").versionRef(versions.sqlite)
            library("sqlite.android", "androidx.sqlite", "sqlite-bundled").versionRef(versions.sqlite)

            library("vlcj", "uk.co.caprica", "vlcj").versionRef(versions.vlcj)
            library("mp3agic", "com.mpatric", "mp3agic").versionRef(versions.mp3agic)
            library("tika", "org.apache.tika", "tika-core").versionRef(versions.tika)

            library("logging", "io.github.oshai", "kotlin-logging").versionRef(versions.logging)
            library("logback.jvm", "ch.qos.logback", "logback-classic").versionRef(versions.logback.jvm)
            library("logback.android", "com.github.tony19", "logback-android").versionRef(versions.logback.android)

            library("ktor.serialization", "io.ktor", "ktor-serialization-kotlinx-json").versionRef(versions.ktor)
            library("ktor.server.core", "io.ktor", "ktor-server-core").versionRef(versions.ktor)
            library("ktor.server.engine.cio", "io.ktor", "ktor-server-cio").versionRef(versions.ktor)
            library("ktor.server.engine.netty", "io.ktor", "ktor-server-netty").versionRef(versions.ktor)
            library("ktor.server.logging", "io.ktor", "ktor-server-call-logging").versionRef(versions.ktor)
            library("ktor.server.contentNegotiation", "io.ktor", "ktor-server-content-negotiation").versionRef(versions.ktor)
            library("ktor.server.sse", "io.ktor", "ktor-server-sse").versionRef(versions.ktor)
            library("ktor.client.core", "io.ktor", "ktor-client-core").versionRef(versions.ktor)
            library("ktor.client.engine", "io.ktor", "ktor-client-cio").versionRef(versions.ktor)
            library("ktor.client.logging", "io.ktor", "ktor-client-logging").versionRef(versions.ktor)
            library("ktor.client.contentNegotiation", "io.ktor", "ktor-client-content-negotiation").versionRef(versions.ktor)

            library("android.coreKtx", "androidx.core", "core-ktx").versionRef(versions.android.coreKtx)
            library("android.appcompat", "androidx.appcompat", "appcompat").versionRef(versions.android.appcompat)
            library("android.activityKtx", "androidx.activity", "activity-ktx").versionRef(versions.android.activity)
            library("android.activityCompose", "androidx.activity", "activity-compose").versionRef(versions.android.activity)
            library("android.media3.common", "androidx.media3", "media3-common").versionRef(versions.android.media3)
            library("android.media3.exoplayer", "androidx.media3", "media3-exoplayer").versionRef(versions.android.media3)
            library("android.media3.session", "androidx.media3", "media3-session").versionRef(versions.android.media3)
            library("android.documentfile", "androidx.documentfile", "documentfile").versionRef(versions.android.documentfile)
        }
    }
}
