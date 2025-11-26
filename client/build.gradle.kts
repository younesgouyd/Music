plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
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
                implementation(project(":common"))
                implementation(libs.json)
                implementation(libs.jsonJava)
                implementation(libs.coroutines.core)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(libs.room.runtime)
                implementation(libs.mp3agic)
                implementation(libs.tika)
                implementation(libs.slf4j)
                implementation(libs.ktor.serialization)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.engine)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.client.core)
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.sqlite.jvm)
                implementation(libs.coroutines.desktop)
                implementation(compose.desktop.currentOs) {
                    exclude("org.jetbrains.compose.material") // todo
                }
                implementation(libs.vlcj)
                implementation(libs.logback.jvm)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.sqlite.android)
                implementation(libs.coroutines.android)
                implementation(libs.android.coreKtx)
                implementation(libs.android.appcompat)
                implementation(libs.android.activityKtx)
                implementation(libs.android.activityCompose)
                implementation(libs.android.media3.common)
                implementation(libs.android.media3.exoplayer)
                implementation(libs.android.media3.session)
                implementation(libs.logback.android)
                implementation(libs.android.documentfile)
            }
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
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
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.younesgouyd.apps.music.android"
        minSdk = 29
        targetSdk = 36
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

    packaging.resources {
        // Multiple dependency bring these files in. Exclude them to enable
        // our test APK to build (has no effect on our AARs)
        excludes += "/META-INF/AL2.0"
        excludes += "/META-INF/LGPL2.1"
    }
}