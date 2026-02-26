plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidMultiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
    androidLibrary {
        namespace = "dev.younesgouyd.apps.music.client.app.multiplatform"
        compileSdk = 36
        minSdk = 29
        androidResources.enable = true
    }
    jvm()
    sourceSets {
        commonMain.dependencies {
            implementation(project(":common"))
            implementation(project(":client:spotifyapi"))
            implementation(libs.json)
            implementation(libs.jsonJava)
            implementation(libs.coroutines.core)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.room.runtime)
            implementation(libs.mp3agic)
            implementation(libs.tika)
            implementation(libs.logging)
            implementation(libs.ktor.serialization)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.engine)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.contentNegotiation)
        }
        jvmMain.dependencies {
            implementation(libs.sqlite.jvm)
            implementation(libs.coroutines.desktop)
            implementation(compose.desktop.currentOs) {
                exclude("org.jetbrains.compose.material") // todo
            }
            implementation(libs.vlcj)
            implementation(libs.logback.jvm)
        }
        androidMain.dependencies {
            implementation(libs.sqlite.android)
            implementation(libs.coroutines.android)
            implementation(libs.android.coreKtx)
            implementation(libs.android.appcompat)
            implementation(libs.android.activityKtx)
            implementation(libs.android.activityCompose)
            implementation(libs.android.media3.common)
            implementation(libs.android.media3.exoplayer)
            implementation(libs.android.media3.session)
            implementation(libs.android.documentfile)
            implementation(libs.logback.android)
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}
