group = "dev.younesgouyd"
version = "0.1.0"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    implementation(libs.json)
    implementation(libs.coroutines.core)
    implementation(libs.slf4j)
    implementation(libs.ktor.serialization)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.engine)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.contentNegotiation)
}
