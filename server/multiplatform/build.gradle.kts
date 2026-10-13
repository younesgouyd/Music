plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
    androidLibrary {
        namespace = "dev.younesgouyd.apps.music.server.multiplatform"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = 36
        androidResources.enable = true
        packaging.resources {
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
    jvm()
    sourceSets {
        commonMain.dependencies {
            implementation(project(":common"))
            implementation(libs.serialization.core)
            implementation(libs.serialization.json)
            implementation(libs.jsonJava)
            implementation(libs.coroutines.core)
            implementation(libs.datetime)
            implementation(libs.room.runtime)
            implementation(libs.logging)
            implementation(libs.ktor.serialization)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.engine)
            implementation(libs.ktor.server.logging)
            implementation(libs.ktor.server.contentNegotiation)
            implementation(libs.ktor.server.websockets)
            runtimeOnly(libs.ktor.server.tls)
            implementation(libs.ktor.server.auth)
            implementation(libs.ktor.tlsCertificates)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.engine)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)
        }
        jvmMain.dependencies {
            implementation(libs.sqlite.jvm)
            implementation(libs.coroutines.desktop)
            implementation(libs.logback.jvm)
            runtimeOnly(compose.desktop.currentOs) {
                exclude("org.jetbrains.compose.material") // todo
            }
        }
        androidMain.dependencies {
            implementation(libs.sqlite.android)
            implementation(libs.coroutines.android)
            implementation(libs.android.coreKtx)
            implementation(libs.android.activityKtx)
            implementation(libs.logback.android)
            implementation(libs.android.activityCompose)
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
