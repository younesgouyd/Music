rootProject.name = "Music"

include(":server")
include(":common")
include(":desktop")
include(":android")

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
                val kotlin = version("kotlin", "2.0.20")
                val coroutines = version("coroutines", "1.8.1")
                val ktor = version("ktor", "3.1.2")
                val logback = version("logback", "1.4.11")
                val okhttp = version("okhttp", "4.10.0")
                val compose = version("compose.jetbrains", "1.6.11")
                val android = object {
                    val agp = version("agp", "8.7.0")
                    val coreKtx = version("coreKtx", "1.15.0")
                    val appcompat = version("appcompat", "1.7.0")
                    val activity = version("android.activity", "1.10.0")
                    val lifecycle = version("android.lifecycle", "2.8.7")
                    val navigation = version("android.navigation", "2.8.6")
                    val media3 = version("android.media3", "1.6.1")
                }
                val sqldelight = version("sqldelight", "2.0.2")
                val json = version("json", "20240303")
                val vlcj = version("vlcj", "4.8.3")
                val mp3agic = version("mp3agic", "0.9.1")
            }

            plugin("kotlin.jvm", "org.jetbrains.kotlin.jvm").versionRef(versions.kotlin)
            plugin("kotlin.serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef(versions.kotlin)
            plugin("compose.jetbrains", "org.jetbrains.compose").versionRef(versions.compose)
            plugin("compose.compiler", "org.jetbrains.kotlin.plugin.compose").versionRef(versions.kotlin)
            plugin("sqldelight", "app.cash.sqldelight").versionRef(versions.sqldelight)
            library("logback", "ch.qos.logback", "logback-classic").versionRef(versions.logback)

            library("ktor.serialization", "io.ktor", "ktor-serialization-kotlinx-json").versionRef(versions.ktor)
            library("ktor.server.core", "io.ktor", "ktor-server-core").versionRef(versions.ktor)
            library("ktor.server.netty", "io.ktor", "ktor-server-netty").versionRef(versions.ktor)
            library("ktor.server.logging", "io.ktor", "ktor-server-call-logging").versionRef(versions.ktor)
            library("ktor.server.contentNegotiation", "io.ktor", "ktor-server-content-negotiation").versionRef(versions.ktor)
            library("ktor.server.websockets", "io.ktor", "ktor-server-websockets").versionRef(versions.ktor)
            library("ktor.server.sessions", "io.ktor", "ktor-server-sessions").versionRef(versions.ktor)

            library("ktor.client.core", "io.ktor", "ktor-client-core").versionRef(versions.ktor)
            library("ktor.client.coreJvm", "io.ktor", "ktor-client-core-jvm").versionRef(versions.ktor)
            library("ktor.client.cio", "io.ktor", "ktor-client-cio").versionRef(versions.ktor)
            library("ktor.client.logging", "io.ktor", "ktor-client-logging").versionRef(versions.ktor)
            library("ktor.client.loggingJvm", "io.ktor", "ktor-client-logging-jvm").versionRef(versions.ktor)
            library("ktor.client.contentNegotiation", "io.ktor", "ktor-client-content-negotiation").versionRef(versions.ktor)
            library("ktor.client.websockets", "io.ktor", "ktor-client-websockets").versionRef(versions.ktor)
            library("ktor.client.auth", "io.ktor", "ktor-client-auth").versionRef(versions.ktor)

            library("okhttp.logging", "com.squareup.okhttp3", "logging-interceptor").versionRef(versions.okhttp)

            library("coroutines.core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef(versions.coroutines)
            library("coroutines.desktop", "org.jetbrains.kotlinx", "kotlinx-coroutines-swing").versionRef(versions.coroutines)
            library("sqldelight.sqliteDriver", "app.cash.sqldelight", "sqlite-driver").versionRef(versions.sqldelight)
            library("sqldelight.jdbcDriver", "app.cash.sqldelight", "jdbc-driver").versionRef(versions.sqldelight)
            library("sqldelight.sqliteDialect", "app.cash.sqldelight", "sqlite-3-38-dialect").versionRef(versions.sqldelight)
            library("sqldelight.coroutines", "app.cash.sqldelight", "coroutines-extensions").versionRef(versions.sqldelight)
            library("json", "org.json", "json").versionRef(versions.json)
            library("vlcj", "uk.co.caprica", "vlcj").versionRef(versions.vlcj)
            library("mp3agic", "com.mpatric", "mp3agic").versionRef(versions.mp3agic)

            // ANDROID
            plugin("kotlin.android", "org.jetbrains.kotlin.android").versionRef(versions.kotlin)
            plugin("android.application", "com.android.application").versionRef(versions.android.agp)

            library("coroutines.android", "org.jetbrains.kotlinx", "kotlinx-coroutines-android").versionRef(versions.coroutines)
            library("android.coreKtx", "androidx.core", "core-ktx").versionRef(versions.android.coreKtx)
            library("android.appcompat", "androidx.appcompat", "appcompat").versionRef(versions.android.appcompat)
            library("android.activityKtx", "androidx.activity", "activity-ktx").versionRef(versions.android.activity)
            library("android.activityCompose", "androidx.activity", "activity-compose").versionRef(versions.android.activity)
            library("android.lifecycle.viewmodelKtx", "androidx.lifecycle", "lifecycle-viewmodel-ktx").versionRef(versions.android.lifecycle)
            library("android.lifecycle.savedState", "androidx.lifecycle", "lifecycle-viewmodel-savedstate").versionRef(versions.android.lifecycle)
            library("android.lifecycle.runtimeKtx", "androidx.lifecycle", "lifecycle-runtime-ktx").versionRef(versions.android.lifecycle)
            library("android.lifecycle.viewmodelCompose", "androidx.lifecycle", "lifecycle-viewmodel-compose").versionRef(versions.android.lifecycle)
            library("android.navigation.uiKtx", "androidx.navigation", "navigation-ui-ktx").versionRef(versions.android.navigation)
            library("android.navigation.compose", "androidx.navigation", "navigation-compose").versionRef(versions.android.navigation)
            library("sqldelight.androidDriver", "app.cash.sqldelight", "android-driver").versionRef(versions.sqldelight)
            library("android.media3.common", "androidx.media3", "media3-common").versionRef(versions.android.media3)
            library("android.media3.exoplayer", "androidx.media3", "media3-exoplayer").versionRef(versions.android.media3)
            library("android.media3.ui", "androidx.media3", "media3-ui").versionRef(versions.android.media3)
            library("android.media3.session", "androidx.media3", "media3-session").versionRef(versions.android.media3)
        }
    }
}
