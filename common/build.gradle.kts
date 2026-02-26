group = "dev.younesgouyd"
version = "0.1.0"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
    jvm()
    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.serialization)
        }
    }
}

