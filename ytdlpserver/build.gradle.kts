group = "dev.younesgouyd"
version = "0.1.0"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    implementation(project(":common"))
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.desktop)
    implementation(libs.serialization.core)
    implementation(libs.serialization.json)
    implementation(libs.logging)
    implementation(libs.logback.jvm)
    implementation(libs.ktor.serialization)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.engine)
    implementation(libs.ktor.server.logging)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.sse)
    implementation(libs.ktor.tlsCertificates)
    runtimeOnly(libs.ktor.server.tls)
    implementation(libs.compose.uiDesktop)
    implementation(libs.compose.material3)
    implementation(libs.compose.materialIconsExtended)
    runtimeOnly(compose.desktop.currentOs) {
        exclude("org.jetbrains.compose.material") // todo
    }
}
