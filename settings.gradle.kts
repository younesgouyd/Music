rootProject.name = "Music"

include(":app")

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
                val coroutines = version("coroutines", "1.10.2")
                val compose = version("compose.jetbrains", "1.8.2")
                val android = object {
                    val agp = version("agp", "8.10.0")
                    val coreKtx = version("coreKtx", "1.16.0")
                    val appcompat = version("appcompat", "1.7.1")
                    val activity = version("android.activity", "1.10.1")
                    val media3 = version("android.media3", "1.7.1")
                }
                val sqldelight = version("sqldelight", "2.1.0")
                val vlcj = version("vlcj", "4.11.0")
                val mp3agic = version("mp3agic", "0.9.1")
            }

            plugin("kotlin.multiplatform", "org.jetbrains.kotlin.multiplatform").versionRef(versions.kotlin)
            plugin("composeMultiplatform", "org.jetbrains.compose").versionRef(versions.compose)
            plugin("composeCompiler", "org.jetbrains.kotlin.plugin.compose").versionRef(versions.kotlin)
            plugin("sqldelight", "app.cash.sqldelight").versionRef(versions.sqldelight)
            plugin("androidApplication", "com.android.application").versionRef(versions.android.agp)

            library("json", "org.jetbrains.kotlinx", "kotlinx-serialization-json").versionRef(versions.json)

            library("coroutines.core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef(versions.coroutines)
            library("coroutines.desktop", "org.jetbrains.kotlinx", "kotlinx-coroutines-swing").versionRef(versions.coroutines)
            library("coroutines.android", "org.jetbrains.kotlinx", "kotlinx-coroutines-android").versionRef(versions.coroutines)

            library("sqldelight.sqliteDialect", "app.cash.sqldelight", "sqlite-3-38-dialect").versionRef(versions.sqldelight)
            library("sqldelight.coroutines", "app.cash.sqldelight", "coroutines-extensions").versionRef(versions.sqldelight)
            library("sqldelight.sqliteDriver", "app.cash.sqldelight", "sqlite-driver").versionRef(versions.sqldelight)
            library("sqldelight.androidDriver", "app.cash.sqldelight", "android-driver").versionRef(versions.sqldelight)

            library("vlcj", "uk.co.caprica", "vlcj").versionRef(versions.vlcj)
            library("mp3agic", "com.mpatric", "mp3agic").versionRef(versions.mp3agic)

            library("android.coreKtx", "androidx.core", "core-ktx").versionRef(versions.android.coreKtx)
            library("android.appcompat", "androidx.appcompat", "appcompat").versionRef(versions.android.appcompat)
            library("android.activityKtx", "androidx.activity", "activity-ktx").versionRef(versions.android.activity)
            library("android.activityCompose", "androidx.activity", "activity-compose").versionRef(versions.android.activity)
            library("android.media3.common", "androidx.media3", "media3-common").versionRef(versions.android.media3)
            library("android.media3.exoplayer", "androidx.media3", "media3-exoplayer").versionRef(versions.android.media3)
            library("android.media3.session", "androidx.media3", "media3-session").versionRef(versions.android.media3)
        }
    }
}
