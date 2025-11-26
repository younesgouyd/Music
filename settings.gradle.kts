rootProject.name = "Music"

include(":client")
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
                val java = version("java", "22")
                val kotlin = version("kotlin", "2.2.0")
                val json = version("json", "1.9.0")
                val jsonJava = version("jsonJava", "20250517")
                val coroutines = version("coroutines", "1.10.2")
                val room = version("room", "2.8.1")
                val sqlite = version("sqlite", "2.6.1")
                val ksp = version("ksp", "2.2.0-2.0.2")
                val slf4j = version("slf4j", "2.0.17")
                val log4j = version("log4j", "2.25.1")
                val logback = object {
                    val jvm = version("logback.jvm", "1.5.18")
                    val android = version("logback.android", "3.0.0")
                }
                val ktor = version("ktor", "3.2.3")
                val compose = version("compose.jetbrains", "1.8.2")
                val android = object {
                    val agp = version("agp", "8.10.0")
                    val coreKtx = version("coreKtx", "1.16.0")
                    val appcompat = version("appcompat", "1.7.1")
                    val activity = version("android.activity", "1.10.1")
                    val media3 = version("android.media3", "1.7.1")
                    val documentfile = version("documentfile", "1.1.0")
                }
                val vlcj = version("vlcj", "4.11.0")
                val mp3agic = version("mp3agic", "0.9.1")
                val tika = version("tika", "3.2.3")
            }

            plugin("kotlin.jvm", "org.jetbrains.kotlin.jvm").versionRef(versions.kotlin)
            plugin("kotlin.serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef(versions.kotlin)
            plugin("kotlin.multiplatform", "org.jetbrains.kotlin.multiplatform").versionRef(versions.kotlin)
            plugin("composeMultiplatform", "org.jetbrains.compose").versionRef(versions.compose)
            plugin("composeCompiler", "org.jetbrains.kotlin.plugin.compose").versionRef(versions.kotlin)
            plugin("ksp", "com.google.devtools.ksp").versionRef(versions.ksp)
            plugin("room", "androidx.room").versionRef(versions.room)
            plugin("androidApplication", "com.android.application").versionRef(versions.android.agp)

            library("json", "org.jetbrains.kotlinx", "kotlinx-serialization-json").versionRef(versions.json)
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

            library("slf4j", "org.slf4j", "slf4j-api").versionRef(versions.slf4j)
            library("log4j.core", "org.apache.logging.log4j", "log4j-core").versionRef(versions.log4j)
            library("log4j.slf4jImpl", "org.apache.logging.log4j", "log4j-slf4j-impl").versionRef(versions.log4j)
            library("logback.jvm", "ch.qos.logback", "logback-classic").versionRef(versions.logback.jvm)
            library("logback.android", "com.github.tony19", "logback-android").versionRef(versions.logback.android)

            library("ktor.serialization", "io.ktor", "ktor-serialization-kotlinx-json").versionRef(versions.ktor)
            library("ktor.server.core", "io.ktor", "ktor-server-core").versionRef(versions.ktor)
            library("ktor.server.engine", "io.ktor", "ktor-server-cio").versionRef(versions.ktor)
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
