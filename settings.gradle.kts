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
                val kotlin = version("kotlin", "2.0.20")
                val junit = version("junit", "5.11.0")
                val coroutines = version("coroutines", "1.8.1")
                val compose = version("compose.jetbrains", "1.6.11")
                val sqldelight = version("sqldelight", "2.0.2")
                val json = version("json", "20240303")
                val vlcj = version("vlcj", "4.8.3")
            }

            plugin("kotlin.jvm", "org.jetbrains.kotlin.jvm").versionRef(versions.kotlin)
            plugin("compose.jetbrains", "org.jetbrains.compose").versionRef(versions.compose)
            plugin("compose.compiler", "org.jetbrains.kotlin.plugin.compose").versionRef(versions.kotlin)
            plugin("sqldelight", "app.cash.sqldelight").versionRef(versions.sqldelight)

            library("coroutines.core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef(versions.coroutines)
            library("sqldelight.sqliteDriver", "app.cash.sqldelight", "sqlite-driver").versionRef(versions.sqldelight)
            library("sqldelight.jdbcDriver", "app.cash.sqldelight", "jdbc-driver").versionRef(versions.sqldelight)
            library("sqldelight.sqliteDialect", "app.cash.sqldelight", "sqlite-3-38-dialect").versionRef(versions.sqldelight)
            library("sqldelight.coroutines", "app.cash.sqldelight", "coroutines-extensions").versionRef(versions.sqldelight)
            library("json", "org.json", "json").versionRef(versions.json)
            library("vlcj", "uk.co.caprica", "vlcj").versionRef(versions.vlcj)
        }
    }
}