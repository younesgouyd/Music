group = "dev.younesgouyd"
version = "0.1.0"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    implementation(project(":common"))
    implementation(project(":server:multiplatform"))
    implementation(libs.coroutines.core)
    implementation(libs.android.coreKtx)
    implementation(libs.android.activityCompose)
    implementation(libs.compose.material3)
}

android {
    namespace = "dev.younesgouyd.apps.music.server.android"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    defaultConfig {
        applicationId = "dev.younesgouyd.apps.music.server.android"
        minSdk = 36
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1"
    }

    // defaults to jvmToolchain(libs.versions.java.get().toInt()) set above
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_26
//        targetCompatibility = JavaVersion.VERSION_26
//    }

    packaging.resources {
        excludes += "/META-INF/AL2.0"
        excludes += "/META-INF/LGPL2.1"
        excludes += "/META-INF/INDEX.LIST"
        excludes += "/META-INF/io.netty.versions.properties"
    }
}