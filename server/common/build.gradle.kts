plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
    androidLibrary {
        namespace = "dev.younesgouyd.apps.music.server.common"
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
            implementation(libs.jsonJava)
            implementation(libs.coroutines.core)
            implementation(libs.room.runtime)
            implementation(libs.logging)
            implementation(libs.ktor.serialization)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.engine)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.engine.cio)
            implementation(libs.ktor.server.logging)
            implementation(libs.ktor.server.contentNegotiation)
        }
        jvmMain.dependencies {
            implementation(libs.sqlite.jvm)
            implementation(libs.coroutines.desktop)
            implementation(libs.logback.jvm)
        }
        androidMain.dependencies {
            implementation(libs.sqlite.android)
            implementation(libs.coroutines.android)
            implementation(libs.android.coreKtx)
            implementation(libs.android.activityKtx)
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
