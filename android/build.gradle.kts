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
        minSdk = 21
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
    implementation(project(":common"))
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.android.coreKtx)
    implementation(libs.android.appcompat)
    implementation(libs.android.activityKtx)
    implementation(libs.android.activityCompose)
    implementation(libs.android.lifecycle.viewmodelKtx)
    implementation(libs.android.lifecycle.savedState)
    implementation(libs.android.lifecycle.viewmodelCompose)
    implementation(libs.android.navigation.compose)
    implementation(libs.android.media3.common)
    implementation(libs.android.media3.exoplayer)
    implementation(libs.android.media3.ui)
    implementation(libs.android.media3.session)
    implementation(libs.sqldelight.androidDriver)
}