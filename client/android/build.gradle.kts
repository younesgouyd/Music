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
    implementation(project(":client:multiplatform"))
    implementation(libs.android.coreKtx)
    implementation(libs.android.activityKtx)
    implementation(libs.android.activityCompose)
    implementation(libs.compose.material3)
}

android {
    namespace = "dev.younesgouyd.apps.music.client.android"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    defaultConfig {
        applicationId = "dev.younesgouyd.apps.music.client.android"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1"

    }

    // defaults to jvmToolchain(libs.versions.java.get().toInt()) set above
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_25
//        targetCompatibility = JavaVersion.VERSION_25
//    }

    packaging.resources {
        excludes += "META-INF/AL2.0"
        excludes += "META-INF/LGPL2.1"
        excludes += "META-INF/native-image/**"
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}