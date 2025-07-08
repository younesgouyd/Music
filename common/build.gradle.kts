group = "dev.younesgouyd"
version = "0.1.0"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.jetbrains)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.sqldelight.sqliteDriver)
    implementation(libs.sqldelight.jdbcDriver)
    implementation(libs.sqldelight.coroutines)
    implementation(libs.json)
    implementation(libs.ktor.serialization)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.coreJvm)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.loggingJvm)
    implementation(libs.mp3agic)
}

sqldelight {
    databases {
        create("YounesMusic") {
            deriveSchemaFromMigrations.set(true)
            dialect(libs.sqldelight.sqliteDialect)
            packageName.set("dev.younesgouyd.apps.music.common.data.sqldelight")
        }
    }
}