plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.jetbrains)
    alias(libs.plugins.sqldelight)
}

android {
    namespace = "dev.younesgouyd.apps.music.android"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.younesgouyd.apps.music.android"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_22
        targetCompatibility = JavaVersion.VERSION_22
    }
    kotlinOptions {
        jvmTarget = libs.versions.java.get()
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlin.get()
    }

    packaging.resources {
        // Multiple dependency bring these files in. Exclude them to enable
        // our test APK to build (has no effect on our AARs)
        excludes += "/META-INF/AL2.0"
        excludes += "/META-INF/LGPL2.1"
    }
}

dependencies {
    // modules
    implementation(project(":common"))

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Compose
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Android
    implementation(libs.android.coreKtx)
    implementation(libs.android.appcompat)
    implementation(libs.android.activityKtx)
    implementation(libs.android.activityCompose)

    // Lifecycle
    implementation(libs.android.lifecycle.viewmodelKtx)
    implementation(libs.android.lifecycle.savedState)
    implementation(libs.android.lifecycle.viewmodelCompose)

    // Navigation
    implementation(libs.android.navigation.compose)

    // SQL Delight
    implementation(libs.sqldelight.androidDriver)
}