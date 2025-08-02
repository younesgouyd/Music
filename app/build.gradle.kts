plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.androidApplication)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
    androidTarget {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_22
                }
            }
        }
    }
    jvm {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_22
                }
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.json)
                implementation(libs.coroutines.core)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(libs.sqldelight.coroutines)
                implementation(libs.mp3agic)
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.coroutines.desktop)
                implementation(compose.desktop.currentOs) {
                    exclude("org.jetbrains.compose.material") // todo
                }
                implementation(libs.sqldelight.sqliteDriver)
                implementation(libs.json)
                implementation(libs.vlcj)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.coroutines.android)
                implementation(libs.android.coreKtx)
                implementation(libs.android.appcompat)
                implementation(libs.android.activityKtx)
                implementation(libs.android.activityCompose)
                implementation(libs.sqldelight.androidDriver)
                implementation(libs.android.media3.common)
                implementation(libs.android.media3.exoplayer)
                implementation(libs.android.media3.session)
            }
        }
    }
}

sqldelight {
    databases {
        create("YounesMusic") {
            deriveSchemaFromMigrations.set(true)
            dialect(libs.sqldelight.sqliteDialect)
            packageName.set("dev.younesgouyd.apps.music.common.data.sqldelight")
        }
    }
}

//compose.desktop {
//    application {
//        mainClass = "org.example.project.MainKt"
//
//        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
//            packageName = "org.example.project"
//            packageVersion = "1.0.0"
//        }
//    }
//}

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
//    buildTypes {
//        release {
//            isMinifyEnabled = false
//        }
//    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_22
        targetCompatibility = JavaVersion.VERSION_22
    }
//    buildFeatures {
//        compose = true
//    }
//    composeOptions {
//        kotlinCompilerExtensionVersion = libs.versions.kotlin.get()
//    }

    packaging.resources {
        // Multiple dependency bring these files in. Exclude them to enable
        // our test APK to build (has no effect on our AARs)
        excludes += "/META-INF/AL2.0"
        excludes += "/META-INF/LGPL2.1"
    }
}