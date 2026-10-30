plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
    android {
        namespace = "dev.younesgouyd.apps.music.common"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        androidResources.enable = true
    }
    jvm()
    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.serialization)
            implementation(libs.ktor.server.core)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)
        }
        androidMain.dependencies {
            implementation(libs.android.coreKtx)
        }
    }
}

