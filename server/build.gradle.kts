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
    implementation(project(":common"))
    implementation(libs.coroutines.core)
    implementation(libs.slf4j)
    implementation(libs.log4j.core)
    implementation(libs.log4j.slf4jImpl)
    implementation(libs.logback.jvm)
    implementation(libs.ktor.serialization)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.engine)
    implementation(libs.ktor.server.logging)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.sse)
}
