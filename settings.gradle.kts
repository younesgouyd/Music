rootProject.name = "Music"

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
                val compose = version("compose.jetbrains", "1.6.11")
                val android = object {
                    val agp = version("agp", "8.7.0")
                    val coreKtx = version("coreKtx", "1.15.0")
                    val appcompat = version("appcompat", "1.7.0")
                    val activity = version("android.activity", "1.10.0")
                    val lifecycle = version("android.lifecycle", "2.8.7")
                    val navigation = version("android.navigation", "2.8.6")
                }
                val sqldelight = version("sqldelight", "2.0.2")
                val json = version("json", "20240303")
                val vlcj = version("vlcj", "4.8.3")
                val mp3agic = version("mp3agic", "0.9.1")
            }

            plugin("kotlin.jvm", "org.jetbrains.kotlin.jvm").versionRef(versions.kotlin)
            plugin("kotlin.android", "org.jetbrains.kotlin.android").versionRef(versions.kotlin)
            plugin("android.application", "com.android.application").versionRef(versions.android.agp)
            plugin("compose.jetbrains", "org.jetbrains.compose").versionRef(versions.compose)
            plugin("compose.compiler", "org.jetbrains.kotlin.plugin.compose").versionRef(versions.kotlin)
            plugin("sqldelight", "app.cash.sqldelight").versionRef(versions.sqldelight)

            library("coroutines.core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef(versions.coroutines)
            library("coroutines.android", "org.jetbrains.kotlinx", "kotlinx-coroutines-android").versionRef(versions.coroutines)
            library("sqldelight.sqliteDriver", "app.cash.sqldelight", "sqlite-driver").versionRef(versions.sqldelight)
            library("sqldelight.jdbcDriver", "app.cash.sqldelight", "jdbc-driver").versionRef(versions.sqldelight)
            library("sqldelight.androidDriver", "app.cash.sqldelight", "android-driver").versionRef(versions.sqldelight)
            library("sqldelight.sqliteDialect", "app.cash.sqldelight", "sqlite-3-38-dialect").versionRef(versions.sqldelight)
            library("sqldelight.coroutines", "app.cash.sqldelight", "coroutines-extensions").versionRef(versions.sqldelight)
            library("json", "org.json", "json").versionRef(versions.json)
            library("vlcj", "uk.co.caprica", "vlcj").versionRef(versions.vlcj)
            library("mp3agic", "com.mpatric", "mp3agic").versionRef(versions.mp3agic)

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
        }
    }
}
