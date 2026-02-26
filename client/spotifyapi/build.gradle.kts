group = "dev.younesgouyd"
version = "0.1.0"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidMultiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
    jvm()
    androidLibrary {
        namespace = "dev.younesgouyd.apps.music.client.spotifyapi"
        compileSdk = 36
        minSdk = 29
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.json)
            implementation(libs.coroutines.core)
            implementation(libs.ktor.serialization)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.engine)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.contentNegotiation)
        }
        jvmMain.dependencies {
            implementation(libs.logback.jvm)
        }
        androidMain.dependencies {
            implementation(libs.logback.android)
        }
    }
}

