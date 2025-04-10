group = "dev.younesgouyd"
version = "0.1.0"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.jetbrains)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    implementation(project(":common"))
    implementation(libs.coroutines.core)
    implementation(compose.desktop.currentOs) {
        exclude("org.jetbrains.compose.material") // todo
    }
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.sqldelight.sqliteDriver)
    implementation(libs.sqldelight.jdbcDriver)
    implementation(libs.json)
    implementation(libs.vlcj.desktop)
    implementation(libs.mp3agic)
}
