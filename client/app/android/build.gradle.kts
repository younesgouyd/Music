group = "dev.younesgouyd"
version = "0.1.0"

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlin.android)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    implementation(project(":client:app:multiplatform"))
}

android {
    namespace = "dev.younesgouyd.apps.music.client.app.android"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.younesgouyd.apps.music.client.app.android"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging.resources {
        // Multiple dependency bring these files in. Exclude them to enable
        // our test APK to build (has no effect on our AARs)
        excludes += "/META-INF/AL2.0"
        excludes += "/META-INF/LGPL2.1"
    }
}